import { Component, OnInit } from '@angular/core';
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

  constructor(private ordersService: OrdersService) {}

  ngOnInit(): void {
    this.isLoading = true;
    this.ordersService.getOrdersGroupedByCategory().subscribe({
      next: (data) => {
        this.groupedOrders = data;
        this.categoryKeys = Object.keys(data);
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        if (err.status === 401) {
          this.errorMessage = 'Session expired. Please login again.';
        } else {
          this.errorMessage = 'Failed to load orders. Please try again.';
        }
      }
    });
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
}
