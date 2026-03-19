import { Component, OnInit, Inject, PLATFORM_ID, ChangeDetectorRef } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CartService } from '../core/services/cart.service';
import { OrderService } from '../core/services/order.service';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.css']
})
export class CheckoutComponent implements OnInit {

  checkoutForm!: FormGroup;
  cart: any = null;
  loading = true;
  submitting = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private cartService: CartService,
    private orderService: OrderService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    // Build delivery details form with validation
    this.checkoutForm = this.fb.group({
      customerName: ['', [Validators.required, Validators.minLength(2)]],
      mobile: ['', [Validators.required, Validators.pattern('^[0-9]{10}$')]],
      addressLine: ['', Validators.required],
      city: ['', Validators.required],
      state: ['', Validators.required],
      pincode: ['', [Validators.required, Validators.pattern('^[0-9]{6}$')]]
    });

    // Load cart summary
    if (isPlatformBrowser(this.platformId)) {
      this.loadCart();
    }
  }

  loadCart(): void {
    this.cartService.getCart().subscribe({
      next: (data) => {
        this.cart = data;
        this.loading = false;
        // If cart is empty, redirect back
        if (!data.items || data.items.length === 0) {
          this.router.navigate(['/cart']);
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
        this.router.navigate(['/cart']);
      }
    });
  }

  onSubmit(): void {
    if (this.checkoutForm.invalid) {
      // Mark all fields as touched to show validation errors
      Object.keys(this.checkoutForm.controls).forEach(key => {
        this.checkoutForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.submitting = true;
    this.errorMessage = '';

    this.orderService.checkout(this.checkoutForm.value).subscribe({
      next: (order) => {
        this.submitting = false;
        this.cartService.resetCartCount();
        this.cdr.detectChanges();
        // Navigate to order confirmation with the order ID
        this.router.navigate(['/order-confirmation', order.orderId]);
      },
      error: (err) => {
        this.submitting = false;
        this.errorMessage = err.error?.message || 'Checkout failed. Please try again.';
        console.error('Checkout error:', err);
        this.cdr.detectChanges();
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/cart']);
  }
}
