import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { environment } from '../environments/environment';

@Component({
  selector: 'app-login',
  standalone: true,
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  imports: [CommonModule, ReactiveFormsModule, RouterLink]
})
export class LoginComponent implements OnInit {
  loginForm: FormGroup;
  errorMessage: string = '';

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    // If user navigated back to login page, clear their session
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('userRole');
    }
  }

  onSubmit(): void {
    if (!this.loginForm.valid) return;

    const { username, password } = this.loginForm.value;

    this.encryptAesGcm(password).then(encryptedPassword => {
      this.http.post<any>(
        `${environment.apiUrl}/api/users/login`,
        { username, password: encryptedPassword }
      ).subscribe({
        next: (response: any) => {
          localStorage.setItem('accessToken', response.accessToken);
          localStorage.setItem('refreshToken', response.refreshToken);
          localStorage.setItem('userRole', response.role);
          localStorage.removeItem('guestId');

          if (response.role === 'EMPLOYEE' || response.role === 'ADMIN' || response.role === 'BRANCH_MANAGER') {
            this.router.navigate(['/inventory-dashboard']);
          } else {
            this.router.navigate(['/products']);
          }
        },
        error: (err) => {
          console.error('Login failed:', err);
          this.errorMessage = 'Invalid username or password.';
        }
      });
    }).catch(() => {
      this.errorMessage = 'Encryption error. Please refresh and try again.';
    });
  }

  /**
   * AES-256-GCM encryption using the Web Crypto API.
   * The 32-byte key matches the HEX_KEY hardcoded in RsaKeyService.java.
   * Returns "<base64-iv>:<base64-ciphertext>" — the format Java expects.
   */
  private async encryptAesGcm(plaintext: string): Promise<string> {
    const HEX_KEY = '6a8f3b2e9c1d5f7a0e4b8c2d6f9a1e3b7d5c8f2a4e6b0d3f5a7c9e1b3d7f9ac4';

    const keyBytes = new Uint8Array(HEX_KEY.match(/.{2}/g)!.map(b => parseInt(b, 16)));
    const cryptoKey = await crypto.subtle.importKey(
      'raw', keyBytes, { name: 'AES-GCM' }, false, ['encrypt']
    );

    const iv = crypto.getRandomValues(new Uint8Array(12));
    const encoded = new TextEncoder().encode(plaintext);
    const cipherBuffer = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, cryptoKey, encoded);

    const ivB64   = btoa(String.fromCharCode(...iv));
    const ctB64   = btoa(String.fromCharCode(...new Uint8Array(cipherBuffer)));
    return `${ivB64}:${ctB64}`;
  }
}
