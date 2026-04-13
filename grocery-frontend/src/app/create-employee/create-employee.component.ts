import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { environment } from '../environments/environment';

@Component({
  selector: 'app-create-employee',
  templateUrl: './create-employee.component.html',
  styleUrls: ['./create-employee.component.css'],
  imports: [CommonModule, ReactiveFormsModule, RouterLink, FormsModule]
})
export class CreateEmployeeComponent implements OnInit {
  createEmployeeForm: FormGroup;
  successMessage: string = '';
  errorMessage: string = '';

  showCreateRoleModal: boolean = false;
  newRoleName: string = '';
  roleModules: string[] = [];
  roleActions: string[] = [];
  roleMatrix: { module: string, action: string, allowed: boolean }[] = [];
  roleCreating: boolean = false;
  roleCreateError: string = '';
  roleCreateSuccess: string = '';
  rolesList: string[] = ['EMPLOYEE', 'ADMIN'];

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

  ngOnInit(): void {
    this.http.get<string[]>(`${environment.apiUrl}/api/users/roles`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` }
    }).subscribe({
      next: (roles) => {
        this.rolesList = roles;
      },
      error: (err) => console.error('Failed to load roles', err)
    });
  }

  openCreateRoleModal(): void {
    this.showCreateRoleModal = true;
    this.newRoleName = '';
    this.roleCreateError = '';
    this.roleCreateSuccess = '';
    
    this.http.get<any[]>(`${environment.apiUrl}/api/role-permissions/modules`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` }
    }).subscribe({
      next: (data) => {
        this.roleModules = [...new Set(data.map(e => e.module))].sort() as string[];
        this.roleActions = ['VIEW','CREATE','EDIT','DELETE'].filter(a => data.some(e => e.action === a));
        
        this.roleMatrix = [];
        for (const mod of this.roleModules) {
          for (const act of this.roleActions) {
            this.roleMatrix.push({ module: mod, action: act, allowed: false });
          }
        }
      },
      error: (err) => {
        this.roleCreateError = 'Failed to load permissions registry';
        console.error(err);
      }
    });
  }

  getRoleCell(module: string, action: string) {
    return this.roleMatrix.find(p => p.module === module && p.action === action);
  }

  toggleRoleCell(module: string, action: string): void {
    const cell = this.getRoleCell(module, action);
    if (cell) cell.allowed = !cell.allowed;
  }

  submitCreateRole(): void {
    this.newRoleName = this.newRoleName.trim().toUpperCase();
    if (!this.newRoleName) return;

    const permissions = this.roleMatrix.filter(p => p.allowed);
    if (permissions.length === 0) {
      this.roleCreateError = 'Please select at least one permission.';
      return;
    }

    this.roleCreating = true;
    this.roleCreateError = '';

    const body = {
      roleName: this.newRoleName,
      permissions: permissions
    };

    this.http.post(`${environment.apiUrl}/api/role-permissions/roles`, body, {
      headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` }
    }).subscribe({
      next: (res) => {
        this.roleCreateSuccess = 'Role created!';
        if (!this.rolesList.includes(this.newRoleName)) {
          this.rolesList.push(this.newRoleName);
        }
        this.roleCreating = false;
        setTimeout(() => this.closeCreateRoleModal(), 1500);
      },
      error: (err) => {
        this.roleCreating = false;
        this.roleCreateError = typeof err.error === 'string' ? err.error : 'Failed to create role';
        console.error(err);
      }
    });
  }

  closeCreateRoleModal(): void {
    this.showCreateRoleModal = false;
  }
}
