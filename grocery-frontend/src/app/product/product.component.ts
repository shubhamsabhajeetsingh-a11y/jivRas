import { Component, OnInit, Inject, PLATFORM_ID, ChangeDetectorRef } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { ProductService } from '../core/services/product.service';
import { CartService } from '../core/services/cart.service';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { UserProfile } from '../user-profile/user-profile';

@Component({
  selector: 'app-product',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, UserProfile],
  templateUrl: './product.component.html',
  styleUrls: ['./product.component.css']
})
export class ProductComponent implements OnInit {

  products: any[] = [];
  filteredProducts: any[] = [];
  searchTerm: string = '';
  isEmployee = false;
  isLoggedIn = false;
  isGuest = false;   // GUEST role has no registered account; hide My Orders for them
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

      // Staff must not browse the customer-facing product page
      if (role === 'EMPLOYEE' || role === 'ADMIN' || role === 'BRANCH_MANAGER') {
        this.router.navigate(['/inventory-dashboard']);
        return;
      }

      this.isEmployee = false;
      this.isLoggedIn = !!localStorage.getItem('accessToken');
      // Determine if this is a guest session (no account, no JWT role)
      const role = localStorage.getItem('userRole');
      this.isGuest = !role || role === 'GUEST';
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
    this.productService.getActiveProducts().subscribe({
      next: (data) => {
        this.products = data;
        this.filteredProducts = data;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error loading products:', error);
      }
    });
  }

  filterProducts(): void {
    if (!this.searchTerm) {
      this.filteredProducts = this.products;
    } else {
      const term = this.searchTerm.toLowerCase();
      this.filteredProducts = this.products.filter(p => 
        p.name.toLowerCase().includes(term) || 
        (p.description && p.description.toLowerCase().includes(term))
      );
    }
    this.cdr.detectChanges();
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