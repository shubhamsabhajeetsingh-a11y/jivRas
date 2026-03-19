import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

/**
 * Service for order/checkout operations.
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
}
