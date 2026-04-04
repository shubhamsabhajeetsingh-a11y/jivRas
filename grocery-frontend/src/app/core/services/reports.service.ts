import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ReportSummary {
  totalRevenueToday: number;
  totalOrdersToday: number;
  topSellingItem: string;
  topCategory: string;
  lowStockAlertsCount: number;
}

export interface SalesTrendPoint {
  date: string;        // "2026-04-04"
  revenue: number;
  orderCount: number;
}

export interface TopProductResponse {
  productName: string;
  totalQuantityKg: number;
  totalRevenue: number;
}

export interface CategoryBreakdownResponse {
  categoryName: string;
  totalRevenue: number;
  totalQuantityKg: number;
  orderCount: number;
}

export interface BranchStockComparison {
  branchId: number;
  branchName: string;
  totalProducts: number;
  lowStockCount: number;
  totalStockKg: number;
  lowStockPercentage: number;
}

export interface StockAlertItem {
  branchId: number;
  branchName: string;
  productId: number;
  productName: string;
  categoryName: string;
  availableStockKg: number;
  lowStockThreshold: number;
  severity: 'CRITICAL' | 'WARNING';
  projectedDaysLeft: number | null;
}

@Injectable({ providedIn: 'root' })
export class ReportsService {

  private base = `${environment.apiUrl}/api/reports`;

  constructor(private http: HttpClient) {}

  getSummary(): Observable<ReportSummary> {
    return this.http.get<ReportSummary>(`${this.base}/summary`);
  }

  getSalesTrend(from: string, to: string): Observable<SalesTrendPoint[]> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<SalesTrendPoint[]>(`${this.base}/sales-trend`, { params });
  }

  getTopProducts(from: string, to: string, limit = 10): Observable<TopProductResponse[]> {
    const params = new HttpParams().set('from', from).set('to', to).set('limit', limit);
    return this.http.get<TopProductResponse[]>(`${this.base}/top-products`, { params });
  }

  getCategoryBreakdown(from: string, to: string): Observable<CategoryBreakdownResponse[]> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<CategoryBreakdownResponse[]>(`${this.base}/category-breakdown`, { params });
  }

  getBranchComparison(): Observable<BranchStockComparison[]> {
    return this.http.get<BranchStockComparison[]>(`${this.base}/branch-comparison`);
  }

  getLowStock(): Observable<StockAlertItem[]> {
    return this.http.get<StockAlertItem[]>(`${this.base}/low-stock`);
  }

  getExcelExportUrl(from: string, to: string): string {
    return `${this.base}/export/excel?from=${from}&to=${to}`;
  }
}
