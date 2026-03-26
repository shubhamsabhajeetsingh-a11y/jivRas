import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { environment } from '../environments/environment';

@Component({
  selector: 'app-login',
  standalone: true,
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  imports: [CommonModule, ReactiveFormsModule, RouterLink]
})
export class LoginComponent implements OnInit {
  loginForm: FormGroup;
  errorMessage: string = '';

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    // If user navigated back to login page, clear their session
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('userRole');
    }
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

            if (response.role === 'ROLE_EMPLOYEE' || response.role === 'ROLE_ADMIN') {
              this.router.navigate(['/inventory-dashboard']);
            } else {
              this.router.navigate(['/products']);
            }
          },
          error: (err) => {
            console.error('Login failed:', err);
            this.errorMessage = 'Invalid username or password.';
          }
        });
    }
  }
}