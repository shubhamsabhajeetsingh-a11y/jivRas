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
  const role = localStorage.getItem("userRole");

  if (token) {
    // If trying to access add-product or create-employee, enforce employee role
    const currentUrl = router.url; // Or we can rely on route data, but simpler here:
    if (window.location.pathname.includes('/add-product') || window.location.pathname.includes('/create-employee')) {
        if (role === 'EMPLOYEE' || role === 'ADMIN' || role === 'BRANCH_MANAGER') {
            return true;
        } else {
            router.navigate(['/products']);
            return false;
        }
    }
    return true;
  }

  router.navigate(['/login']);
  return false;
};