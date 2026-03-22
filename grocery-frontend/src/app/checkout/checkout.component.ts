import { Component, OnInit, Inject, PLATFORM_ID, ChangeDetectorRef, HostListener, ElementRef } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CartService } from '../core/services/cart.service';
import { OrderService } from '../core/services/order.service';
import { LocationService } from '../core/services/location.service';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.css']
})
export class CheckoutComponent implements OnInit {

  checkoutForm!: FormGroup;
  cart: any = null;
  loading = true;
  submitting = false;
  errorMessage = '';

  // Location data
  allStates: string[] = [];
  filteredStates: string[] = [];
  allCities: string[] = [];
  filteredCities: string[] = [];
  validPincodes: string[] = [];

  // Search inputs
  stateSearch = '';
  citySearch = '';

  // Dropdown visibility
  stateDropdownOpen = false;
  cityDropdownOpen = false;

  // Selected values (display)
  selectedState = '';
  selectedCity = '';

  // Pincode validation
  pincodeInvalid = false;
  pincodeValidationMsg = '';

  constructor(
    private fb: FormBuilder,
    private cartService: CartService,
    private orderService: OrderService,
    private locationService: LocationService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private elRef: ElementRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    this.checkoutForm = this.fb.group({
      customerName: ['', [Validators.required, Validators.minLength(2)]],
      mobile: ['', [Validators.required, Validators.pattern('^[0-9]{10}$')]],
      addressLine: ['', Validators.required],
      state: ['', Validators.required],
      city: ['', Validators.required],
      pincode: ['', [Validators.required, Validators.pattern('^[0-9]{6}$')]]
    });

    if (isPlatformBrowser(this.platformId)) {
      this.loadCart();
      this.loadStates();
    }
  }

  // Close dropdowns when clicking outside
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event): void {
    if (!this.elRef.nativeElement.contains(event.target)) {
      this.stateDropdownOpen = false;
      this.cityDropdownOpen = false;
    }
  }

  loadCart(): void {
    this.cartService.getCart().subscribe({
      next: (data) => {
        this.cart = data;
        this.loading = false;
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

  loadStates(): void {
    this.locationService.getStates().subscribe({
      next: (states) => {
        this.allStates = states;
        this.filteredStates = [...states];
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Failed to load states:', err)
    });
  }

  // --- State Dropdown ---
  toggleStateDropdown(event: Event): void {
    event.stopPropagation();
    this.stateDropdownOpen = !this.stateDropdownOpen;
    this.cityDropdownOpen = false;
    if (this.stateDropdownOpen) {
      this.stateSearch = '';
      this.filteredStates = [...this.allStates];
    }
  }

  filterStates(): void {
    const search = this.stateSearch.toLowerCase();
    this.filteredStates = this.allStates.filter(s => s.toLowerCase().includes(search));
  }

  selectState(state: string): void {
    this.selectedState = state;
    this.checkoutForm.patchValue({ state, city: '', pincode: '' });
    this.stateDropdownOpen = false;
    this.stateSearch = '';

    // Reset city & pincode
    this.selectedCity = '';
    this.allCities = [];
    this.filteredCities = [];
    this.validPincodes = [];
    this.pincodeInvalid = false;
    this.pincodeValidationMsg = '';

    // Fetch cities for selected state
    this.locationService.getCities(state).subscribe({
      next: (cities) => {
        this.allCities = cities;
        this.filteredCities = [...cities];
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Failed to load cities:', err)
    });
  }

  // --- City Dropdown ---
  toggleCityDropdown(event: Event): void {
    event.stopPropagation();
    if (!this.selectedState) return; // don't open if no state selected
    this.cityDropdownOpen = !this.cityDropdownOpen;
    this.stateDropdownOpen = false;
    if (this.cityDropdownOpen) {
      this.citySearch = '';
      this.filteredCities = [...this.allCities];
    }
  }

  filterCities(): void {
    const search = this.citySearch.toLowerCase();
    this.filteredCities = this.allCities.filter(c => c.toLowerCase().includes(search));
  }

  selectCity(city: string): void {
    this.selectedCity = city;
    this.checkoutForm.patchValue({ city, pincode: '' });
    this.cityDropdownOpen = false;
    this.citySearch = '';
    this.pincodeInvalid = false;
    this.pincodeValidationMsg = '';

    // Fetch pincodes for selected city
    this.locationService.getPincodes(this.selectedState, city).subscribe({
      next: (pincodes) => {
        this.validPincodes = pincodes;
        // Auto-fill first pincode
        if (pincodes.length > 0) {
          this.checkoutForm.patchValue({ pincode: pincodes[0] });
        }
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Failed to load pincodes:', err)
    });
  }

  // --- Pincode Validation ---
  onPincodeChange(): void {
    const pincode = this.checkoutForm.get('pincode')?.value;
    if (pincode && this.validPincodes.length > 0 && !this.validPincodes.includes(pincode)) {
      this.pincodeInvalid = true;
      this.pincodeValidationMsg = `Pincode ${pincode} is not valid for ${this.selectedCity}, ${this.selectedState}. Valid pincodes: ${this.validPincodes.join(', ')}`;
    } else {
      this.pincodeInvalid = false;
      this.pincodeValidationMsg = '';
    }
  }

  onSubmit(): void {
    if (this.checkoutForm.invalid || this.pincodeInvalid) {
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
