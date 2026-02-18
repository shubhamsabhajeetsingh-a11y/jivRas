import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-create-users',
  templateUrl: './create-users.component.html',
  styleUrls: ['./create-users.component.css'],
  imports: [CommonModule, ReactiveFormsModule]
})
export class CreateUsersComponent {
  createUserForm: FormGroup;
  successMessage: string = '';
  errorMessage: string = '';

  constructor(private fb: FormBuilder, private http: HttpClient) {
    this.createUserForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required],
      role: ['', Validators.required]
    });
  }
  
  onSubmit(): void {
      if (this.createUserForm.valid) {
        const userData = [this.createUserForm.value]; // Wrap as list

        // Added { responseType: 'text' } so Angular accepts the string response
        this.http.post('http://localhost:8080/api/users/create', userData, { responseType: 'text' })
          .subscribe({
            next: (response) => {
              console.log('Backend response:', response);
              this.successMessage = 'User created successfully!';
              this.errorMessage = '';
              this.createUserForm.reset();
            },
            error: (err) => {
              console.error('Error details:', err);
              this.errorMessage = 'Failed to create user. Check console for details.';
              this.successMessage = '';
            }
          });
      }
    }
}