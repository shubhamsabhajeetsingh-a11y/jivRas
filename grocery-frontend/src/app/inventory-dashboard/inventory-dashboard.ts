import { Component, OnInit, ChangeDetectorRef, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ProductService } from '../core/services/product.service';
import { UserProfile } from '../user-profile/user-profile';
import { OrdersComponent } from '../orders/orders.component';

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
  products: any[] = [];
  filteredProducts: any[] = [];
  searchTerm: string = '';
  errorMessage: string = '';
  successMessage: string = '';

  groupedProducts: { cat: string; catColor: string; catEmoji: string; items: any[] }[] = [];
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
    private productService: ProductService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.loadProducts();
    }
  }

  loadProducts(): void {
    this.productService.getAllProducts().subscribe({
      next: (data) => {
        this.products = data;
        this.filterProducts();
      },
      error: (err) => {
        this.errorMessage = 'Failed to load products.';
        console.error(err);
        this.cdr.detectChanges();
      }
    });
  }

  getProductCat(p: any): string {
    if (p.category && typeof p.category === 'object' && p.category.name) return p.category.name;
    if (typeof p.category === 'string') return p.category;
    if (p.cat) return p.cat;
    
    const n = (p.name || '').toLowerCase();
    if (n.includes('makhana') || n.includes('dry') || n.includes('nut') || n.includes('almond') || n.includes('cashew')) return 'Dry Fruits';
    if (n.includes('jaggery') || n.includes('mishri') || n.includes('sugar') || n.includes('sweet')) return 'Sweeteners';
    if (n.includes('turmeric') || n.includes('chilli') || n.includes('spice')) return 'Spices';
    if (n.includes('rice') || n.includes('wheat') || n.includes('grain')) return 'Grains';
    return 'Other';
  }

  filterProducts(): void {
    const q = this.searchTerm.toLowerCase();
    
    // Enrich all products first
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
      result = result.filter(p => p.name.toLowerCase().includes(q));
    }
    this.filteredProducts = result;

    // Build the grouped array for rendering
    const cats = [...new Set(this.filteredProducts.map(p => p.computedCategory))];
    this.groupedProducts = cats.map(cat => ({
      cat,
      catColor: this.getCatColor(cat),
      catEmoji: this.getCatEmoji(cat),
      items: this.filteredProducts.filter(p => p.computedCategory === cat),
    }));

    this.cdr.detectChanges();
  }

  calculateStats(): void {
    this.stats.active = this.products.filter(p => p.active !== false).length;
    this.stats.lowOrInactive = this.products.filter(p => p.active === false || (p.availableStockKg ?? p.stock ?? 0) < 20).length;
    
    this.stats.catCounts['all'] = this.products.length;
    for (const c of this.CATEGORIES) {
      this.stats.catCounts[c.name] = this.products.filter(p => p.computedCategory === c.name).length;
    }
  }

  onItemChange(p: any): void {
    p.computedStatus = this.getStatusLabel(p);
    this.calculateStats();
  }

  toggleStatus(p: any): void {
    p.active = p.active === false ? true : false;
    this.onItemChange(p);
  }

  setView(v: 'card' | 'table'): void {
    this.currentView = v;
    this.cdr.detectChanges();
  }

  setTab(tab: 'inventory' | 'orders' | 'reports'): void {
    this.activeTab = tab;
    // No backend API calls should trigger when switching to orders or reports per requirements
  }

  filterCat(cat: string): void {
    this.currentCat = cat;
    this.filterProducts();
  }

  getStatusLabel(p: any): { label: string; cls: string } {
    const stock = p.availableStockKg ?? p.stock ?? 0;
    if (stock < 20) return { label: 'Low Stock', cls: 'status-low' };
    if (p.active === false) return { label: 'Inactive', cls: 'status-inactive' };
    return { label: 'Active', cls: 'status-active' };
  }

  getCatColor(cat: string): string {
    return this.COLORS[cat] || '#888';
  }

  getCatEmoji(cat: string): string {
    return this.EMOJIS[cat] || '📦';
  }

  updateProduct(product: any): void {
    this.productService.updateProduct(product.id, product).subscribe({
      next: () => {
        this.successMessage = `Product "${product.name}" updated successfully!`;
        this.cdr.detectChanges();
        setTimeout(() => { this.successMessage = ''; this.cdr.detectChanges(); }, 3000);
      },
      error: (err) => {
        this.errorMessage = `Failed to update ${product.name}.`;
        console.error(err);
        this.cdr.detectChanges();
        setTimeout(() => { this.errorMessage = ''; this.cdr.detectChanges(); }, 3000);
      }
    });
  }

  deleteProduct(id: number): void {
    this.deleteTargetId = id;
    this.showDeleteModal = true;
    this.cdr.detectChanges();
  }

  confirmDelete(): void {
    if (this.deleteTargetId !== null) {
      this.productService.deleteProduct(this.deleteTargetId).subscribe({
        next: () => {
          this.successMessage = 'Product deleted successfully!';
          this.showDeleteModal = false;
          this.deleteTargetId = null;
          this.cdr.detectChanges();
          this.loadProducts();
          setTimeout(() => { this.successMessage = ''; this.cdr.detectChanges(); }, 3000);
        },
        error: (err) => {
          this.errorMessage = 'Failed to delete product.';
          this.showDeleteModal = false;
          this.deleteTargetId = null;
          console.error(err);
          this.cdr.detectChanges();
          setTimeout(() => { this.errorMessage = ''; this.cdr.detectChanges(); }, 3000);
        }
      });
    }
  }

  cancelDelete(): void {
    this.showDeleteModal = false;
    this.deleteTargetId = null;
    this.cdr.detectChanges();
  }
}
