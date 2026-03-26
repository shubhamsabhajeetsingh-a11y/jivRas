import { Component, OnInit, ChangeDetectorRef, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ProductService } from '../core/services/product.service';
import { UserProfile } from '../user-profile/user-profile';

@Component({
  selector: 'app-inventory-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, UserProfile],
  templateUrl: './inventory-dashboard.html',
  styleUrl: './inventory-dashboard.css',
})
export class InventoryDashboard implements OnInit {
  products: any[] = [];
  filteredProducts: any[] = [];
  searchTerm: string = '';
  errorMessage: string = '';
  successMessage: string = '';

  constructor(
    private productService: ProductService, 
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.loadProducts();
    }
  }

  loadProducts(): void {
    this.productService.getAllProducts().subscribe({
      next: (data) => {
        this.products = data;
        this.filterProducts();
      },
      error: (err) => {
        this.errorMessage = 'Failed to load products.';
        console.error(err);
        this.cdr.detectChanges();
      }
    });
  }

  filterProducts(): void {
    if (!this.searchTerm) {
      this.filteredProducts = this.products;
    } else {
      const term = this.searchTerm.toLowerCase();
      this.filteredProducts = this.products.filter(p => 
        p.name.toLowerCase().includes(term)
      );
    }
    this.cdr.detectChanges();
  }

  updateProduct(product: any): void {
    this.productService.updateProduct(product.id, product).subscribe({
      next: () => {
        this.successMessage = `Product "${product.name}" updated successfully!`;
        this.cdr.detectChanges();
        setTimeout(() => { this.successMessage = ''; this.cdr.detectChanges(); }, 3000);
      },
      error: (err) => {
        this.errorMessage = `Failed to update ${product.name}.`;
        console.error(err);
        this.cdr.detectChanges();
        setTimeout(() => { this.errorMessage = ''; this.cdr.detectChanges(); }, 3000);
      }
    });
  }

  // Custom modal state
  showDeleteModal: boolean = false;
  deleteTargetId: number | null = null;

  deleteProduct(id: number): void {
    this.deleteTargetId = id;
    this.showDeleteModal = true;
    this.cdr.detectChanges();
  }

  confirmDelete(): void {
    if (this.deleteTargetId !== null) {
      this.productService.deleteProduct(this.deleteTargetId).subscribe({
        next: () => {
          this.successMessage = 'Product deleted successfully!';
          this.showDeleteModal = false;
          this.deleteTargetId = null;
          this.cdr.detectChanges();
          this.loadProducts();
          setTimeout(() => { this.successMessage = ''; this.cdr.detectChanges(); }, 3000);
        },
        error: (err) => {
          this.errorMessage = 'Failed to delete product.';
          this.showDeleteModal = false;
          this.deleteTargetId = null;
          console.error(err);
          this.cdr.detectChanges();
          setTimeout(() => { this.errorMessage = ''; this.cdr.detectChanges(); }, 3000);
        }
      });
    }
  }

  cancelDelete(): void {
    this.showDeleteModal = false;
    this.deleteTargetId = null;
    this.cdr.detectChanges();
  }
}

