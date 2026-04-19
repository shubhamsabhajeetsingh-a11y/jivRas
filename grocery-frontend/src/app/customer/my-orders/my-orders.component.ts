import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { OrderService } from '../../core/services/order.service';
import { CustomerOrderSummary } from '../../models/order.model';

@Component({
  selector: 'app-my-orders',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './my-orders.component.html',
  styleUrls: ['./my-orders.component.css']
})
export class MyOrdersComponent implements OnInit {
  orders: CustomerOrderSummary[] = [];
  loading = true;
  error: string | null = null;

  // Client-side filter — no backend round-trip needed since all orders are already loaded
  activeFilter: 'ALL' | 'ACTIVE' | 'DELIVERED' | 'CANCELLED' = 'ALL';

  // Cancel flow state
  cancellingOrderId: number | null = null;
  cancelConfirmOrderId: number | null = null;  // order currently shown in the confirm dialog

  constructor(
    private orderService: OrderService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit() {
    if (!isPlatformBrowser(this.platformId)) return;

    // Guest users have no JWT; redirect them to login
    const role = localStorage.getItem('userRole');
    const token = localStorage.getItem('accessToken');
    if (!token || role === 'GUEST' || !role) {
      this.router.navigate(['/login']);
      return;
    }

    this.loadOrders();
  }

  loadOrders() {
    this.loading = true;
    this.error = null;
    this.orderService.getMyOrders().subscribe({
      next: (data) => {
        this.orders = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Could not load your orders. Please try again.';
        this.loading = false;
        console.error(err);
      }
    });
  }

  get filteredOrders(): CustomerOrderSummary[] {
    if (this.activeFilter === 'ALL') return this.orders;
    if (this.activeFilter === 'ACTIVE') {
      // Active = anything not yet delivered or cancelled
      return this.orders.filter(o => o.status !== 'DELIVERED' && o.status !== 'CANCELLED');
    }
    return this.orders.filter(o => o.status === this.activeFilter);
  }

  setFilter(f: 'ALL' | 'ACTIVE' | 'DELIVERED' | 'CANCELLED') {
    this.activeFilter = f;
  }

  openCancelConfirm(orderId: number) {
    this.cancelConfirmOrderId = orderId;
  }

  closeCancelConfirm() {
    this.cancelConfirmOrderId = null;
  }

  confirmCancel() {
    if (!this.cancelConfirmOrderId) return;
    const id = this.cancelConfirmOrderId;
    this.cancellingOrderId = id;
    this.closeCancelConfirm();

    this.orderService.cancelMyOrder(id).subscribe({
      next: () => {
        this.cancellingOrderId = null;
        // Refresh the list so status chip and canCancel flag update
        this.loadOrders();
      },
      error: (err) => {
        this.cancellingOrderId = null;
        const msg = err?.error?.message || 'Could not cancel order. Please try again.';
        alert(msg);  // TODO: replace with toast/snackbar component when available
      }
    });
  }

  statusChipClass(status: string): string {
    return 'status-' + status.toLowerCase();
  }

  paymentChipLabel(status: string): string {
    switch (status) {
      case 'PAID':           return '💳 Paid';
      case 'CREATED':        return '⏳ Pending';
      case 'FAILED':         return '❌ Failed';
      case 'REFUNDED':       return '↩️ Refunded';
      default:               return '— N/A';
    }
  }
}
