import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { CaseStatus, CaseSummary, Page } from '../../core/api.models';

@Component({
  selector: 'app-case-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    MatTableModule, MatPaginatorModule, MatFormFieldModule,
    MatSelectModule, MatButtonModule, MatIconModule
  ],
  template: `
    <div class="page">
      <div style="display:flex; align-items:center; gap:16px;">
        <h1 style="flex:1; margin:0">Cases</h1>
        @if (auth.hasAnyRole('AGENT','SUPERVISOR','SYSTEM_ADMIN')) {
          <a mat-raised-button color="primary" routerLink="/cases/new">
            <mat-icon>add</mat-icon> New Case
          </a>
        }
      </div>
      <div class="subtitle">Browse and manage case files.</div>

      <div class="card-section">
        <mat-form-field appearance="outline" style="width:220px">
          <mat-label>Status filter</mat-label>
          <mat-select [(value)]="status" (selectionChange)="reload()">
            <mat-option [value]="undefined">All</mat-option>
            <mat-option value="DRAFT">Draft</mat-option>
            <mat-option value="PENDING_APPROVAL">Pending Approval</mat-option>
            <mat-option value="OPEN">Open</mat-option>
            <mat-option value="SUSPENDED">Suspended</mat-option>
            <mat-option value="CLOSURE_REVIEW">Closure Review</mat-option>
            <mat-option value="CLOSED">Closed</mat-option>
            <mat-option value="ARCHIVED">Archived</mat-option>
          </mat-select>
        </mat-form-field>

        <table mat-table [dataSource]="data().content" class="w-100">
          <ng-container matColumnDef="caseNumber">
            <th mat-header-cell *matHeaderCellDef>Case #</th>
            <td mat-cell *matCellDef="let c">
              <a [routerLink]="['/cases', c.id]" class="link">{{ c.caseNumber }}</a>
            </td>
          </ng-container>
          <ng-container matColumnDef="title">
            <th mat-header-cell *matHeaderCellDef>Title</th>
            <td mat-cell *matCellDef="let c">{{ c.title }}</td>
          </ng-container>
          <ng-container matColumnDef="classification">
            <th mat-header-cell *matHeaderCellDef>Class.</th>
            <td mat-cell *matCellDef="let c">{{ c.classificationCode }}</td>
          </ng-container>
          <ng-container matColumnDef="office">
            <th mat-header-cell *matHeaderCellDef>Office</th>
            <td mat-cell *matCellDef="let c">{{ c.originatingOffice }}</td>
          </ng-container>
          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Status</th>
            <td mat-cell *matCellDef="let c">
              <span class="badge" [ngClass]="c.status">{{ c.status }}</span>
            </td>
          </ng-container>
          <ng-container matColumnDef="docs">
            <th mat-header-cell *matHeaderCellDef>Docs</th>
            <td mat-cell *matCellDef="let c">{{ c.documentCount }}</td>
          </ng-container>
          <ng-container matColumnDef="created">
            <th mat-header-cell *matHeaderCellDef>Created</th>
            <td mat-cell *matCellDef="let c">{{ c.createdAt | date:'short' }}</td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="cols"></tr>
          <tr mat-row *matRowDef="let row; columns: cols"></tr>
        </table>

        <mat-paginator [length]="data().totalElements"
                       [pageIndex]="data().number"
                       [pageSize]="data().size"
                       [pageSizeOptions]="[10, 25, 50, 100]"
                       (page)="onPage($event)">
        </mat-paginator>
      </div>
    </div>
  `,
  styles: [`
    .link { color: #1d4ed8; text-decoration: none; font-weight: 500; }
    .link:hover { text-decoration: underline; }
  `]
})
export class CaseListComponent implements OnInit {
  private api = inject(ApiService);
  auth        = inject(AuthService);

  status: CaseStatus | undefined;
  cols        = ['caseNumber','title','classification','office','status','docs','created'];
  data        = signal<Page<CaseSummary>>({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 25 });

  ngOnInit() { this.reload(); }

  reload(page = 0, size = 25) {
    this.api.listCases(this.status, page, size).subscribe(p => this.data.set(p));
  }
  onPage(e: PageEvent) { this.reload(e.pageIndex, e.pageSize); }
}
