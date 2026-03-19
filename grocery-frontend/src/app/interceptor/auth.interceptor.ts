import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import {
  HttpEvent,
  HttpInterceptor,
  HttpHandler,
  HttpRequest,
  HttpErrorResponse
} from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';

/**
 * Intercepts all HTTP requests to:
 * 1. Attach JWT Bearer token if user is logged in
 * 2. Attach X-Guest-Id header if user is a guest (for cart/order)
 * 3. Redirect to /login on 401 errors
 */
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {

    if (!isPlatformBrowser(this.platformId)) {
      return next.handle(req);
    }

    const token = localStorage.getItem('accessToken');
    const guestId = this.getOrCreateGuestId();

    // Clone request and attach appropriate headers
    let headers: { [key: string]: string } = {};

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    } else if (guestId) {
      headers['X-Guest-Id'] = guestId;
    }

    req = req.clone({ setHeaders: headers });

    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          localStorage.removeItem('userRole');
          this.router.navigate(['/login']);
        }
        return throwError(() => error);
      })
    );
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
