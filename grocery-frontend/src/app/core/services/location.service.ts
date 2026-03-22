import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class LocationService {

  private baseUrl = `${environment.apiUrl}/api/locations`;

  constructor(private http: HttpClient) {}

  getStates(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/states`);
  }

  getCities(state: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/cities`, { params: { state } });
  }

  getPincodes(state: string, city: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/pincodes`, { params: { state, city } });
  }
}
