import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { environment } from '../environments/environment';

@Component({
  selector: 'app-login',
  standalone: true,
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  imports: [CommonModule, ReactiveFormsModule, RouterLink]
})
export class LoginComponent {
  loginForm: FormGroup;
  errorMessage: string = '';

  constructor(private fb: FormBuilder, private http: HttpClient, private router: Router) {
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      const loginData = this.loginForm.value;

      this.http.post<any>(`${environment.apiUrl}/api/users/login`, loginData)
        .subscribe({
          next: (response: any) => {
            console.log('Login successful:', response);
            localStorage.setItem('accessToken', response.accessToken);
            localStorage.setItem('refreshToken', response.refreshToken);

            // Store the role from backend (e.g. "ROLE_EMPLOYEE", "ROLE_CUSTOMER")
            localStorage.setItem('userRole', response.role);

            // Remove guestId once logged in — cart will use username instead
            localStorage.removeItem('guestId');

            this.router.navigate(['/products']);
          },
          error: (err) => {
            console.error('Login failed:', err);
            this.errorMessage = 'Invalid username or password.';
          }
        });
    }
  }
}