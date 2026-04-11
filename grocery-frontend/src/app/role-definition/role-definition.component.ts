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
  saveSuccess?: string;
  saveError?: string;
  pwSuccess?: string;
  pwError?: string;
}

export interface RolePermissionRow {
  id: number;
  role: string;
  module: string;
  action: string;
  allowed: boolean;
  toggling?: boolean;
}

export interface MatrixCell {
  module: string;
  action: string;
  allowed: boolean;
  toggling: boolean;
}

export interface ModuleActionEntry {
  httpMethod: string;
  uriPattern: string;
  module: string;
  action: string;
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

  allModules: string[] = [];
  allActions: string[] = [];
  private matrixCache: Record<string, MatrixCell[]> = {};
  expandedRole: string | null = null;
  currentMatrix: MatrixCell[] = [];
  matrixLoading: boolean = false;

  constructor(
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.loadEmployees();
      this.loadModuleRegistry();
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

  private loadModuleRegistry(): void {
    this.http.get<ModuleActionEntry[]>(
      `${environment.apiUrl}/api/role-permissions/modules`,
      { headers: this.headers() }
    ).subscribe({
      next: (data) => {
        this.allModules = [...new Set(data.map(e => e.module))].sort();
        this.allActions = ['VIEW', 'CREATE', 'EDIT', 'DELETE'].filter(a => data.some(e => e.action === a));
        this.cdr.detectChanges();
      },
      error: () => {}
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

  // ── Permissions matrix (per role) ────────────────────────────────────────

  toggleRoleMatrix(role: string): void {
    if (this.expandedRole === role) {
      this.expandedRole = null;
      this.currentMatrix = [];
      return;
    }
    this.expandedRole = role;

    if (this.matrixCache[role]) {
      this.currentMatrix = this.deepCopyMatrix(this.matrixCache[role]);
      this.cdr.detectChanges();
      return;
    }

    this.matrixLoading = true;
    this.cdr.detectChanges();

    this.http.get<RolePermissionRow[]>(
      `${environment.apiUrl}/api/role-permissions/matrix?role=${role}`,
      { headers: this.headers() }
    ).subscribe({
      next: (rows) => {
        const cells: MatrixCell[] = [];
        for (const module of this.allModules) {
          for (const action of this.allActions) {
            const match = rows.find(r => r.module === module && r.action === action);
            cells.push({ module, action, allowed: match ? match.allowed === true : false, toggling: false });
          }
        }
        this.matrixCache[role] = cells;
        this.currentMatrix = this.deepCopyMatrix(cells);
        this.matrixLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.matrixLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  toggleMatrixCell(cell: MatrixCell): void {
    if (cell.toggling || !this.expandedRole) return;
    cell.toggling = true;
    this.cdr.detectChanges();

    this.http.put(
      `${environment.apiUrl}/api/role-permissions/matrix`,
      { role: this.expandedRole, module: cell.module, action: cell.action, allowed: !cell.allowed },
      { headers: this.headers() }
    ).subscribe({
      next: () => {
        cell.allowed = !cell.allowed;
        cell.toggling = false;
        if (this.matrixCache[this.expandedRole!]) {
          const cached = this.matrixCache[this.expandedRole!].find(
            c => c.module === cell.module && c.action === cell.action
          );
          if (cached) cached.allowed = cell.allowed;
        }
        this.cdr.detectChanges();
      },
      error: () => {
        cell.toggling = false;
        this.cdr.detectChanges();
      }
    });
  }

  deepCopyMatrix(cells: MatrixCell[]): MatrixCell[] {
    return cells.map(c => ({ ...c, toggling: false }));
  }

  getCell(module: string, action: string): MatrixCell | undefined {
    return this.currentMatrix.find(c => c.module === module && c.action === action);
  }

  @Output() tabChange = new EventEmitter<string>();

  // ── Navigation ────────────────────────────────────────────────────────────

  goToCreateRole(): void {
    this.tabChange.emit('create-role');
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  getRoleBadgeClass(role: string): string {
    switch (role) {
      case 'EMPLOYEE':       return 'badge-employee';
      case 'BRANCH_MANAGER': return 'badge-manager';
      default:               return 'badge-default';
    }
  }
}
