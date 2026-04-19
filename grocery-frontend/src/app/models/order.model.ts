import { PaymentStatus } from './payment.model';

/**
 * Slim summary of a customer's order — returned by GET /api/orders/my-orders.
 * Full details available via GET /api/orders/:id (order-confirmation route).
 */
export interface CustomerOrderSummary {
  orderId: number;
  orderDate: string;           // ISO timestamp
  status: string;              // CREATED / CONFIRMED / DISPATCHED / DELIVERED / CANCELLED
  paymentStatus: PaymentStatus;
  totalAmount: number;         // rupees (backend converts from paise)
  itemCount: number;
  firstItemName: string;
  additionalItemCount: number;
  canCancel: boolean;          // true only when status == CREATED and no PAID payment exists
}
