import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { ProductComponent } from './product/product.component';
import { CreateUsersComponent } from './create-users/create-users.component';
import { AddProduct } from './add-product/add-product';
import { CartComponent } from './cart/cart.component';
import { CheckoutComponent } from './checkout/checkout.component';
import { OrderConfirmationComponent } from './order-confirmation/order-confirmation.component';
import { authGuard } from './auth.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },

  // Products — open to everyone (guests can browse and add to cart)
  { path: 'products', component: ProductComponent },

  { path: 'create-users', component: CreateUsersComponent },

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