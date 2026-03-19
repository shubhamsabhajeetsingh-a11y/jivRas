import { CanActivateFn, Router } from '@angular/router';
import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export const authGuard: CanActivateFn = () => {

  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);

  // During SSR/prerendering, allow the route (no localStorage available)
  if (!isPlatformBrowser(platformId)) {
    return true;
  }

  const token = localStorage.getItem("accessToken");

  if (token) {
    return true;
  }

  router.navigate(['/login']);
  return false;
};