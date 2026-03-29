import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  {
    // Dynamic routes with params must use Client mode
    path: 'order-confirmation/:id',
    renderMode: RenderMode.Client
  },
  {
    // SSR pre-rendering causes blank pages on auth routes (localStorage is unavailable server-side).
    // Use Client mode for all routes so the browser handles rendering after hydration.
    path: '**',
    renderMode: RenderMode.Client
  }
];
