import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrdersService, AdminOrderResponse } from '../core/services/orders.service';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './orders.component.html',
  styleUrl: './orders.component.css'
})
export class OrdersComponent implements OnInit {

  groupedOrders: { [category: string]: AdminOrderResponse[] } = {};
  categoryKeys: string[] = [];
  expandedCategories: Set<string> = new Set<string>();
  expandedOrders: Set<number> = new Set<number>();
  
  isLoading: boolean = false;
  errorMessage: string = '';

  // Summary Stats
  totalOrders: number = 0;
  pendingOrders: number = 0;
  deliveredOrders: number = 0;
  cancelledOrders: number = 0;

  // Status updating state
  statusError: { [orderId: number]: string } = {};

  availableStatuses = ['CONFIRMED', 'DISPATCHED', 'DELIVERED', 'CANCELLED'];

  constructor(
    private ordersService: OrdersService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.isLoading = true;
    this.ordersService.getOrdersGroupedByCategory().subscribe({
      next: (data) => {
        this.groupedOrders = data;
        this.categoryKeys = Object.keys(data);
        this.calculateStats(data);
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isLoading = false;
        if (err.status === 401) {
          this.errorMessage = 'Session expired. Please login again.';
        } else if (err.status === 0) {
          this.errorMessage = 'Cannot connect to server. Is backend running?';
        } else {
          this.errorMessage = 'Failed to load orders. Error: ' + err.status;
        }
        this.cdr.detectChanges();
      }
    });
  }

  calculateStats(data: { [category: string]: AdminOrderResponse[] }): void {
    let total = 0;
    let delivered = 0;
    let cancelled = 0;
    let pending = 0;

    const countedIds = new Set<number>();

    Object.values(data).forEach(orders => {
      orders.forEach(order => {
        if (!countedIds.has(order.orderId)) {
          countedIds.add(order.orderId);
          total++;
          const st = order.orderStatus?.toUpperCase() || '';
          if (st === 'DELIVERED') delivered++;
          else if (st === 'CANCELLED') cancelled++;
          else pending++;
        }
      });
    });

    this.totalOrders = total;
    this.deliveredOrders = delivered;
    this.cancelledOrders = cancelled;
    this.pendingOrders = pending;
  }

  toggleCategory(category: string): void {
    if (this.expandedCategories.has(category)) {
      this.expandedCategories.delete(category);
    } else {
      this.expandedCategories.add(category);
    }
  }

  toggleOrderItems(orderId: number): void {
    if (this.expandedOrders.has(orderId)) {
      this.expandedOrders.delete(orderId);
    } else {
      this.expandedOrders.add(orderId);
    }
  }

  updateOrderStatus(order: AdminOrderResponse, event: any): void {
    const newStatus = event.target.value;
    if (!newStatus || order.orderStatus === newStatus) return;
    
    this.statusError[order.orderId] = '';
    
    this.ordersService.updateOrderStatus(order.orderId, newStatus).subscribe({
      next: () => {
        order.orderStatus = newStatus;
        this.calculateStats(this.groupedOrders);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.statusError[order.orderId] = 'Failed to update status. Please try again.';
        event.target.value = order.orderStatus; // reset to previous
        this.cdr.detectChanges();
      }
    });
  }

  getStatusClass(status: string): string {
    if (!status) return 'badge-grey';
    switch (status.toUpperCase()) {
      case 'CREATED': return 'badge-grey';
      case 'CONFIRMED': return 'badge-blue';
      case 'DISPATCHED': return 'badge-orange';
      case 'DELIVERED': return 'badge-green';
      case 'CANCELLED': return 'badge-red';
      default: return 'badge-grey';
    }
  }

  getBorderColor(status: string): string {
    if (!status) return '#e0e0e0';
    switch (status.toUpperCase()) {
      case 'CREATED': return '#9e9e9e';
      case 'CONFIRMED': return '#1976d2';
      case 'DISPATCHED': return '#e65100';
      case 'DELIVERED': return '#2e7d32';
      case 'CANCELLED': return '#c62828';
      default: return '#e0e0e0';
    }
  }
}
