import { Component, OnInit, Inject, PLATFORM_ID, ChangeDetectorRef } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { OrderService } from '../core/services/order.service';

@Component({
  selector: 'app-order-confirmation',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './order-confirmation.component.html',
  styleUrls: ['./order-confirmation.component.css']
})
export class OrderConfirmationComponent implements OnInit {

  order: any = null;
  loading = true;
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private orderService: OrderService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      const orderId = Number(this.route.snapshot.paramMap.get('id'));
      if (orderId) {
        this.loadOrder(orderId);
      } else {
        this.router.navigate(['/products']);
      }
    }
  }

  loadOrder(orderId: number): void {
    this.orderService.getOrder(orderId).subscribe({
      next: (data) => {
        this.order = data;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = 'Could not load order details.';
        this.loading = false;
        console.error(err);
        this.cdr.detectChanges();
      }
    });
  }

  goToProducts(): void {
    this.router.navigate(['/products']);
  }
}
