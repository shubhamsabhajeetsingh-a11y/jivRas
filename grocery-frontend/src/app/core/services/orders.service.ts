import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { PaymentStatus, OrderPaymentTimeline } from '../../models/payment.model';

export interface OrderItemResponse {
  id: number;
  productId: number;
  productName: string;
  quantityKg: number;
  pricePerKg: number;
  subtotal: number;
}

export interface AdminOrderResponse {
  orderId: number;
  orderStatus: string;
  orderDate: string;
  totalAmount: number;
  estimatedDeliveryDays: number;
  customerName: string;
  mobile: string;
  addressLine: string;
  city: string;
  state: string;
  pincode: string;
  items: OrderItemResponse[];
  latestPaymentStatus?: PaymentStatus | null;
  paymentDetailsExpanded?: boolean;
  paymentTimeline?: OrderPaymentTimeline;
  isGuest?: boolean;          // true when the order was placed without an account
  customerId?: number | null; // null for guest orders
}

@Injectable({
  providedIn: 'root'
})
export class OrdersService {

  private apiUrl = `${environment.apiUrl}/api/orders/admin`;

  constructor(private http: HttpClient) {}

  /**
   * Fetch orders grouped by product category.
   * @param guestOnly null = all, true = guests only, false = registered only
   */
  getOrdersGroupedByCategory(guestOnly?: boolean | null): Observable<{ [category: string]: AdminOrderResponse[] }> {
    let params = new HttpParams();
    // Only set the param if a value is explicitly requested
    if (guestOnly === true)  params = params.set('guestOnly', 'true');
    if (guestOnly === false) params = params.set('guestOnly', 'false');
    return this.http.get<{ [category: string]: AdminOrderResponse[] }>(`${this.apiUrl}/grouped-by-category`, { params });
  }

  updateOrderStatus(id: number, status: string): Observable<any> {
    return this.http.patch(`${environment.apiUrl}/api/orders/${id}/status`, { status });
  }

  getOrderTimeline(orderId: number): Observable<any[]> {
    return this.http.get<any[]>(`${environment.apiUrl}/api/orders/${orderId}/timeline`);
  }

  downloadInvoice(orderId: number): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/api/orders/${orderId}/invoice`, { responseType: 'blob' });
  }
}
