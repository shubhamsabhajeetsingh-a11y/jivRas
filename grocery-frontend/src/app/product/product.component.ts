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
 
  products: any[] = []; 
  isOwner = false; 

  constructor(
    private productService: ProductService, 
    private router: Router,
   
    private cdr: ChangeDetectorRef, 
    @Inject(PLATFORM_ID) private platformId: Object 
  ) {}

 logout(): void {
  localStorage.removeItem("accessToken");   // ✅ matches login save
  localStorage.removeItem("refreshToken");  // ✅ clear refresh too
  localStorage.removeItem("isOwner");
  this.router.navigate(['/login']);
}

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      const isOwnerStr = localStorage.getItem('isOwner');
      console.log('isOwner from localStorage:', isOwnerStr);
      this.isOwner = isOwnerStr === 'true';
      console.log('isOwner boolean:', this.isOwner);
      this.loadProducts();
    }
  }

  loadProducts(): void {
    console.log('Loading products...');
    this.productService.getAllProducts().subscribe({
      next: (data) => {
        console.log('Products loaded successfully:', data);
        this.products = data;
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