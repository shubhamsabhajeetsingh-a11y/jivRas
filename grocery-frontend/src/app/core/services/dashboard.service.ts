import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface LowStockAlertDTO {
  branchName: string;
  productName: string;
  currentStock: number;
  reorderThreshold: number;
}

export interface MorningSummaryDTO {
  todayOrders: number;
  todayRevenue: number;
  pendingOrders: number;
  todayNewCustomers: number;
  lowStockAlerts: LowStockAlertDTO[];
}

@Injectable({ providedIn: 'root' })
export class DashboardService {

  private readonly base = `${environment.apiUrl}/api/dashboard`;

  constructor(private http: HttpClient) {}

  getMorningSummary(): Observable<MorningSummaryDTO> {
    return this.http.get<MorningSummaryDTO>(`${this.base}/morning-summary`);
  }
}
