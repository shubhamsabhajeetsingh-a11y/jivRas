import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { OrdersService, AdminOrderResponse } from '../core/services/orders.service';
import { PaymentService } from '../core/services/payment.service';
import { PaymentStatus } from '../models/payment.model';
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
  activePaymentFilter: PaymentStatus | null = null;
  paymentFailedCount: number = 0;

  // Tracks the latest search query emitted by the service
  currentSearchQuery = '';

  // Status updating state
  statusError: { [orderId: number]: string } = {};
  availableStatuses = ['CONFIRMED', 'DISPATCHED', 'DELIVERED', 'CANCELLED'];

  // Timeline state
  timelineData: { [orderId: number]: any[] } = {};
  loadingTimeline: Set<number> = new Set();
  readonly timelineSteps = ['CREATED', 'CONFIRMED', 'DISPATCHED', 'DELIVERED'];

  // Invoice download state
  invoiceLoading: Set<number> = new Set();

  // Guest/registered filter for admin view — null = all, true = guests only, false = registered only
  guestFilter: boolean | null = null;

  private searchSub?: Subscription;

  constructor(
    private ordersService: OrdersService,
    private searchService: SearchService,
    private paymentService: PaymentService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Subscribe once; keep currentSearchQuery in sync and re-apply all filters
    this.searchSub = this.searchService.query$.subscribe(query => {
      this.currentSearchQuery = query;
      this.applyAllFilters(query);
    });

    this.loadOrdersFromBackend();
  }

  /** Fetches orders from backend with current guestFilter applied. */
  private loadOrdersFromBackend(): void {
    this.isLoading = true;
    this.ordersService.getOrdersGroupedByCategory(this.guestFilter).subscribe({
      next: (data) => {
        this.allGroupedOrders = data;
        this.loadPaymentStatusForAll();
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

  setGuestFilter(value: boolean | null): void {
    this.guestFilter = value;
    // Refetch from backend with the new filter
    this.loadOrdersFromBackend();
  }

  private loadPaymentStatusForAll(): void {
    this.paymentService.getAllPayments().subscribe(payments => {
      const statusByOrderId = new Map<number, PaymentStatus>();
      for (const p of payments) {
        const existing = statusByOrderId.get(p.orderId);
        if (!existing) {
          statusByOrderId.set(p.orderId, p.status);
        }
      }

      for (const cat of Object.keys(this.allGroupedOrders)) {
        for (const order of this.allGroupedOrders[cat]) {
          order.latestPaymentStatus = statusByOrderId.get(order.orderId) || null;
        }
      }

      this.paymentFailedCount = new Set(
        payments.filter(p => p.status === 'FAILED').map(p => p.orderId)
      ).size;
      
      this.applyAllFilters(this.currentSearchQuery);
      this.cdr.detectChanges();
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

      // Payment filter
      if (this.activePaymentFilter) {
        orders = orders.filter(o => o.latestPaymentStatus === this.activePaymentFilter);
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
    this.activePaymentFilter = null;
    // clearQuery() emits '' → subscription calls applyAllFilters('') automatically
    this.searchService.clearQuery();
  }

  togglePaymentFilter(status: PaymentStatus): void {
    this.activePaymentFilter = this.activePaymentFilter === status ? null : status;
    this.applyAllFilters(this.currentSearchQuery);
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

  togglePaymentDetails(order: AdminOrderResponse): void {
    order.paymentDetailsExpanded = !order.paymentDetailsExpanded;
    if (order.paymentDetailsExpanded && !order.paymentTimeline) {
      this.paymentService.getTimelineForOrder(order.orderId).subscribe({
        next: (timeline) => {
          order.paymentTimeline = timeline;
          this.cdr.detectChanges();
        },
        error: () => {
          order.paymentTimeline = {
            orderId: order.orderId,
            totalAmount: 0,
            effectiveStatus: 'NOT_APPLICABLE',
            attempts: []
          };
          this.cdr.detectChanges();
        }
      });
    }
  }

  toggleOrderItems(orderId: number): void {
    if (this.expandedOrders.has(orderId)) {
      this.expandedOrders.delete(orderId);
    } else {
      this.expandedOrders.add(orderId);
      this.loadingTimeline.add(orderId);
      this.loadTimeline(orderId);
    }
  }

  loadTimeline(orderId: number): void {
    this.ordersService.getOrderTimeline(orderId).subscribe({
      next: (data) => {
        this.timelineData[orderId] = data;
        this.loadingTimeline.delete(orderId);
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingTimeline.delete(orderId);
        this.cdr.detectChanges();
      }
    });
  }

  getTimelineEntry(orderId: number, status: string): any {
    return this.timelineData[orderId]?.find(e => e.status === status) ?? null;
  }

  downloadInvoice(orderId: number): void {
    this.invoiceLoading.add(orderId);
    this.cdr.detectChanges();

    this.ordersService.downloadInvoice(orderId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `JivRas_Invoice_#${orderId}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        this.invoiceLoading.delete(orderId);
        this.cdr.detectChanges();
      },
      error: () => {
        this.statusError[orderId] = 'Failed to download invoice. Please try again.';
        this.invoiceLoading.delete(orderId);
        this.cdr.detectChanges();
      }
    });
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
