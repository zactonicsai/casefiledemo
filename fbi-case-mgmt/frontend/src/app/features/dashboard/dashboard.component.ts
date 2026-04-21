import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { DashboardStats } from '../../core/api.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, MatCardModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page">
      <h1>Welcome, {{ auth.user()?.fullName || auth.user()?.username }}</h1>
      <div class="subtitle">Case management at a glance</div>

      @if (stats(); as s) {
        <div class="tile-row">
          <div class="stat-tile">
            <div class="label">Total Cases</div>
            <div class="value">{{ s.totalCases }}</div>
          </div>
          <div class="stat-tile">
            <div class="label">Open</div>
            <div class="value" style="color:#047857">{{ s.openCases }}</div>
          </div>
          <div class="stat-tile">
            <div class="label">Pending Approval</div>
            <div class="value" style="color:#b45309">{{ s.pendingApproval }}</div>
          </div>
          <div class="stat-tile">
            <div class="label">Closed</div>
            <div class="value" style="color:#4338ca">{{ s.closedCases }}</div>
          </div>
        </div>

        <div class="card-section">
          <h2 style="margin-top:0">Breakdown by Status</h2>
          <div class="breakdown">
            @for (k of statusKeys(); track k) {
              <div class="bar-row">
                <span class="badge" [ngClass]="k">{{ k }}</span>
                <div class="bar">
                  <div class="fill" [style.width.%]="pct(s.byStatus[k], s.totalCases)"></div>
                </div>
                <span class="num">{{ s.byStatus[k] || 0 }}</span>
              </div>
            }
          </div>
        </div>
      } @else {
        <p>Loading…</p>
      }

      <div class="card-section">
        <h2 style="margin-top:0">Quick Actions</h2>
        <a mat-raised-button color="primary" routerLink="/cases/new">
          <mat-icon>add</mat-icon> Create new case
        </a>
        &nbsp;
        <a mat-stroked-button routerLink="/search">
          <mat-icon>search</mat-icon> Search documents
        </a>
      </div>
    </div>
  `,
  styles: [`
    .breakdown { display: flex; flex-direction: column; gap: 10px; }
    .bar-row   { display: grid; grid-template-columns: 160px 1fr 60px; align-items: center; gap: 12px; }
    .bar       { background: #e5e7eb; border-radius: 999px; height: 10px; overflow: hidden; }
    .fill      { height: 100%; background: #2563eb; transition: width 0.3s; }
    .num       { text-align: right; font-weight: 500; }
  `]
})
export class DashboardComponent implements OnInit {
  private api  = inject(ApiService);
  auth         = inject(AuthService);
  stats        = signal<DashboardStats | null>(null);

  statusKeys = () => ['DRAFT','PENDING_APPROVAL','OPEN','SUSPENDED','CLOSURE_REVIEW','CLOSED','ARCHIVED'] as const;
  pct(n: number | undefined, total: number) { return total > 0 ? ((n || 0) / total * 100) : 0; }

  ngOnInit() {
    this.api.dashboardStats().subscribe(s => this.stats.set(s));
  }
}
