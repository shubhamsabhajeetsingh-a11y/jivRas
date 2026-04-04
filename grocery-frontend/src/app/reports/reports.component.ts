import {
  Component,
  OnInit,
  OnDestroy,
  AfterViewInit,
  ViewChild,
  ElementRef,
  ChangeDetectorRef,
  Inject,
  PLATFORM_ID,
} from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Chart, registerables } from 'chart.js';

import {
  ReportsService,
  ReportSummary,
  SalesTrendPoint,
  TopProductResponse,
  CategoryBreakdownResponse,
  BranchStockComparison,
  StockAlertItem,
} from '../core/services/reports.service';
import { environment } from '../environments/environment';

Chart.register(...registerables);

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reports.component.html',
  styleUrl: './reports.component.css',
})
export class ReportsComponent implements OnInit, AfterViewInit, OnDestroy {

  // ── Canvas refs for Chart.js ──────────────────────────────────────
  @ViewChild('salesTrendCanvas')    salesTrendCanvas!:    ElementRef<HTMLCanvasElement>;
  @ViewChild('topProductsCanvas')   topProductsCanvas!:   ElementRef<HTMLCanvasElement>;
  @ViewChild('categoryPieCanvas')   categoryPieCanvas!:   ElementRef<HTMLCanvasElement>;
  @ViewChild('branchCompareCanvas') branchCompareCanvas!: ElementRef<HTMLCanvasElement>;

  // ── Chart instances (kept for destroy/update) ─────────────────────
  private trendChart?:   Chart;
  private topChart?:     Chart;
  private pieChart?:     Chart;
  private branchChart?:  Chart;

  // ── Data ──────────────────────────────────────────────────────────
  summary?: ReportSummary;
  stockAlerts: StockAlertItem[] = [];
  branchComparisons: BranchStockComparison[] = [];

  // ── Filters ───────────────────────────────────────────────────────
  fromDate: string = '';
  toDate:   string = '';

  // ── UI state ──────────────────────────────────────────────────────
  loading = false;
  error   = '';
  chartsReady = false;
  exportingExcel = false;

  // ── Alert table filter ────────────────────────────────────────────
  alertFilter: 'ALL' | 'CRITICAL' | 'WARNING' = 'ALL';
  alertSearchTerm = '';

  constructor(
    private reportsService: ReportsService,
    private cdr: ChangeDetectorRef,
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object,
  ) {}

  ngOnInit(): void {
    // Default: last 30 days
    const today = new Date();
    this.toDate   = this.formatDate(today);
    const past    = new Date(today);
    past.setDate(past.getDate() - 29);
    this.fromDate = this.formatDate(past);
  }

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.chartsReady = true;
      this.loadAll();
    }
  }

  ngOnDestroy(): void {
    this.destroyCharts();
  }

  // ── Load all data ─────────────────────────────────────────────────

  loadAll(): void {
    this.error   = '';
    this.loading = true;
    this.cdr.detectChanges();

    let pending = 5;
    const done = () => { if (--pending === 0) { this.loading = false; this.cdr.detectChanges(); } };
    const fail = (msg: string) => { this.error = msg; this.loading = false; this.cdr.detectChanges(); };

    // KPI Summary
    this.reportsService.getSummary().subscribe({
      next: (s) => { this.summary = s; done(); },
      error: () => fail('Failed to load summary.'),
    });

    // Sales Trend → line chart
    this.reportsService.getSalesTrend(this.fromDate, this.toDate).subscribe({
      next: (data) => { this.buildTrendChart(data); done(); },
      error: () => fail('Failed to load sales trend.'),
    });

    // Top Products → bar chart
    this.reportsService.getTopProducts(this.fromDate, this.toDate).subscribe({
      next: (data) => { this.buildTopProductsChart(data); done(); },
      error: () => fail('Failed to load top products.'),
    });

    // Category Breakdown → pie chart
    this.reportsService.getCategoryBreakdown(this.fromDate, this.toDate).subscribe({
      next: (data) => { this.buildCategoryPieChart(data); done(); },
      error: () => fail('Failed to load category breakdown.'),
    });

    // Branch Comparison + Stock Alerts (combined call)
    this.reportsService.getBranchComparison().subscribe({
      next: (data) => {
        this.branchComparisons = data;
        this.buildBranchComparisonChart(data);
      },
      error: () => {},
    });
    this.reportsService.getLowStock().subscribe({
      next: (data) => { this.stockAlerts = data; done(); },
      error: () => fail('Failed to load stock alerts.'),
    });
  }

  onFilterApply(): void {
    if (!this.fromDate || !this.toDate) return;
    this.destroyCharts();
    this.loadAll();
  }

  setQuickRange(days: number): void {
    const today = new Date();
    this.toDate   = this.formatDate(today);
    const past    = new Date(today);
    past.setDate(past.getDate() - (days - 1));
    this.fromDate = this.formatDate(past);
    this.destroyCharts();
    this.loadAll();
  }

  // ── Chart builders ────────────────────────────────────────────────

  private buildTrendChart(data: SalesTrendPoint[]): void {
    if (!this.chartsReady || !this.salesTrendCanvas) return;
    const ctx = this.salesTrendCanvas.nativeElement.getContext('2d')!;
    this.trendChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: data.map(d => d.date),
        datasets: [{
          label: 'Revenue (₹)',
          data: data.map(d => d.revenue),
          borderColor: '#d57937',
          backgroundColor: 'rgba(213,121,55,0.12)',
          tension: 0.4,
          fill: true,
          pointRadius: 3,
          pointHoverRadius: 6,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: { grid: { color: 'rgba(0,0,0,0.04)' }, ticks: { maxTicksLimit: 10 } },
          y: {
            grid: { color: 'rgba(0,0,0,0.04)' },
            ticks: { callback: (v) => '₹' + Number(v).toLocaleString('en-IN') },
          },
        },
      },
    });
    this.cdr.detectChanges();
  }

  private buildTopProductsChart(data: TopProductResponse[]): void {
    if (!this.chartsReady || !this.topProductsCanvas) return;
    const ctx = this.topProductsCanvas.nativeElement.getContext('2d')!;
    const palette = ['#d57937','#E07A5F','#D4820A','#81B29A','#EFC94C',
                     '#6B8CAE','#C0392B','#8FC49E','#D4A574','#7A7A7A'];
    this.topChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: data.map(d => d.productName),
        datasets: [{
          label: 'Qty Sold (kg)',
          data: data.map(d => d.totalQuantityKg),
          backgroundColor: data.map((_, i) => palette[i % palette.length]),
          borderRadius: 5,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: { grid: { display: false } },
          y: { grid: { color: 'rgba(0,0,0,0.04)' } },
        },
      },
    });
    this.cdr.detectChanges();
  }

  private buildCategoryPieChart(data: CategoryBreakdownResponse[]): void {
    if (!this.chartsReady || !this.categoryPieCanvas) return;
    const ctx = this.categoryPieCanvas.nativeElement.getContext('2d')!;
    const palette = ['#d57937','#81B29A','#EFC94C','#E07A5F','#6B8CAE','#D4A574'];
    this.pieChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: data.map(d => d.categoryName),
        datasets: [{
          data: data.map(d => d.totalRevenue),
          backgroundColor: palette.slice(0, data.length),
          hoverOffset: 8,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'right', labels: { boxWidth: 12, padding: 14 } },
          tooltip: {
            callbacks: {
              label: (ctx) => ` ₹${Number(ctx.parsed).toLocaleString('en-IN')}`,
            },
          },
        },
      },
    });
    this.cdr.detectChanges();
  }

  private buildBranchComparisonChart(data: BranchStockComparison[]): void {
    if (!this.chartsReady || !this.branchCompareCanvas) return;
    const ctx = this.branchCompareCanvas.nativeElement.getContext('2d')!;
    this.branchChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: data.map(d => d.branchName),
        datasets: [
          {
            label: 'Total Stock (kg)',
            data: data.map(d => d.totalStockKg),
            backgroundColor: 'rgba(129,178,154,0.8)',
            borderRadius: 5,
          },
          {
            label: 'Low Stock Items',
            data: data.map(d => d.lowStockCount),
            backgroundColor: 'rgba(213,121,55,0.85)',
            borderRadius: 5,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { position: 'top', labels: { boxWidth: 12 } } },
        scales: {
          x: { grid: { display: false } },
          y: { grid: { color: 'rgba(0,0,0,0.04)' } },
        },
      },
    });
    this.cdr.detectChanges();
  }

  // ── Helpers ───────────────────────────────────────────────────────

  get filteredAlerts(): StockAlertItem[] {
    return this.stockAlerts.filter(a => {
      const matchesSeverity = this.alertFilter === 'ALL' || a.severity === this.alertFilter;
      const matchesSearch   = !this.alertSearchTerm ||
        a.productName.toLowerCase().includes(this.alertSearchTerm.toLowerCase()) ||
        a.branchName.toLowerCase().includes(this.alertSearchTerm.toLowerCase());
      return matchesSeverity && matchesSearch;
    });
  }

  downloadExcel(): void {
    const token = typeof localStorage !== 'undefined'
        ? localStorage.getItem('accessToken') : '';
    const url = this.reportsService.getExcelExportUrl(this.fromDate, this.toDate);

    this.exportingExcel = true;
    this.cdr.detectChanges();

    this.http.get(url, {
      headers: new HttpHeaders({ Authorization: `Bearer ${token}` }),
      responseType: 'blob',
    }).subscribe({
      next: (blob) => {
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `JivRas_Report_${this.fromDate}_to_${this.toDate}.xlsx`;
        link.click();
        URL.revokeObjectURL(link.href);
        this.exportingExcel = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.exportingExcel = false;
        this.error = 'Excel export failed.';
        this.cdr.detectChanges();
      },
    });
  }

  private destroyCharts(): void {
    this.trendChart?.destroy();
    this.topChart?.destroy();
    this.pieChart?.destroy();
    this.branchChart?.destroy();
    this.trendChart = this.topChart = this.pieChart = this.branchChart = undefined;
  }

  private formatDate(d: Date): string {
    return d.toISOString().slice(0, 10);
  }

  formatCurrency(v: number): string {
    return '₹' + v.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
}
