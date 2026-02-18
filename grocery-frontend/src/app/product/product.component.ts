import { Component, OnInit, Inject, PLATFORM_ID, ChangeDetectorRef } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common'; 
import { ProductService } from '../core/services/product.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-product',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './product.component.html',
  styleUrls: ['./product.component.css']
})
export class ProductComponent implements OnInit {
  // 1. FIX: We define a simple array 'products', NOT 'products$'
  products: any[] = []; 
  isOwner = false; 

  constructor(
    private productService: ProductService, 
    private router: Router,
    // 2. FIX: We inject 'cdr' here so 'this.cdr' works later
    private cdr: ChangeDetectorRef, 
    @Inject(PLATFORM_ID) private platformId: Object 
  ) {}

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.isOwner = localStorage.getItem('isOwner') === 'true';
      this.loadProducts();
    }
  }

  loadProducts(): void {
    this.productService.getAllProducts().subscribe({
      next: (data) => {
        // 3. FIX: Now 'this.products' exists and works
        this.products = data;
        console.log('Products loaded:', data);
        
        // 4. FIX: Now 'this.cdr' exists and works
        this.cdr.detectChanges(); 
      },
      error: (error) => {
        console.error('Error loading products:', error);
      }
    });
  }

  navigateToAddProduct() {
    this.router.navigate(['/add-product']);
  }
}