import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    CommonModule, RouterLink, RouterLinkActive, RouterOutlet,
    MatSidenavModule, MatToolbarModule, MatButtonModule, MatIconModule,
    MatListModule, MatMenuModule
  ],
  template: `
    <mat-sidenav-container class="shell">
      <mat-sidenav mode="side" opened class="sidenav">
        <div class="brand">
          <div class="seal">Demo Only</div>
          <div class="brand-sub">Case Management</div>
        </div>
        <mat-nav-list>
          <a mat-list-item routerLink="/dashboard" routerLinkActive="active">
            <mat-icon matListItemIcon>dashboard</mat-icon>
            <span matListItemTitle>Dashboard</span>
          </a>
          <a mat-list-item routerLink="/cases" routerLinkActive="active">
            <mat-icon matListItemIcon>folder</mat-icon>
            <span matListItemTitle>Cases</span>
          </a>
          <a mat-list-item routerLink="/search" routerLinkActive="active">
            <mat-icon matListItemIcon>search</mat-icon>
            <span matListItemTitle>Search</span>
          </a>
          @if (auth.hasAnyRole('AUDITOR','SYSTEM_ADMIN')) {
            <a mat-list-item routerLink="/audit" routerLinkActive="active">
              <mat-icon matListItemIcon>verified</mat-icon>
              <span matListItemTitle>Audit Log</span>
            </a>
          }
        </mat-nav-list>
      </mat-sidenav>

      <mat-sidenav-content>
        <mat-toolbar class="topbar">
          <span class="page-title">{{ pageTitle() }}</span>
          <span class="spacer"></span>
          <button mat-button [matMenuTriggerFor]="userMenu">
            <mat-icon>person</mat-icon>
            {{ auth.user()?.fullName || auth.user()?.username }}
          </button>
          <mat-menu #userMenu="matMenu">
            <div class="menu-info">
              <div><strong>{{ auth.user()?.username }}</strong></div>
              <div class="muted">{{ auth.user()?.email }}</div>
              <div class="muted roles">{{ (auth.user()?.roles || []).join(', ') }}</div>
            </div>
            <button mat-menu-item (click)="auth.logout()">
              <mat-icon>logout</mat-icon> Sign out
            </button>
          </mat-menu>
        </mat-toolbar>

        <router-outlet></router-outlet>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`
    .shell    { height: 100vh; }
    .sidenav  { width: 240px; background: #111827; color: #e5e7eb; border-right: none; }
    .brand    { padding: 28px 24px 16px; border-bottom: 1px solid #1f2937; }
    .seal     { font-weight: 700; letter-spacing: 6px; color: #60a5fa; font-size: 1.05rem; }
    .brand-sub{ font-size: 0.75rem; color: #9ca3af; margin-top: 4px; }
    ::ng-deep .sidenav .mat-mdc-list-item { color: #cbd5e1 !important; }
    ::ng-deep .sidenav .mat-mdc-list-item .mat-icon { color: #94a3b8 !important; }
    ::ng-deep .sidenav .mat-mdc-list-item.active { background: #1d4ed8 !important; color: #fff !important; }
    ::ng-deep .sidenav .mat-mdc-list-item.active .mat-icon { color: #fff !important; }
    .topbar   { background: #fff; box-shadow: 0 1px 2px rgba(17,24,39,.05); color: #111827; }
    .page-title { font-weight: 500; font-size: 1.05rem; }
    .menu-info { padding: 12px 16px; min-width: 220px; }
    .menu-info .muted { color: #6b7280; font-size: 0.85rem; }
    .menu-info .roles { margin-top: 6px; font-size: 0.75rem; }
  `]
})
export class ShellComponent {
  auth = inject(AuthService);
  pageTitle = () => 'Demo Only';
}
