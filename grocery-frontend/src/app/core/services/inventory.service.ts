import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { BranchInventoryResponse, BranchInventoryRequest } from '../../models/inventory.model';

@Injectable({
  providedIn: 'root'
})
export class InventoryService {

  private baseUrl = `${environment.apiUrl}/api/inventory`;

  constructor(private http: HttpClient) {}

  /**
   * For EMPLOYEE / BRANCH_MANAGER — backend reads branchId from JWT, no param needed.
   * GET /api/inventory/my-branch
   */
  getMyBranchInventory(): Observable<BranchInventoryResponse[]> {
    return this.http.get<BranchInventoryResponse[]>(`${this.baseUrl}/my-branch`);
  }

  /**
   * For ADMIN — can query any branch by ID.
   * GET /api/inventory/branch/{branchId}
   */
  getAnyBranchInventory(branchId: number): Observable<BranchInventoryResponse[]> {
    return this.http.get<BranchInventoryResponse[]>(`${this.baseUrl}/branch/${branchId}`);
  }

  /**
   * Add or update stock entry for a branch+product combination.
   * POST /api/inventory/stock
   */
  updateStock(request: BranchInventoryRequest): Observable<BranchInventoryResponse> {
    return this.http.post<BranchInventoryResponse>(`${this.baseUrl}/stock`, request);
  }
}
