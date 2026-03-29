import { Injectable, Inject, PLATFORM_ID, Injector } from '@angular/core';
import {
  HttpEvent,
  HttpInterceptor,
  HttpHandler,
  HttpRequest,
  HttpErrorResponse,
  HttpClient
} from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, switchMap, filter, take } from 'rxjs/operators';
import { Router } from '@angular/router';
import { environment } from '../environments/environment';

/**
 * Intercepts all HTTP requests to:
 * 1. Attach JWT Bearer token if user is logged in
 * 2. Attach X-Guest-Id header if user is a guest (for cart/order)
 * 3. Handle 401 errors by attempting to refresh the token using the refresh token
 */
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private isRefreshing = false;
  private refreshTokenSubject: BehaviorSubject<any> = new BehaviorSubject<any>(null);

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
    private injector: Injector
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {

    if (!isPlatformBrowser(this.platformId)) {
      return next.handle(req);
    }

    const token = localStorage.getItem('accessToken');
    const guestId = this.getOrCreateGuestId();

    let reqToForward = req;

    if (token && !req.url.includes('/api/users/refresh') && !req.url.includes('/api/users/login')) {
      reqToForward = req.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      });
    } else if (!token && guestId && !req.url.includes('/api/users/login') && !req.url.includes('/api/users/refresh')) {
      reqToForward = req.clone({
        setHeaders: { 'X-Guest-Id': guestId }
      });
    }

    return next.handle(reqToForward).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401 && !req.url.includes('/api/users/login') && !req.url.includes('/api/users/refresh')) {
          return this.handle401Error(reqToForward, next);
        } else if (error.status === 401 && req.url.includes('/api/users/refresh')) {
          this.doLogout();
        }
        return throwError(() => error);
      })
    );
  }

  private handle401Error(request: HttpRequest<any>, next: HttpHandler) {
    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshTokenSubject.next(null);

      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        const http = this.injector.get(HttpClient);
        return http.post<any>(`${environment.apiUrl}/api/users/refresh`, { refreshToken }).pipe(
          switchMap((response: any) => {
            this.isRefreshing = false;
            
            localStorage.setItem('accessToken', response.accessToken);
            localStorage.setItem('refreshToken', response.refreshToken);
            localStorage.setItem('userRole', response.role);
            
            this.refreshTokenSubject.next(response.accessToken);
            
            const newReq = request.clone({
              setHeaders: { Authorization: `Bearer ${response.accessToken}` }
            });
            return next.handle(newReq);
          }),
          catchError((err) => {
            this.isRefreshing = false;
            this.doLogout();
            return throwError(() => err);
          })
        );
      } else {
        this.isRefreshing = false;
        this.doLogout();
        return throwError(() => new Error('No refresh token available'));
      }
    } else {
      return this.refreshTokenSubject.pipe(
        filter(token => token != null),
        take(1),
        switchMap(jwt => {
          const newReq = request.clone({
            setHeaders: { Authorization: `Bearer ${jwt}` }
          });
          return next.handle(newReq);
        })
      );
    }
  }

  private doLogout() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userRole');
    this.router.navigate(['/login']);
  }

  /**
   * Get or create a guest ID for anonymous users.
   * Stored in localStorage so the cart persists across page refreshes.
   */
  private getOrCreateGuestId(): string {
    let guestId = localStorage.getItem('guestId');
    if (!guestId) {
      guestId = crypto.randomUUID();
      localStorage.setItem('guestId', guestId);
    }
    return guestId;
  }
}
