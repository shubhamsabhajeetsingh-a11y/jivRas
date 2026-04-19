import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
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
}

@Injectable({
  providedIn: 'root'
})
export class OrdersService {

  private apiUrl = `${environment.apiUrl}/api/orders/admin`;

  constructor(private http: HttpClient) {}

  getOrdersGroupedByCategory(): Observable<{ [category: string]: AdminOrderResponse[] }> {
    return this.http.get<{ [category: string]: AdminOrderResponse[] }>(`${this.apiUrl}/grouped-by-category`);
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
