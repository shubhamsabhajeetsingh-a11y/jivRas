import { Component, OnInit, ViewChild, ElementRef, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Chart, registerables } from 'chart.js';
import { PaymentService } from '../../core/services/payment.service';
import { PaymentListItem, PaymentAnalytics, PaymentStatus } from '../../models/payment.model';

Chart.register(...registerables);

@Component({
  selector: 'app-payments-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './payments-dashboard.component.html',
  styleUrls: ['./payments-dashboard.component.css']
})
export class PaymentsDashboardComponent implements OnInit {
  @ViewChild('chartHost') chartHost!: ElementRef<HTMLCanvasElement>;

  payments: PaymentListItem[] = [];
  filteredPayments: PaymentListItem[] = [];
  analytics: PaymentAnalytics | null = null;
  permissionError: boolean = false;

  // Filters
  searchQuery = '';
  activeStatusFilter: PaymentStatus | null = null;
  fromDate: string = '';   // yyyy-mm-dd
  toDate: string = '';     // yyyy-mm-dd
  dateRangeDays = 30;

  statusOptions = [
    { value: 'PAID' as PaymentStatus, label: '💳 Paid' },
    { value: 'FAILED' as PaymentStatus, label: '❌ Failed' },
    { value: 'CREATED' as PaymentStatus, label: '⏳ Pending' },
    { value: 'REFUNDED' as PaymentStatus, label: '↩️ Refunded' }
  ];

  private chart?: Chart;

  constructor(
    private paymentService: PaymentService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    // Default to last 30 days
    this.setQuickRange(30);
  }

  setQuickRange(days: number) {
    this.dateRangeDays = days;
    const to = new Date();
    const from = new Date();
    from.setDate(from.getDate() - days);
    this.fromDate = from.toISOString().split('T')[0];
    this.toDate = to.toISOString().split('T')[0];
    this.reloadAll();
  }

  onDateRangeChange() {
    if (!this.fromDate || !this.toDate) return;
    const from = new Date(this.fromDate);
    const to = new Date(this.toDate);
    this.dateRangeDays = Math.round((to.getTime() - from.getTime()) / (1000 * 60 * 60 * 24));
    this.reloadAll();
  }

  private reloadAll() {
    this.permissionError = false;
    this.loadAnalytics();
    this.loadPayments();
  }

  private loadAnalytics() {
    this.paymentService.getAnalytics(this.fromDate, this.toDate).subscribe({
      next: (data) => {
        this.analytics = data;
        this.cdr.detectChanges();
      },
      error: (err) => {
        if (err.status === 403) {
          this.permissionError = true;
        }
        console.error('Failed to load payment analytics', err);
        this.cdr.detectChanges();
      }
    });
  }

  private loadPayments() {
    const fromDt = this.fromDate + 'T00:00:00';
    const toDt = this.toDate + 'T23:59:59';
    this.paymentService.getPaymentsList(null, fromDt, toDt).subscribe({
      next: (data) => {
        this.payments = data || [];
        this.applyFilters();
        // Render chart after getting the full list of payments
        setTimeout(() => this.renderChart(), 0);
      },
      error: (err) => {
        if (err.status === 403) {
          this.permissionError = true;
        }
        console.error('Failed to load payments', err);
        this.cdr.detectChanges();
      }
    });
  }

  applyFilters() {
    const q = this.searchQuery.trim().toLowerCase();
    this.filteredPayments = this.payments.filter(p => {
      // Status filter
      if (this.activeStatusFilter && p.status !== this.activeStatusFilter) return false;
      // Search filter — matches orderId, razorpay IDs, or customer phone
      if (q) {
        const haystack = [
          String(p.orderId),
          p.razorpayOrderId || '',
          p.razorpayPaymentId || '',
          p.customerPhone || ''
        ].join(' ').toLowerCase();
        if (!haystack.includes(q)) return false;
      }
      return true;
    });
    this.cdr.detectChanges();
  }

  toggleStatusFilter(status: PaymentStatus) {
    this.activeStatusFilter = this.activeStatusFilter === status ? null : status;
    this.applyFilters();
  }

  toggleTimeline(payment: PaymentListItem) {
    // Toggle expansion; lazy-load timeline data on first open
    payment.timelineExpanded = !payment.timelineExpanded;

    // Load only once; cache thereafter so repeated toggles don't re-fetch
    if (payment.timelineExpanded && !payment.timeline && !payment.timelineLoading) {
      payment.timelineLoading = true;
      payment.timelineError = undefined;
      // TODO: share timeline cache across rows that belong to the same orderId —
      // currently each row fetches independently on first expand.
      this.paymentService.getTimelineForOrder(payment.orderId).subscribe({
        next: (timeline) => {
          payment.timeline = timeline;
          payment.timelineLoading = false;
          this.cdr.detectChanges();
        },
        error: () => {
          payment.timelineError = 'Could not load payment timeline.';
          payment.timelineLoading = false;
          this.cdr.detectChanges();
        }
      });
    }
  }

  private renderChart() {
    if (!this.chartHost || !this.payments.length) return;
    const ctx = this.chartHost.nativeElement.getContext('2d');
    if (!ctx) return;

    if (this.chart) {
      this.chart.destroy();
    }

    // Group this.payments by day + status
    const dailyData: Record<string, { paid: number, failed: number, pending: number }> = {};
    
    // Sort payments by date to get chronological days
    const sorted = [...this.payments].sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
    
    sorted.forEach(p => {
      const day = p.createdAt.split('T')[0];
      if (!dailyData[day]) {
        dailyData[day] = { paid: 0, failed: 0, pending: 0 };
      }
      if (p.status === 'PAID') dailyData[day].paid++;
      else if (p.status === 'FAILED') dailyData[day].failed++;
      else if (p.status === 'CREATED') dailyData[day].pending++;
    });

    const labels = Object.keys(dailyData);
    const paidData = labels.map(l => dailyData[l].paid);
    const failedData = labels.map(l => dailyData[l].failed);
    const pendingData = labels.map(l => dailyData[l].pending);

    this.chart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels,
        datasets: [
          {
            label: 'Paid',
            data: paidData,
            backgroundColor: '#166534',
          },
          {
            label: 'Failed',
            data: failedData,
            backgroundColor: '#991b1b',
          },
          {
            label: 'Pending',
            data: pendingData,
            backgroundColor: '#d97706',
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: { stacked: true },
          y: { stacked: true }
        }
      }
    });
  }
}
