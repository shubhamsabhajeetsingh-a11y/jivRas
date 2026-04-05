import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { OrdersService, AdminOrderResponse } from '../core/services/orders.service';
import { SearchService } from '../core/services/search.service';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './orders.component.html',
  styleUrl: './orders.component.css'
})
export class OrdersComponent implements OnInit, OnDestroy {

  allGroupedOrders: { [category: string]: AdminOrderResponse[] } = {};
  groupedOrders:    { [category: string]: AdminOrderResponse[] } = {};
  categoryKeys: string[] = [];
  expandedCategories: Set<string> = new Set<string>();
  expandedOrders: Set<number> = new Set<number>();

  isLoading = false;
  errorMessage = '';

  // KPI cards — always reflect the filtered result
  totalOrders     = 0;
  pendingOrders   = 0;
  deliveredOrders = 0;
  cancelledOrders = 0;

  // Status/date filters
  filters = { status: '', from: '', to: '' };

  // Tracks the latest search query emitted by the service
  currentSearchQuery = '';

  // Status updating state
  statusError: { [orderId: number]: string } = {};
  availableStatuses = ['CONFIRMED', 'DISPATCHED', 'DELIVERED', 'CANCELLED'];

  private searchSub?: Subscription;

  constructor(
    private ordersService: OrdersService,
    private searchService: SearchService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Subscribe once; keep currentSearchQuery in sync and re-apply all filters
    this.searchSub = this.searchService.query$.subscribe(query => {
      this.currentSearchQuery = query;
      this.applyAllFilters(query);
    });

    this.isLoading = true;
    this.ordersService.getOrdersGroupedByCategory().subscribe({
      next: (data) => {
        this.allGroupedOrders = data;
        // Apply the current search + filter state to freshly loaded data
        this.applyAllFilters(this.currentSearchQuery);
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

  // ── Core filter engine ─────────────────────────────────────────────

  applyAllFilters(searchQuery: string = ''): void {
    const q = searchQuery.trim().toLowerCase();
    const filtered: { [category: string]: AdminOrderResponse[] } = {};

    for (const cat of Object.keys(this.allGroupedOrders)) {
      let orders = [...this.allGroupedOrders[cat]];

      // Search: customer name, phone, order ID
      if (q) {
        orders = orders.filter(o =>
          o.customerName?.toLowerCase().includes(q) ||
          o.mobile?.includes(q) ||
          o.orderId?.toString().includes(q)
        );
      }

      // Status filter
      if (this.filters.status) {
        orders = orders.filter(o => o.orderStatus === this.filters.status);
      }

      // Date from
      if (this.filters.from) {
        const from = new Date(this.filters.from);
        orders = orders.filter(o => new Date(o.orderDate) >= from);
      }

      // Date to (inclusive of the whole day)
      if (this.filters.to) {
        const to = new Date(this.filters.to);
        to.setHours(23, 59, 59, 999);
        orders = orders.filter(o => new Date(o.orderDate) <= to);
      }

      if (orders.length > 0) {
        filtered[cat] = orders;
      }
    }

    this.groupedOrders = filtered;
    this.categoryKeys   = Object.keys(filtered);
    this.calculateStats();
    this.cdr.detectChanges();
  }

  // Called by status / date filter controls in the template
  onFilterChange(): void {
    this.applyAllFilters(this.currentSearchQuery);
  }

  clearFilters(): void {
    this.filters = { status: '', from: '', to: '' };
    // clearQuery() emits '' → subscription calls applyAllFilters('') automatically
    this.searchService.clearQuery();
  }

  // ── KPI stats — always computed from filtered groupedOrders ───────

  calculateStats(): void {
    let total = 0, delivered = 0, cancelled = 0, pending = 0;
    const seen = new Set<number>();

    Object.values(this.groupedOrders).forEach(orders => {
      orders.forEach(order => {
        if (!seen.has(order.orderId)) {
          seen.add(order.orderId);
          total++;
          const st = order.orderStatus?.toUpperCase() || '';
          if (st === 'DELIVERED') delivered++;
          else if (st === 'CANCELLED') cancelled++;
          else pending++;
        }
      });
    });

    this.totalOrders     = total;
    this.deliveredOrders = delivered;
    this.cancelledOrders = cancelled;
    this.pendingOrders   = pending;
  }

  // ── UI helpers ─────────────────────────────────────────────────────

  toggleCategory(category: string): void {
    this.expandedCategories.has(category)
      ? this.expandedCategories.delete(category)
      : this.expandedCategories.add(category);
  }

  toggleOrderItems(orderId: number): void {
    this.expandedOrders.has(orderId)
      ? this.expandedOrders.delete(orderId)
      : this.expandedOrders.add(orderId);
  }

  updateOrderStatus(order: AdminOrderResponse, event: any): void {
    const newStatus = event.target.value;
    if (!newStatus || order.orderStatus === newStatus) return;

    this.statusError[order.orderId] = '';

    this.ordersService.updateOrderStatus(order.orderId, newStatus).subscribe({
      next: () => {
        order.orderStatus = newStatus;
        this.calculateStats(); // refresh KPIs after status change
        this.cdr.detectChanges();
      },
      error: () => {
        this.statusError[order.orderId] = 'Failed to update status. Please try again.';
        event.target.value = order.orderStatus;
        this.cdr.detectChanges();
      }
    });
  }

  get hasActiveFilters(): boolean {
    return !!(this.filters.status || this.filters.from || this.filters.to || this.currentSearchQuery);
  }

  getStatusClass(status: string): string {
    switch (status?.toUpperCase()) {
      case 'CREATED':          return 'badge-grey';
      case 'CONFIRMED':        return 'badge-blue';
      case 'DISPATCHED':       return 'badge-orange';
      case 'DELIVERED':        return 'badge-green';
      case 'CANCELLED':        return 'badge-red';
      default:                 return 'badge-grey';
    }
  }

  getBorderColor(status: string): string {
    switch (status?.toUpperCase()) {
      case 'CREATED':          return '#9e9e9e';
      case 'CONFIRMED':        return '#1976d2';
      case 'DISPATCHED':       return '#e65100';
      case 'DELIVERED':        return '#2e7d32';
      case 'CANCELLED':        return '#c62828';
      default:                 return '#e0e0e0';
    }
  }

  ngOnDestroy(): void {
    this.searchSub?.unsubscribe();
  }
}
