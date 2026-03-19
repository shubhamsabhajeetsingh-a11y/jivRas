import { Component, OnInit, Inject, PLATFORM_ID, ChangeDetectorRef } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { ProductService } from '../core/services/product.service';
import { CartService } from '../core/services/cart.service';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-product',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './product.component.html',
  styleUrls: ['./product.component.css']
})
export class ProductComponent implements OnInit {

  products: any[] = [];
  isEmployee = false;
  isLoggedIn = false;
  cartItemCount = 0;
  addedProductId: number | null = null; // For showing "Added!" feedback

  constructor(
    private productService: ProductService,
    private cartService: CartService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      const role = localStorage.getItem('userRole');
      this.isEmployee = role === 'ROLE_EMPLOYEE';
      this.isLoggedIn = !!localStorage.getItem('accessToken');
      this.loadProducts();

      // Subscribe to cart count for badge
      this.cartService.cartCount$.subscribe(count => {
        this.cartItemCount = count;
        this.cdr.detectChanges();
      });

      // Load initial cart count
      this.cartService.getCart().subscribe({
        next: () => {},
        error: () => {} // Cart might be empty, that's ok
      });
    }
  }

  loadProducts(): void {
    this.productService.getAllProducts().subscribe({
      next: (data) => {
        this.products = data;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error loading products:', error);
      }
    });
  }

  addToCart(product: any): void {
    this.cartService.addToCart(product.id, 1).subscribe({
      next: () => {
        this.addedProductId = product.id;
        // Reset feedback after 1.5 seconds
        setTimeout(() => {
          this.addedProductId = null;
          this.cdr.detectChanges();
        }, 1500);
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error adding to cart:', err);
      }
    });
  }

  navigateToAddProduct(): void {
    this.router.navigate(['/add-product']);
  }

  navigateToCart(): void {
    this.router.navigate(['/cart']);
  }

  logout(): void {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userRole');
    this.router.navigate(['/login']);
  }
}