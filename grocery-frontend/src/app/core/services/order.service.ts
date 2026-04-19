import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CustomerOrderSummary } from '../../models/order.model';

/**
 * Service for customer-facing order operations (checkout, order history, self-cancel).
 * Admin order operations live in orders.service.ts.
 */
@Injectable({
  providedIn: 'root'
})
export class OrderService {

  private baseUrl = `${environment.apiUrl}/api/orders`;

  constructor(private http: HttpClient) {}

  /** Checkout the cart — sends delivery details, backend creates the order */
  checkout(deliveryDetails: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/checkout`, deliveryDetails);
  }

  /** Get order by ID */
  getOrder(orderId: number): Observable<any> {
    return this.http.get(`${this.baseUrl}/${orderId}`);
  }

  /** Get logged-in customer's own order history — backend auto-scopes to their userId */
  getMyOrders(): Observable<CustomerOrderSummary[]> {
    return this.http.get<CustomerOrderSummary[]>(`${this.baseUrl}/my-orders`);
  }

  /** Self-cancel: allowed only for CREATED orders with no PAID payment */
  cancelMyOrder(orderId: number): Observable<any> {
    return this.http.post(`${this.baseUrl}/${orderId}/cancel`, {});
  }
}

