import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { UserProfile } from '../../models/inventory.model';

// Kept for backward compat (used by user-profile component)
export interface UserProfileBasic {
  firstName: string;
  lastName: string;
  address: string;
  role: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private baseUrl = `${environment.apiUrl}/api/users`;

  constructor(private http: HttpClient) {}

  /** Used by UserProfile widget — calls /me (basic fields) */
  getProfile(): Observable<UserProfileBasic> {
    return this.http.get<UserProfileBasic>(`${this.baseUrl}/me`);
  }

  /** Used by inventory dashboard — calls /profile (includes username, branchId) */
  getUserProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.baseUrl}/profile`);
  }
}
