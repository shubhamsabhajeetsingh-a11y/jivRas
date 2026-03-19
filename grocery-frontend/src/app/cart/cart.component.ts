import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CartService } from '../core/services/cart.service';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.css']
})
export class CartComponent implements OnInit {

  cart: any = null;
  loading = true;
  errorMessage = '';

  constructor(
    private cartService: CartService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.loadCart();
    }
  }

  loadCart(): void {
    this.loading = true;
    this.cartService.getCart().subscribe({
      next: (data) => {
        this.cart = data;
        this.loading = false;
      },
      error: () => {
        this.cart = null;
        this.loading = false;
      }
    });
  }

  updateQuantity(item: any, newQuantity: number): void {
    if (newQuantity < 0.1) return;
    this.cartService.updateCartItem(item.cartItemId, newQuantity).subscribe({
      next: (data) => {
        this.cart = data;
      },
      error: (err) => {
        this.errorMessage = 'Failed to update quantity.';
        console.error(err);
      }
    });
  }

  removeItem(item: any): void {
    this.cartService.removeCartItem(item.cartItemId).subscribe({
      next: (data) => {
        this.cart = data;
      },
      error: (err) => {
        this.errorMessage = 'Failed to remove item.';
        console.error(err);
      }
    });
  }

  proceedToCheckout(): void {
    this.router.navigate(['/checkout']);
  }

  continueShopping(): void {
    this.router.navigate(['/products']);
  }
}
