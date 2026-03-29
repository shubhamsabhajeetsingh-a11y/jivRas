import { Component, PLATFORM_ID, Inject, NgZone, OnInit, OnDestroy } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from "@angular/router";
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  template: `
    <router-outlet></router-outlet>

    <div *ngIf="showTimeoutPopup" class="modal-overlay">
      <div class="modal-content">
        <h2>Session Expiring</h2>
        <p>You have been inactive for a while. Your session will expire in <strong style="color: red; font-size: 1.2rem">{{ countdown }}</strong> seconds.</p>
        <button (click)="stayLoggedIn()" class="stay-btn">Stay Logged In</button>
      </div>
    </div>
  `,
  styleUrls: ['./app.component.css'],
  imports: [RouterOutlet, CommonModule]
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'grocery-frontend';

  showTimeoutPopup = false;
  countdown = 20;

  private inactivityTimer: any;
  private countdownInterval: any;

  // 15 mins for inactivity limit, 20 secs for countdown
  private readonly INACTIVITY_LIMIT = 15 * 60 * 1000;
  private readonly COUNTDOWN_LIMIT = 20;

  // Track last activity to limit reset frequency
  private lastActivityTime = Date.now();

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
    private ngZone: NgZone
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.setupActivityListeners();
      
      // Reset timer on navigation and check login state
      this.router.events.pipe(
        filter(event => event instanceof NavigationEnd)
      ).subscribe(() => {
        this.resetInactivityTimer();
      });
      
      this.resetInactivityTimer();
    }
  }

  ngOnDestroy() {
    if (isPlatformBrowser(this.platformId)) {
      this.removeActivityListeners();
      clearTimeout(this.inactivityTimer);
      clearInterval(this.countdownInterval);
    }
  }

  private setupActivityListeners() {
    this.ngZone.runOutsideAngular(() => {
      window.addEventListener('mousemove', this.userActivityHandler);
      window.addEventListener('keydown', this.userActivityHandler);
      window.addEventListener('scroll', this.userActivityHandler);
      window.addEventListener('click', this.userActivityHandler);
    });
  }

  private removeActivityListeners() {
    if (isPlatformBrowser(this.platformId)) {
      window.removeEventListener('mousemove', this.userActivityHandler);
      window.removeEventListener('keydown', this.userActivityHandler);
      window.removeEventListener('scroll', this.userActivityHandler);
      window.removeEventListener('click', this.userActivityHandler);
    }
  }

  private userActivityHandler = () => {
    const now = Date.now();
    // Throttle resets to max once every 3 seconds to avoid performance hit
    if (now - this.lastActivityTime > 3000) {
      if (!this.showTimeoutPopup) {
        this.lastActivityTime = now;
        this.resetInactivityTimer();
      }
    }
  };

  private resetInactivityTimer() {
    if (!isPlatformBrowser(this.platformId)) return;

    clearTimeout(this.inactivityTimer);

    // Only set timer if user is logged in
    const isLoggedIn = !!localStorage.getItem('accessToken');
    if (!isLoggedIn) return;

    this.inactivityTimer = setTimeout(() => {
      this.ngZone.run(() => this.showPopup());
    }, this.INACTIVITY_LIMIT);
  }

  showPopup() {
    this.showTimeoutPopup = true;
    this.countdown = this.COUNTDOWN_LIMIT;

    this.countdownInterval = setInterval(() => {
      this.countdown--;
      
      if (this.countdown <= 0) {
        clearInterval(this.countdownInterval);
        this.logout();
      }
    }, 1000);
  }

  stayLoggedIn() {
    this.showTimeoutPopup = false;
    clearInterval(this.countdownInterval);
    this.resetInactivityTimer();
  }

  logout() {
    this.showTimeoutPopup = false;
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userRole');
    this.router.navigate(['/login']);
  }
}