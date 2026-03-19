import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';

/**
 * Service for shopping cart operations.
 * Maintains a cartCount$ observable for the navbar badge.
 */
@Injectable({
  providedIn: 'root'
})
export class CartService {

  private baseUrl = `${environment.apiUrl}/api/cart`;

  /** Observable cart item count — used by navbar to show badge */
  private cartCountSubject = new BehaviorSubject<number>(0);
  cartCount$ = this.cartCountSubject.asObservable();

  constructor(private http: HttpClient) {}

  /** Add a product to the cart */
  addToCart(productId: number, quantity: number): Observable<any> {
    return this.http.post(`${this.baseUrl}/add`, { productId, quantity }).pipe(
      tap((cart: any) => this.cartCountSubject.next(cart.items?.length || 0))
    );
  }

  /** Get the current cart */
  getCart(): Observable<any> {
    return this.http.get(this.baseUrl).pipe(
      tap((cart: any) => this.cartCountSubject.next(cart.items?.length || 0))
    );
  }

  /** Update quantity of a cart item */
  updateCartItem(itemId: number, quantity: number): Observable<any> {
    return this.http.put(`${this.baseUrl}/update/${itemId}`, { quantity }).pipe(
      tap((cart: any) => this.cartCountSubject.next(cart.items?.length || 0))
    );
  }

  /** Remove a cart item */
  removeCartItem(itemId: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/${itemId}`).pipe(
      tap((cart: any) => this.cartCountSubject.next(cart.items?.length || 0))
    );
  }

  /** Reset cart count (called after checkout or when cart is empty) */
  resetCartCount(): void {
    this.cartCountSubject.next(0);
  }
}
