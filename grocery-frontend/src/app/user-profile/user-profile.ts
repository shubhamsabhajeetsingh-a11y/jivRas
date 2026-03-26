import { Component, OnInit, ChangeDetectorRef, HostListener, ElementRef, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { UserService, UserProfile as UserProfileModel } from '../core/services/user.service';

@Component({
  selector: 'app-user-profile',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-profile.html',
  styleUrl: './user-profile.css',
})
export class UserProfile implements OnInit {
  profile: UserProfileModel | null = null;
  initials: string = '';
  isDropdownOpen: boolean = false;
  isLoggedIn: boolean = false;

  constructor(
    private userService: UserService,
    private router: Router,
    private eRef: ElementRef,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId) && localStorage.getItem('accessToken')) {
      this.isLoggedIn = true;
      this.fetchProfile();
    }
  }

  fetchProfile() {
    this.userService.getProfile().subscribe({
      next: (data) => {
        this.profile = data;
        let f = data.firstName ? data.firstName.charAt(0).toUpperCase() : '';
        let l = data.lastName ? data.lastName.charAt(0).toUpperCase() : '';
        this.initials = (f + l) || 'U'; // default to U if no name
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load profile', err);
        // Token is stale/invalid — clear it and show Login button
        if (isPlatformBrowser(this.platformId)) {
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          localStorage.removeItem('userRole');
        }
        this.isLoggedIn = false;
        this.cdr.detectChanges();
      }
    });
  }

  toggleDropdown() {
    this.isDropdownOpen = !this.isDropdownOpen;
  }

  @HostListener('document:click', ['$event'])
  clickout(event: Event) {
    if(this.eRef && !this.eRef.nativeElement.contains(event.target)) {
      this.isDropdownOpen = false;
    }
  }

  logout() {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('userRole');
    }
    this.router.navigate(['/login']);
  }
}
