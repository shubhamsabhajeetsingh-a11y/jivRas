import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { ProductComponent } from './product/product.component';
import { CreateUsersComponent } from './create-users/create-users.component';
import { CreateEmployeeComponent } from './create-employee/create-employee.component';
import { AddProduct } from './add-product/add-product';
import { CartComponent } from './cart/cart.component';
import { CheckoutComponent } from './checkout/checkout.component';
import { OrderConfirmationComponent } from './order-confirmation/order-confirmation.component';
import { authGuard } from './auth.guard';
import { InventoryDashboard } from './inventory-dashboard/inventory-dashboard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },

  // Products — open to everyone (guests can browse and add to cart)
  { path: 'products', component: ProductComponent },

  // Customer self-registration (public)
  { path: 'create-users', component: CreateUsersComponent },

  // Employee registration — EMPLOYEE/ADMIN only
  { path: 'create-employee', component: CreateEmployeeComponent, canActivate: [authGuard] },

  // Inventory Dashboard - EMPLOYEE only
  { path: 'inventory-dashboard', component: InventoryDashboard, canActivate: [authGuard] },

  // Add product — EMPLOYEE only (auth guard protects)
  { path: 'add-product', component: AddProduct, canActivate: [authGuard] },

  // Cart — open to everyone (guests use X-Guest-Id)
  { path: 'cart', component: CartComponent },

  // Checkout — open to everyone
  { path: 'checkout', component: CheckoutComponent },

  // Order confirmation
  { path: 'order-confirmation/:id', component: OrderConfirmationComponent },

  // Default redirect
  { path: '', redirectTo: 'products', pathMatch: 'full' },
  { path: '**', redirectTo: 'products' }
];