import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { environment } from '../environments/environment';

@Component({
  selector: 'app-create-employee',
  templateUrl: './create-employee.component.html',
  styleUrls: ['./create-employee.component.css'],
  imports: [CommonModule, ReactiveFormsModule, RouterLink]
})
export class CreateEmployeeComponent {
  createEmployeeForm: FormGroup;
  successMessage: string = '';
  errorMessage: string = '';

  constructor(private fb: FormBuilder, private http: HttpClient, private router: Router) {
    this.createEmployeeForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      mobile: ['', [Validators.required, Validators.pattern('^[0-9]{10}$')]],
      address: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      username: ['', Validators.required],
      password: ['', [Validators.required, Validators.minLength(4)]],
      role: ['EMPLOYEE', Validators.required]
    });
  }

  onSubmit(): void {
    if (this.createEmployeeForm.valid) {
      const employeeData = this.createEmployeeForm.value;

      this.http.post(`${environment.apiUrl}/api/users/register-employee`, employeeData, { responseType: 'text' })
        .subscribe({
          next: (response) => {
            console.log('Employee registration response:', response);
            this.successMessage = response;
            this.errorMessage = '';
            this.createEmployeeForm.reset({ role: 'EMPLOYEE' });
          },
          error: (err) => {
            console.error('Employee registration error:', err);
            this.errorMessage = err.error || 'Failed to create employee. Please try again.';
            this.successMessage = '';
          }
        });
    }
  }
}
