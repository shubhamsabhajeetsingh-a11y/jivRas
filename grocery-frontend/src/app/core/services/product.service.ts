import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private apiUrl = 'http://localhost:8080/api/products';
  // 2. Use the variable + your endpoint
  private baseUrl = `${environment.apiUrl}/api/products`; 

  constructor(private http: HttpClient) {}

  getAllProducts(): Observable<any> {
    return this.http.get(this.baseUrl);
  }

  getActiveProducts(): Observable<any> {
    return this.http.get(`${this.baseUrl}/active`);
  }

  addProduct(product: any): Observable<any> {
    return this.http.post(this.baseUrl, product);
  }

  updateProduct(id: number, product: any): Observable<any> {
    return this.http.put(`${this.baseUrl}/${id}`, product);
  }

// 🟢 NEW METHOD FOR IMAGE UPLOAD
  uploadProductImage(id: number, file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);

    // This calls: POST http://localhost:8080/api/products/7/image
    return this.http.post(`${this.apiUrl}/${id}/image`, formData, { responseType: 'text' });
  }
}