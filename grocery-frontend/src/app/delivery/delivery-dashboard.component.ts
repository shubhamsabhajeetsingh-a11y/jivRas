import { Component, OnInit, Inject, PLATFORM_ID, ChangeDetectorRef } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router, RouterModule } from '@angular/router';
import { environment } from '../environments/environment';

/** Shape of the DeliveryOrderDto returned by GET /api/orders/my-deliveries */
interface DeliveryOrderDto {
  orderId: number;
  orderStatus: string;
  orderDate: string;
  totalAmount: number;
  customerName: string;
  mobile: string;
  addressLine: string;
  city: string;
  state: string;
  pincode: string;
  estimatedDeliveryTime: string | null;
  firstItemName: string;
  additionalItemCount: number;
  items: { productName: string; quantityKg: number; pricePerKg: number; subtotal: number }[];
}

@Component({
  selector: 'app-delivery-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './delivery-dashboard.component.html',
  styleUrls: ['./delivery-dashboard.component.css']
})
export class DeliveryDashboardComponent implements OnInit {

  orders: DeliveryOrderDto[] = [];
  loading = true;
  error: string | null = null;

  // ID of the order currently being marked — used to show "Marking…" on the button
  markingOrderId: number | null = null;

  constructor(
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    // Guard: only DELIVERY_AGENT users may see this page
    const role  = localStorage.getItem('userRole');
    const token = localStorage.getItem('accessToken');
    if (!token || role !== 'DELIVERY_AGENT') {
      this.router.navigate(['/login']);
      return;
    }

    this.loadDeliveries();
  }

  /** Fetches the agent's active (OUT_FOR_DELIVERY) orders from the backend. */
  loadDeliveries(): void {
    this.loading = true;
    this.error   = null;

    this.http.get<DeliveryOrderDto[]>(`${environment.apiUrl}/api/orders/my-deliveries`).subscribe({
      next: (data) => {
        this.orders  = data;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error   = 'Failed to load deliveries. Please try again.';
        this.loading = false;
        console.error(err);
        this.cdr.detectChanges();
      }
    });
  }

  /**
   * Calls PATCH /api/orders/{id}/mark-delivered.
   * On success the card is removed from the list immediately — no reload needed.
   */
  markDelivered(orderId: number): void {
    this.markingOrderId = orderId;

    this.http.patch(`${environment.apiUrl}/api/orders/${orderId}/mark-delivered`, {}).subscribe({
      next: () => {
        // Drop the delivered order from the live list
        this.orders         = this.orders.filter(o => o.orderId !== orderId);
        this.markingOrderId = null;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.markingOrderId = null;
        const msg = err?.error?.message || 'Failed to mark as delivered. Please try again.';
        alert(msg);
        this.cdr.detectChanges();
      }
    });
  }
}
