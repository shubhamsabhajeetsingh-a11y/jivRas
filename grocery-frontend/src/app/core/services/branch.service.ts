import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Branch } from '../../models/inventory.model';

@Injectable({
  providedIn: 'root'
})
export class BranchService {

  private baseUrl = `${environment.apiUrl}/api/branches`;

  constructor(private http: HttpClient) {}

  /**
   * Get all active branches — ADMIN only.
   * GET /api/branches/active
   */
  getAllActiveBranches(): Observable<Branch[]> {
    return this.http.get<Branch[]>(`${this.baseUrl}/active`);
  }
}
