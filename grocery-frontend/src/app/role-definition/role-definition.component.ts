import { Component, OnInit, ChangeDetectorRef, Inject, PLATFORM_ID, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../environments/environment';

export interface EmployeeDetail {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  mobile: string;
  address: string;
  username: string;
  role: string;
  branchId: number | null;
  branchName: string;
  // UI state
  showPasswordReset?: boolean;
  newPassword?: string;
  permissionsExpanded?: boolean;
  permissions?: RolePermissionRow[];
  permissionsLoading?: boolean;
  saveSuccess?: string;
  saveError?: string;
  pwSuccess?: string;
  pwError?: string;
}

export interface RolePermissionRow {
  id: number;
  role: string;
  endpoint: string;
  httpMethod: string;
  allowed: boolean;
  toggling?: boolean;
}

@Component({
  selector: 'app-role-definition',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './role-definition.component.html',
  styleUrl: './role-definition.component.css'
})
export class RoleDefinitionComponent implements OnInit {

  employees: EmployeeDetail[] = [];
  filteredEmployees: EmployeeDetail[] = [];
  searchTerm = '';
  activeRoleFilter = 'All';
  availableRoles: string[] = ['All'];

  loading = false;
  globalError = '';

  // Cache: role → permissions (shared for all employees of same role)
  private permCache: Record<string, RolePermissionRow[]> = {};

  constructor(
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.loadEmployees();
    }
  }

  private headers(): HttpHeaders {
    const token = typeof localStorage !== 'undefined' ? localStorage.getItem('accessToken') : '';
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  // ── Data loading ─────────────────────────────────────────────────────────

  loadEmployees(): void {
    this.loading = true;
    this.globalError = '';
    this.http.get<EmployeeDetail[]>(`${environment.apiUrl}/api/users/employees`, { headers: this.headers() })
      .subscribe({
        next: (data) => {
          this.employees = data.map(e => ({
            ...e,
            showPasswordReset: false,
            newPassword: '',
            permissionsExpanded: false,
            permissions: [],
            permissionsLoading: false,
            saveSuccess: '', saveError: '', pwSuccess: '', pwError: ''
          }));
          // Build role filter tabs
          const roles = [...new Set(data.map(e => e.role))].filter(Boolean);
          this.availableRoles = ['All', ...roles];
          this.applyFilters();
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.globalError = err?.error || 'Failed to load employees.';
          this.loading = false;
          this.cdr.detectChanges();
        }
      });
  }

  // ── Filters ──────────────────────────────────────────────────────────────

  applyFilters(): void {
    let result = this.employees;
    if (this.activeRoleFilter !== 'All') {
      result = result.filter(e => e.role === this.activeRoleFilter);
    }
    const q = this.searchTerm.toLowerCase().trim();
    if (q) {
      result = result.filter(e =>
        `${e.firstName} ${e.lastName}`.toLowerCase().includes(q) ||
        e.username.toLowerCase().includes(q) ||
        e.email.toLowerCase().includes(q) ||
        e.role.toLowerCase().includes(q)
      );
    }
    this.filteredEmployees = result;
    this.cdr.detectChanges();
  }

  setRoleFilter(role: string): void {
    this.activeRoleFilter = role;
    this.applyFilters();
  }

  // ── Save employee details ─────────────────────────────────────────────────

  saveEmployee(emp: EmployeeDetail): void {
    emp.saveSuccess = '';
    emp.saveError = '';

    this.http.put<EmployeeDetail>(
      `${environment.apiUrl}/api/users/employees/${emp.id}`,
      { mobile: emp.mobile, address: emp.address },
      { headers: this.headers() }
    ).subscribe({
      next: (updated) => {
        emp.mobile = updated.mobile;
        emp.address = updated.address;
        emp.saveSuccess = 'Saved successfully!';
        this.cdr.detectChanges();
        setTimeout(() => { emp.saveSuccess = ''; this.cdr.detectChanges(); }, 3000);
      },
      error: (err) => {
        emp.saveError = err?.error || 'Save failed.';
        this.cdr.detectChanges();
        setTimeout(() => { emp.saveError = ''; this.cdr.detectChanges(); }, 4000);
      }
    });
  }

  // ── Password reset ────────────────────────────────────────────────────────

  togglePasswordReset(emp: EmployeeDetail): void {
    emp.showPasswordReset = !emp.showPasswordReset;
    emp.newPassword = '';
    emp.pwSuccess = '';
    emp.pwError = '';
    this.cdr.detectChanges();
  }

  confirmPasswordReset(emp: EmployeeDetail): void {
    emp.pwSuccess = '';
    emp.pwError = '';

    if (!emp.newPassword || emp.newPassword.trim().length < 6) {
      emp.pwError = 'Password must be at least 6 characters.';
      this.cdr.detectChanges();
      return;
    }

    this.http.put<string>(
      `${environment.apiUrl}/api/users/employees/${emp.id}/reset-password`,
      { newPassword: emp.newPassword },
      { headers: this.headers(), responseType: 'text' as 'json' }
    ).subscribe({
      next: () => {
        emp.pwSuccess = 'Password reset!';
        emp.newPassword = '';
        emp.showPasswordReset = false;
        this.cdr.detectChanges();
        setTimeout(() => { emp.pwSuccess = ''; this.cdr.detectChanges(); }, 3000);
      },
      error: (err) => {
        emp.pwError = err?.error || 'Reset failed.';
        this.cdr.detectChanges();
      }
    });
  }

  // ── Permissions (lazy per role) ───────────────────────────────────────────

  togglePermissions(emp: EmployeeDetail): void {
    emp.permissionsExpanded = !emp.permissionsExpanded;

    if (emp.permissionsExpanded && emp.permissions!.length === 0) {
      // Check cache first
      if (this.permCache[emp.role]) {
        emp.permissions = this.deepCopyPerms(this.permCache[emp.role]);
        this.cdr.detectChanges();
        return;
      }
      emp.permissionsLoading = true;
      this.cdr.detectChanges();

      this.http.get<RolePermissionRow[]>(
        `${environment.apiUrl}/api/permissions/role/${emp.role}`,
        { headers: this.headers() }
      ).subscribe({
        next: (perms) => {
          this.permCache[emp.role] = perms;
          // Share same data among all cards of same role
          this.employees
            .filter(e => e.role === emp.role && e.permissions!.length === 0)
            .forEach(e => e.permissions = this.deepCopyPerms(perms));
          emp.permissionsLoading = false;
          this.cdr.detectChanges();
        },
        error: () => {
          emp.permissionsLoading = false;
          this.cdr.detectChanges();
        }
      });
    }
    this.cdr.detectChanges();
  }

  togglePermissionRow(emp: EmployeeDetail, perm: RolePermissionRow): void {
    if (perm.toggling) return;
    perm.toggling = true;
    this.cdr.detectChanges();

    this.http.put<RolePermissionRow>(
      `${environment.apiUrl}/api/permissions/${perm.id}/toggle`,
      {},
      { headers: this.headers() }
    ).subscribe({
      next: (updated) => {
        perm.allowed = updated.allowed;
        perm.toggling = false;
        // Update cache + all same-role employees
        if (this.permCache[emp.role]) {
          const cached = this.permCache[emp.role].find(p => p.id === perm.id);
          if (cached) cached.allowed = updated.allowed;
        }
        this.employees
          .filter(e => e.role === emp.role)
          .forEach(e => {
            const p = e.permissions?.find(x => x.id === perm.id);
            if (p) p.allowed = updated.allowed;
          });
        this.cdr.detectChanges();
      },
      error: () => {
        perm.toggling = false;
        this.cdr.detectChanges();
      }
    });
  }

  @Output() tabChange = new EventEmitter<string>();

  // ── Navigation ────────────────────────────────────────────────────────────

  goToCreateRole(): void {
    // Emit event to parent instead of using router
    // This works reliably every single time regardless of current URL
    this.tabChange.emit('create-role');
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private deepCopyPerms(perms: RolePermissionRow[]): RolePermissionRow[] {
    return perms.map(p => ({ ...p, toggling: false }));
  }

  getRoleBadgeClass(role: string): string {
    switch (role) {
      case 'EMPLOYEE':       return 'badge-employee';
      case 'BRANCH_MANAGER': return 'badge-manager';
      default:               return 'badge-default';
    }
  }
}
