import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { ProductComponent } from './product/product.component';
import { CreateUsersComponent } from './create-users/create-users.component';
import { AddProduct } from './add-product/add-product';
import { authGuard } from './auth.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },

  { path: 'products', component: ProductComponent, canActivate:[authGuard] },

  { path: 'create-users', component: CreateUsersComponent },

  { path: 'add-product', component: AddProduct, canActivate:[authGuard] },

  { path: '**', redirectTo: 'login' }
];