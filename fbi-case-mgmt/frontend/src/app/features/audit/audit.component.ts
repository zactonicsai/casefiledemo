import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { ApiService } from '../../core/api.service';
import { AuditEvent, Page } from '../../core/api.models';

@Component({
  selector: 'app-audit',
  standalone: true,
  imports: [CommonModule, MatTableModule, MatPaginatorModule],
  template: `
    <div class="page">
      <h1>Audit Log</h1>
      <div class="subtitle">Immutable record of material actions taken in the system.</div>

      <div class="card-section">
        <table mat-table [dataSource]="data().content" class="w-100">
          <ng-container matColumnDef="time">
            <th mat-header-cell *matHeaderCellDef>Time</th>
            <td mat-cell *matCellDef="let e">{{ e.occurredAt | date:'medium' }}</td>
          </ng-container>
          <ng-container matColumnDef="actor">
            <th mat-header-cell *matHeaderCellDef>Actor</th>
            <td mat-cell *matCellDef="let e">{{ e.actorUsername }}</td>
          </ng-container>
          <ng-container matColumnDef="action">
            <th mat-header-cell *matHeaderCellDef>Action</th>
            <td mat-cell *matCellDef="let e"><code>{{ e.action }}</code></td>
          </ng-container>
          <ng-container matColumnDef="case">
            <th mat-header-cell *matHeaderCellDef>Case</th>
            <td mat-cell *matCellDef="let e">{{ e.caseNumber || '—' }}</td>
          </ng-container>
          <ng-container matColumnDef="entity">
            <th mat-header-cell *matHeaderCellDef>Entity</th>
            <td mat-cell *matCellDef="let e">{{ e.entityType }} {{ e.entityId }}</td>
          </ng-container>
          <ng-container matColumnDef="outcome">
            <th mat-header-cell *matHeaderCellDef>Outcome</th>
            <td mat-cell *matCellDef="let e">{{ e.outcome }}</td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="cols"></tr>
          <tr mat-row *matRowDef="let row; columns: cols"></tr>
        </table>
        <mat-paginator [length]="data().totalElements"
                       [pageIndex]="data().number"
                       [pageSize]="data().size"
                       [pageSizeOptions]="[25, 50, 100, 200]"
                       (page)="onPage($event)"></mat-paginator>
      </div>
    </div>
  `
})
export class AuditComponent implements OnInit {
  private api = inject(ApiService);
  cols = ['time','actor','action','case','entity','outcome'];
  data = signal<Page<AuditEvent>>({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 50 });

  ngOnInit() { this.load(); }
  load(page = 0, size = 50) { this.api.auditAll(page, size).subscribe(p => this.data.set(p)); }
  onPage(e: PageEvent) { this.load(e.pageIndex, e.pageSize); }
}
