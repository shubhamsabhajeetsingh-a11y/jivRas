import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ProductService } from '../core/services/product.service';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-add-product',
  standalone: true, // Important for Angular 17+
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './add-product.component.html',
  styleUrls: ['./add-product.component.css']
})
export class AddProductComponent {
  productForm: FormGroup;
  successMessage = '';

  constructor(
    private fb: FormBuilder,
    private productService: ProductService,
    private router: Router
  ) {
    this.productForm = this.fb.group({
      name: ['', Validators.required],
      description: [''],
      pricePerKg: [0, [Validators.required, Validators.min(1)]],
      availableStockKg: [0, [Validators.required, Validators.min(0)]],
      // We'll hardcode the image URL for now since file upload is complex
      imageUrl: ['https://via.placeholder.com/150'] 
    });
  }

  onSubmit(): void {
    if (this.productForm.valid) {
      this.productService.addProduct(this.productForm.value).subscribe({
        next: (res) => {
          this.successMessage = 'Product added successfully!';
          // Redirect back to inventory list after 1.5 seconds
          setTimeout(() => this.router.navigate(['/products']), 1500);
        },
        error: (err) => console.error('Error adding product:', err)
      });
    }
  }
}