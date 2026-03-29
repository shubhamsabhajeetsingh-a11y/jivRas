import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ProductService } from '../core/services/product.service';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-add-product',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, FormsModule],
  templateUrl: './add-product.html',
  styleUrl: './add-product.css'
})
export class AddProduct implements OnInit {
  productForm!: FormGroup;
  selectedFile: File | null = null;
  imagePreview: string | null = null;
  successMessage: string = '';
  errorMessage: string = '';

  categories: any[] = [];
  isAddingNewCategory = false;
  newCategoryName = '';

  constructor(
    private fb: FormBuilder,
    private productService: ProductService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.productForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      description: ['', Validators.required],
      categoryId: ['', Validators.required],
      pricePerKg: [null, [Validators.required, Validators.min(1)]],
      availableStockKg: [null, [Validators.required, Validators.min(0)]],
      imageUrl: [''] 
    });

    this.loadCategories();
  }

  loadCategories(): void {
    this.productService.getCategories().subscribe({
      next: (data) => this.categories = data,
      error: (err) => console.error('Failed to load categories', err)
    });
  }

  onCategoryChange(event: any): void {
    if (event.target.value === 'NEW') {
      this.isAddingNewCategory = true;
      this.productForm.get('categoryId')?.setValue(''); // Clear standard validation since it's hidden
    }
  }

  saveNewCategory(): void {
    if (!this.newCategoryName.trim()) return;
    this.productService.addCategory({ name: this.newCategoryName }).subscribe({
      next: (cat) => {
        this.categories.push(cat);
        this.isAddingNewCategory = false;
        this.productForm.get('categoryId')?.setValue(cat.id);
        this.newCategoryName = '';
      },
      error: (err) => {
        this.errorMessage = 'Failed to create category';
        console.error(err);
      }
    });
  }

  cancelNewCategory(): void {
    this.isAddingNewCategory = false;
    this.newCategoryName = '';
  }

  // New method: Captures the file when you choose it in the HTML
  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      
      // Create preview
      const reader = new FileReader();
      reader.onload = () => {
        this.imagePreview = reader.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  onSubmit(): void {
    this.errorMessage = '';
    this.successMessage = '';

    // 1. Validation Check
    if (this.productForm.invalid) {
      this.errorMessage = 'Please fill all text fields correctly.';
      return;
    }

    if (!this.selectedFile) {
      this.errorMessage = 'Please select an image file from your PC.';
      return;
    }

    console.log('Step 1: Creating Product...');

    const formValue = this.productForm.value;
    const productPayload = {
      name: formValue.name,
      description: formValue.description,
      pricePerKg: formValue.pricePerKg,
      availableStockKg: formValue.availableStockKg,
      category: { id: formValue.categoryId }
    };

    // 2. Submit Product Data (Step 1)
    this.productService.addProduct(productPayload).subscribe({
      next: (savedProduct: any) => {
        console.log('Product Created! ID:', savedProduct.id);
        
        // 3. Upload Image using the new ID (Step 2)
        this.uploadImage(savedProduct.id);
      },
      error: (err) => {
        console.error('Error creating product:', err);
        this.errorMessage = 'Failed to create product. Check console.';
      }
    });
  }

  // Helper method to handle the image upload
  uploadImage(productId: number): void {
    if (!this.selectedFile) return;

    console.log('Step 2: Uploading Image...');

    this.productService.uploadProductImage(productId, this.selectedFile).subscribe({
      next: (response) => {
        console.log('Image Upload Success:', response);
        this.successMessage = 'Product and Image saved successfully!';
        
        // Redirect after 1.5 seconds
        setTimeout(() => {
          this.router.navigate(['/inventory-dashboard']);
        }, 1500);
      },
      error: (err) => {
        console.error('Image Upload Failed:', err);
        this.errorMessage = 'Product created, but Image Upload failed.';
      }
    });
  }
}