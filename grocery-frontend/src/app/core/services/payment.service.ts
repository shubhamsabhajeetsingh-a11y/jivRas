import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { OrderPaymentTimeline, PaymentListItem } from '../../models/payment.model';

// ── DTOs matching the backend PaymentController contract ──────────────────────

export interface CreatePaymentOrderResponse {
  razorpayOrderId: string;
  razorpayKeyId: string;  // public key — safe to use in frontend; secret never leaves backend
  amount: number;         // in paise (₹1 = 100 paise)
  currency: string;
  jivrasOrderId: number;
}

export interface VerifyPaymentRequest {
  razorpayOrderId: string;
  razorpayPaymentId: string;
  razorpaySignature: string;
  jivrasOrderId: number;
}

export interface VerifyPaymentResponse {
  success: boolean;
  message: string;
  orderStatus: string;
}

// ─────────────────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class PaymentService {

  private baseUrl = `${environment.apiUrl}/api/payments`;

  constructor(private http: HttpClient) {}

  /** POST /api/payments/create-order — call before showing Razorpay modal */
  createRazorpayOrder(jivrasOrderId: number): Observable<CreatePaymentOrderResponse> {
    return this.http.post<CreatePaymentOrderResponse>(
      `${this.baseUrl}/create-order`,
      { orderId: jivrasOrderId }
    );
  }

  /** POST /api/payments/verify — call from Razorpay success handler */
  verifyPayment(payload: VerifyPaymentRequest): Observable<VerifyPaymentResponse> {
    return this.http.post<VerifyPaymentResponse>(
      `${this.baseUrl}/verify`,
      payload
    );
  }

  getTimelineForOrder(orderId: number): Observable<OrderPaymentTimeline> {
    return this.http.get<OrderPaymentTimeline>(`${this.baseUrl}/by-order/${orderId}`);
  }

  getAllPayments(): Observable<PaymentListItem[]> {
    return this.http.get<PaymentListItem[]>(`${this.baseUrl}/list`);
  }
}
