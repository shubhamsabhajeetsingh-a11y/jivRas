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

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Get token from localStorage only if in browser
    const token = isPlatformBrowser(this.platformId) 
      ? localStorage.getItem('accessToken') 
      : null;

    // Clone the request and add the Authorization header if token exists
    if (token) {
      req = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }

    // Pass the request through and handle errors
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          if (isPlatformBrowser(this.platformId)) {
            // Token expired or invalid, redirect to login
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('isOwner');
          }
          this.router.navigate(['/login']);
        }
        return throwError(() => error);
      })
    );
  }
}
