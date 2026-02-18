import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { environment } from '../environments/environment';

@Component({
  selector: 'app-login',
  standalone: true,
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  imports: [CommonModule, ReactiveFormsModule]
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
        //const credentials = { username: this.username, password: this.password };
        const loginData = this.loginForm.value;

        this.http.post(`${environment.apiUrl}/api/products/login`, loginData, { responseType: 'text' })
          .subscribe({
            next: (response) => {
              console.log('Login successful:', response);
              // Navigate to products page on success
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