import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService, MorningSummaryDTO } from '../core/services/dashboard.service';

@Component({
  selector: 'app-morning-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './morning-dashboard.component.html',
  styleUrls: ['./morning-dashboard.component.css'],
  // OnPush isolates this component from parent cdr.detectChanges() calls in InventoryDashboard.
  // Without this, every loadInventory() / loadAllBranches() in the parent would cascade here
  // and cause the dashboard to flicker or appear to refresh.
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MorningDashboardComponent implements OnInit {

  summary: MorningSummaryDTO | null = null;
  loading = true;
  error = '';
  today = new Date().toLocaleDateString('en-IN', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });

  constructor(
    private dashboardService: DashboardService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.dashboardService.getMorningSummary().subscribe({
      next: (data) => {
        this.summary = data;
        this.loading = false;
        this.cdr.markForCheck(); // notify Angular this OnPush component has new data to render
      },
      error: () => {
        this.error = 'Failed to load dashboard. Please try again.';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }
}
