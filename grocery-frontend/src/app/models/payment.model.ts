export type PaymentStatus =
  | 'CREATED'
  | 'PAID'
  | 'FAILED'
  | 'REFUNDED'
  | 'NOT_APPLICABLE';  // Sentinel for legacy orders without a payment row

export interface PaymentAttempt {
  id: number;
  razorpayOrderId: string;
  razorpayPaymentId: string | null;
  amount: number;              // paise
  currency: string;
  status: PaymentStatus;
  attempts: number;
  createdAt: string;           // ISO timestamp
  verifiedAt: string | null;
  failureReason: string | null;
}

export interface OrderPaymentTimeline {
  orderId: number;
  totalAmount: number;
  effectiveStatus: PaymentStatus;
  attempts: PaymentAttempt[];
}

export interface PaymentListItem {
  paymentId: number;
  orderId: number;
  customerName: string;
  customerPhone: string;
  amount: number;
  status: PaymentStatus;
  attempts: number;
  razorpayOrderId: string;
  razorpayPaymentId: string | null;
  createdAt: string;
  isGuestOrder: boolean;
}
