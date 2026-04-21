import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  template: `
    <div class="login-wrap">
      <div class="login-card">
        <div class="seal">SENTINEL</div>
        <h1>Case Management System</h1>
        <p class="disclaimer">
          UNCLASSIFIED reference system. Authorized users only.
          All activity is logged and subject to audit.
        </p>
        <button mat-raised-button color="primary" (click)="auth.login()">
          <mat-icon>login</mat-icon>&nbsp;Sign in with Keycloak
        </button>
      </div>
    </div>
  `,
  styles: [`
    .login-wrap { min-height: 100vh; display: flex; align-items: center; justify-content: center;
                  background: linear-gradient(135deg, #1f2937 0%, #111827 100%); }
    .login-card { background: #fff; border-radius: 16px; padding: 48px; width: 420px; text-align: center;
                  box-shadow: 0 20px 60px rgba(0,0,0,0.4); }
    .seal { font-weight: 700; letter-spacing: 6px; color: #1d4ed8; margin-bottom: 16px; font-size: 0.8rem; }
    h1 { font-weight: 500; margin: 0 0 20px; }
    .disclaimer { color: #6b7280; font-size: 0.85rem; margin-bottom: 28px; line-height: 1.5; }
  `]
})
export class LoginComponent {
  auth = inject(AuthService);
}
