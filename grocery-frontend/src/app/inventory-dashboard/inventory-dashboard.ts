import { Component, OnInit, ChangeDetectorRef, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { UserProfile } from '../user-profile/user-profile';
import { OrdersComponent } from '../orders/orders.component';
import { UserService } from '../core/services/user.service';
import { InventoryService } from '../core/services/inventory.service';
import { BranchService } from '../core/services/branch.service';
import {
  UserProfile as UserProfileModel,
  Branch,
  BranchInventoryResponse,
  BranchInventoryRequest
} from '../models/inventory.model';

export interface CategoryMeta {
  name: string;
  dot: string;
  emoji: string;
  countKey: string;
}

@Component({
  selector: 'app-inventory-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, UserProfile, OrdersComponent],
  templateUrl: './inventory-dashboard.html',
  styleUrl: './inventory-dashboard.css',
})
export class InventoryDashboard implements OnInit {

  // ── User context ──────────────────────────────────────────────────
  currentUser?: UserProfileModel;

  // ── Branch selector (ADMIN only) ──────────────────────────────────
  branches: Branch[] = [];
  selectedBranchId?: number;

  // ── Inventory data ────────────────────────────────────────────────
  products: BranchInventoryResponse[] = [];
  filteredProducts: BranchInventoryResponse[] = [];
  searchTerm: string = '';
  errorMessage: string = '';
  successMessage: string = '';

  groupedProducts: { cat: string; catColor: string; catEmoji: string; items: BranchInventoryResponse[] }[] = [];
  stats = {
    active: 0,
    lowOrInactive: 0,
    catCounts: {} as Record<string, number>
  };

  currentView: 'card' | 'table' = 'card';
  currentCat: string = 'all';
  activeTab: 'inventory' | 'orders' | 'reports' = 'inventory';

  readonly EMOJIS: Record<string, string> = {
    'Dry Fruits': '🌰',
    'Sweeteners': '🍯',
    'Spices': '🌶️',
    'Grains': '🌾',
  };

  readonly COLORS: Record<string, string> = {
    'Dry Fruits': '#D4A574',
    'Sweeteners': '#EFC94C',
    'Spices': '#E07A5F',
    'Grains': '#81B29A',
  };

  readonly CATEGORIES: CategoryMeta[] = [
    { name: 'Dry Fruits', dot: '#D4A574', emoji: '🌰', countKey: 'df' },
    { name: 'Sweeteners', dot: '#EFC94C', emoji: '🍯', countKey: 'sw' },
    { name: 'Spices', dot: '#E07A5F', emoji: '🌶️', countKey: 'sp' },
    { name: 'Grains', dot: '#81B29A', emoji: '🌾', countKey: 'gr' },
  ];

  // Custom modal state
  showDeleteModal: boolean = false;
  deleteTargetId: number | null = null;

  constructor(
    private userService: UserService,
    private inventoryService: InventoryService,
    private branchService: BranchService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.userService.getUserProfile().subscribe({
        next: (profile) => {
          this.currentUser = profile;
          // ADMIN: always start with no selected branch — let loadAllBranches() pick first branch
          // EMPLOYEE/BRANCH_MANAGER: use their assigned branchId from profile
          this.selectedBranchId = (profile.role === 'ADMIN')
            ? undefined
            : (profile.branchId > 0 ? profile.branchId : undefined);

          if (this.currentUser.role === 'ADMIN') {
            // loadAllBranches() will set selectedBranchId then call loadInventory()
            // Do NOT call loadInventory() here — selectedBranchId is still undefined
            this.loadAllBranches();
          } else {
            // EMPLOYEE / BRANCH_MANAGER have a branchId from profile — load immediately
            this.loadInventory();
          }
        },
        error: (err) => {
          console.error('Failed to load user profile:', err);
          this.errorMessage = 'Unable to load user profile.';
          this.cdr.detectChanges();
        }
      });
    }
  }

  loadAllBranches(): void {
    this.branchService.getAllActiveBranches().subscribe({
      next: (branches) => {
        this.branches = branches;
        if (branches.length > 0) {
          // Always set to first branch if nothing is selected yet
          if (!this.selectedBranchId) {
            this.selectedBranchId = branches[0].id;
          }
          // Always load inventory after branches are resolved for ADMIN
          this.loadInventory();
        }
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load branches:', err);
      }
    });
  }

  loadInventory(): void {
    const isAdmin = this.currentUser?.role === 'ADMIN';

    const request$ = isAdmin
      ? this.inventoryService.getAnyBranchInventory(this.selectedBranchId!)
      : this.inventoryService.getMyBranchInventory();

    request$.subscribe({
      next: (data) => {
        this.products = data;
        this.filterProducts();
      },
      error: (err) => {
        this.errorMessage = 'Failed to load inventory.';
        console.error(err);
        this.cdr.detectChanges();
      }
    });
  }

  onBranchChange(): void {
    this.loadInventory();
  }

  getProductCat(p: BranchInventoryResponse): string {
    const n = (p.productName || '').toLowerCase();
    if (n.includes('makhana') || n.includes('dry') || n.includes('nut') || n.includes('almond') || n.includes('cashew')) return 'Dry Fruits';
    if (n.includes('jaggery') || n.includes('mishri') || n.includes('sugar') || n.includes('sweet')) return 'Sweeteners';
    if (n.includes('turmeric') || n.includes('chilli') || n.includes('spice')) return 'Spices';
    if (n.includes('rice') || n.includes('wheat') || n.includes('grain') || n.includes('dal')) return 'Grains';
    return 'Other';
  }

  filterProducts(): void {
    const q = this.searchTerm.toLowerCase();

    // Enrich all products with computed fields
    this.products.forEach(p => {
      p.computedCategory = this.getProductCat(p);
      p.computedStatus = this.getStatusLabel(p);
    });

    this.calculateStats();

    let result = this.products;

    if (this.currentCat !== 'all') {
      result = result.filter(p => p.computedCategory === this.currentCat);
    }
    if (q) {
      result = result.filter(p => p.productName.toLowerCase().includes(q));
    }
    this.filteredProducts = result;

    // Build grouped array for rendering
    const cats = [...new Set(this.filteredProducts.map(p => p.computedCategory))] as string[];
    this.groupedProducts = cats.map(cat => ({
      cat,
      catColor: this.getCatColor(cat),
      catEmoji: this.getCatEmoji(cat),
      items: this.filteredProducts.filter(p => p.computedCategory === cat),
    }));

    this.cdr.detectChanges();
  }

  /** Uses API's lowStock boolean directly — no hardcoded 20kg threshold */
  calculateStats(): void {
    this.stats.active = this.products.length; // All returned items are active inventory entries
    this.stats.lowOrInactive = this.products.filter(p => p.lowStock).length;

    this.stats.catCounts['all'] = this.products.length;
    for (const c of this.CATEGORIES) {
      this.stats.catCounts[c.name] = this.products.filter(p => p.computedCategory === c.name).length;
    }
  }

  onItemChange(p: BranchInventoryResponse): void {
    p.computedStatus = this.getStatusLabel(p);
    this.calculateStats();
  }

  setView(v: 'card' | 'table'): void {
    this.currentView = v;
    this.cdr.detectChanges();
  }

  setTab(tab: 'inventory' | 'orders' | 'reports'): void {
    this.activeTab = tab;
  }

  filterCat(cat: string): void {
    this.currentCat = cat;
    this.filterProducts();
  }

  /** Uses API's lowStock boolean — no hardcoded threshold */
  getStatusLabel(p: BranchInventoryResponse): { label: string; cls: string } {
    if (p.lowStock) return { label: 'Low Stock', cls: 'status-low' };
    return { label: 'Active', cls: 'status-active' };
  }

  getCatColor(cat: string): string {
    return this.COLORS[cat] || '#888';
  }

  getCatEmoji(cat: string): string {
    return this.EMOJIS[cat] || '📦';
  }

  /** Save button — calls branch inventory update API */
  updateStock(p: BranchInventoryResponse): void {
    const body: BranchInventoryRequest = {
      branchId: this.selectedBranchId ?? p.branchId,
      productId: p.productId,
      availableStockKg: p.availableStockKg,
      lowStockThreshold: p.lowStockThreshold
    };

    this.inventoryService.updateStock(body).subscribe({
      next: () => {
        this.successMessage = `Stock updated for "${p.productName}"!`;
        this.cdr.detectChanges();
        this.loadInventory(); // Refresh to get accurate lowStock flag from API
        setTimeout(() => { this.successMessage = ''; this.cdr.detectChanges(); }, 3000);
      },
      error: (err) => {
        this.errorMessage = `Failed to update stock for ${p.productName}.`;
        console.error(err);
        this.cdr.detectChanges();
        setTimeout(() => { this.errorMessage = ''; this.cdr.detectChanges(); }, 3000);
      }
    });
  }

  // ── Delete (ADMIN only) ───────────────────────────────────────────
  deleteProduct(id: number): void {
    this.deleteTargetId = id;
    this.showDeleteModal = true;
    this.cdr.detectChanges();
  }

  confirmDelete(): void {
    if (this.deleteTargetId !== null) {
      // Future: call inventoryService.deleteInventoryEntry(this.deleteTargetId)
      this.successMessage = 'Product removed from inventory.';
      this.showDeleteModal = false;
      this.deleteTargetId = null;
      this.cdr.detectChanges();
      this.loadInventory();
      setTimeout(() => { this.successMessage = ''; this.cdr.detectChanges(); }, 3000);
    }
  }

  cancelDelete(): void {
    this.showDeleteModal = false;
    this.deleteTargetId = null;
    this.cdr.detectChanges();
  }

  /** Currently displayed branch name — from first product's branchName */
  get currentBranchName(): string {
    if (this.products.length > 0) return this.products[0].branchName;
    if (this.currentUser?.role === 'ADMIN') return 'Select a Branch';
    return 'Your Branch';
  }
}
