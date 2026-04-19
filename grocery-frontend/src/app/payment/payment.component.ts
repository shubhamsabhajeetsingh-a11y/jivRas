import { Component, OnInit, NgZone, Inject, PLATFORM_ID, ChangeDetectorRef } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { OrderService } from '../core/services/order.service';
import {
  PaymentService,
  CreatePaymentOrderResponse,
  VerifyPaymentRequest
} from '../core/services/payment.service';

// Razorpay is loaded as a global script via index.html
declare var Razorpay: any;

type PaymentState = 'LOADING' | 'READY' | 'PROCESSING' | 'FAILED';

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './payment.component.html',
  styleUrls: ['./payment.component.css']
})
export class PaymentComponent implements OnInit {

  orderId!: number;
  order: any = null;
  state: PaymentState = 'LOADING';
  failureReason = '';

  // Stored after createRazorpayOrder — reused when retrying
  private razorpayOrderResponse: CreatePaymentOrderResponse | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private ngZone: NgZone,
    private orderService: OrderService,
    private paymentService: PaymentService,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    this.orderId = +this.route.snapshot.paramMap.get('orderId')!;
    if (!this.orderId) {
      this.router.navigate(['/products']);
      return;
    }

    this.state = 'LOADING';

    // Load order details for display, then initialise Razorpay order
    this.orderService.getOrder(this.orderId).subscribe({
      next: (order) => {
        this.order = order;
        this.createPaymentOrder();
      },
      error: () => this.setFailed('Could not load order details. Please try again.')
    });
  }

  /** Calls backend to create a Razorpay order and transitions to READY state */
  createPaymentOrder(): void {
    this.paymentService.createRazorpayOrder(this.orderId).subscribe({
      next: (res) => {
        this.razorpayOrderResponse = res;
        this.state = 'READY';
        this.cdr.detectChanges();
      },
      error: (err) => this.setFailed(err.error?.message || 'Could not initialise payment. Please try again.')
    });
  }

  /** Opens the Razorpay checkout modal */
  openRazorpay(): void {
    const res = this.razorpayOrderResponse!;

    const options = {
      key: res.razorpayKeyId,          // public key from backend — never hardcode
      amount: res.amount,              // in paise
      currency: res.currency,
      order_id: res.razorpayOrderId,
      name: 'JivRas Groceries',
      description: `Order #${this.orderId}`,
      prefill: {
        name: this.order?.customerName || '',
        contact: this.order?.customerPhone || this.order?.mobile || '',
        email: this.order?.customerEmail || ''
      },
      theme: { color: '#7c3aed' },     // match the purple Place Order button
      handler: (response: any) => {
        // NgZone.run required — Razorpay success callback fires outside Angular zone
        this.ngZone.run(() => this.handlePaymentSuccess(response));
      },
      modal: {
        ondismiss: () => {
          // User closed modal without paying
          this.ngZone.run(() => this.setFailed('Payment cancelled. Please retry.'));
        }
      }
    };

    const rzp = new Razorpay(options);

    rzp.on('payment.failed', (resp: any) => {
      this.ngZone.run(() =>
        this.setFailed(resp.error?.description || 'Payment failed. Please retry.')
      );
    });

    rzp.open();
  }

  /** Called from Razorpay handler after user completes payment in modal */
  handlePaymentSuccess(response: any): void {
    this.state = 'PROCESSING';
    this.cdr.detectChanges();

    const verifyReq: VerifyPaymentRequest = {
      razorpayOrderId: response.razorpay_order_id,
      razorpayPaymentId: response.razorpay_payment_id,
      razorpaySignature: response.razorpay_signature,
      jivrasOrderId: this.orderId
    };

    this.paymentService.verifyPayment(verifyReq).subscribe({
      next: (verifyRes) => {
        if (verifyRes.success) {
          this.router.navigate(['/order-confirmation', this.orderId]);
        } else {
          this.setFailed(verifyRes.message || 'Payment verification failed.');
        }
      },
      error: () => this.setFailed('Payment verification error. Please contact support if amount was deducted.')
    });
  }

  /** Creates a fresh Razorpay order and reopens the modal */
  retryPayment(): void {
    // A fresh Razorpay order is required — the previous one may be in ATTEMPTED state
    this.state = 'LOADING';
    this.razorpayOrderResponse = null;
    this.cdr.detectChanges();
    this.createPaymentOrder();
  }

  private setFailed(msg: string): void {
    this.state = 'FAILED';
    this.failureReason = msg;
    this.cdr.detectChanges();
  }

  /** Formats paise to rupees for display */
  get amountInRupees(): number {
    return this.razorpayOrderResponse ? this.razorpayOrderResponse.amount / 100 : 0;
  }
}
