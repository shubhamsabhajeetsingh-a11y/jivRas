import { Component, OnInit, Inject, PLATFORM_ID, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { ProductService } from '../core/services/product.service';
import { UserService } from '../core/services/user.service';
import { BranchService } from '../core/services/branch.service';
import { InventoryService } from '../core/services/inventory.service';
import { Router, RouterLink } from '@angular/router';
import { UserProfile, Branch } from '../models/inventory.model';

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
  isSubmitting: boolean = false;

  // User + Branch state
  currentUser?: UserProfile;
  branches: Branch[] = [];
  selectedBranchId: number | null = null;
  isLoadingProfile: boolean = false;

  // Category state
  categories: any[] = [];
  isAddingNewCategory = false;
  newCategoryName = '';

  constructor(
    private fb: FormBuilder,
    private productService: ProductService,
    private userService: UserService,
    private branchService: BranchService,
    private inventoryService: InventoryService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    this.productForm = this.fb.group({
      name:               ['', [Validators.required, Validators.minLength(3)]],
      description:        ['', Validators.required],
      categoryId:         ['', Validators.required],
      pricePerKg:         [null, [Validators.required, Validators.min(0.01)]],
      availableStockKg:   [null, [Validators.required, Validators.min(0)]],
      lowStockThreshold:  [5,   [Validators.required, Validators.min(0)]],
      imageUrl:           ['']
    });

    if (isPlatformBrowser(this.platformId)) {
      this.isLoadingProfile = true;
      this.loadUserProfile();
      this.loadCategories();
    }
  }

  get isAdmin(): boolean { return this.currentUser?.role === 'ADMIN'; }
  get isBranchManager(): boolean { return this.currentUser?.role === 'BRANCH_MANAGER'; }

  // ── Data loaders ────────────────────────────────────────────────────

  loadUserProfile(): void {
    this.userService.getUserProfile().subscribe({
      next: (profile) => {
        this.currentUser = profile;
        this.isLoadingProfile = false;

        if (profile.role === 'ADMIN') {
          this.loadBranches();
        } else {
          // BRANCH_MANAGER or EMPLOYEE — use their own branchId
          this.selectedBranchId = profile.branchId > 0 ? profile.branchId : null;
        }
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load user profile:', err);
        this.errorMessage = 'Could not load user profile. Please refresh the page.';
        this.isLoadingProfile = false;
        this.cdr.detectChanges();
      }
    });
  }

  loadBranches(): void {
    this.branchService.getAllActiveBranches().subscribe({
      next: (branches) => {
        this.branches = branches;
        if (branches.length > 0) {
          this.selectedBranchId = branches[0].id;
        }
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load branches:', err);
        this.cdr.detectChanges();
      }
    });
  }

  loadCategories(): void {
    this.productService.getCategories().subscribe({
      next: (data) => {
        this.categories = data;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Failed to load categories', err)
    });
  }

  // ── Category helpers ────────────────────────────────────────────────

  onCategoryChange(event: any): void {
    if (event.target.value === 'NEW') {
      this.isAddingNewCategory = true;
      this.productForm.get('categoryId')?.setValue('');
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
        this.errorMessage = 'Failed to create category.';
        console.error(err);
      }
    });
  }

  cancelNewCategory(): void {
    this.isAddingNewCategory = false;
    this.newCategoryName = '';
  }

  // ── Image handling ──────────────────────────────────────────────────

  onFileSelected(event: any): void {
    const file: File = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      const reader = new FileReader();
      reader.onload = () => { this.imagePreview = reader.result as string; };
      reader.readAsDataURL(file);
    }
  }

  // ── Submit ──────────────────────────────────────────────────────────

  onSubmit(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (this.productForm.invalid) {
      this.errorMessage = 'Please fill all required fields correctly.';
      return;
    }
    if (!this.selectedFile) {
      this.errorMessage = 'Please select a product image.';
      return;
    }
    if (!this.selectedBranchId) {
      this.errorMessage = this.isAdmin
        ? 'Please select a branch.'
        : 'No branch assigned to your account. Contact admin.';
      return;
    }

    this.isSubmitting = true;

    const fv = this.productForm.value;
    const productPayload = {
      name:             fv.name,
      description:      fv.description,
      pricePerKg:       fv.pricePerKg,
      availableStockKg: fv.availableStockKg,
      category:         { id: fv.categoryId }
    };

    // Step 1 — Create product globally
    this.productService.addProduct(productPayload).subscribe({
      next: (savedProduct: any) => {
        // Step 2 — Upload image
        this.productService.uploadProductImage(savedProduct.id, this.selectedFile!).subscribe({
          next: () => {
            // Step 3 — Add to branch inventory
            this.addToInventory(savedProduct.id);
          },
          error: (err) => {
            console.error('Image upload failed:', err);
            // Image failed but product created — continue with inventory anyway
            this.addToInventory(savedProduct.id);
          }
        });
      },
      error: (err) => {
        console.error('Product creation failed:', err);
        this.errorMessage = 'Failed to create product. Please try again.';
        this.isSubmitting = false;
      }
    });
  }

  private addToInventory(productId: number): void {
    const fv = this.productForm.value;
    const stockPayload = {
      branchId:          this.selectedBranchId!,
      productId:         productId,
      availableStockKg:  fv.availableStockKg,
      lowStockThreshold: fv.lowStockThreshold
    };

    this.inventoryService.updateStock(stockPayload).subscribe({
      next: () => {
        this.successMessage = '✅ Product added to branch inventory successfully!';
        this.isSubmitting = false;
        setTimeout(() => this.router.navigate(['/inventory-dashboard']), 1500);
      },
      error: (err) => {
        console.error('Inventory stock failed:', err);
        this.errorMessage =
          '⚠️ Product was created globally, but adding it to branch inventory failed. ' +
          'Please add it manually from the inventory dashboard.';
        this.isSubmitting = false;
      }
    });
  }
}