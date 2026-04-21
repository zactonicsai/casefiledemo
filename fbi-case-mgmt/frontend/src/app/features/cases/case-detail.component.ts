import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { MatExpansionModule } from '@angular/material/expansion';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { CaseDetail, DocumentType } from '../../core/api.models';

@Component({
  selector: 'app-case-detail',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    MatTableModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatDividerModule, MatChipsModule, MatExpansionModule
  ],
  template: `
    @if (data(); as c) {
      <div class="page">
        <div style="display:flex; align-items:flex-start; gap:16px;">
          <div style="flex:1">
            <div style="color:#6b7280; font-size:0.85rem">Case File</div>
            <h1 style="margin:4px 0 8px">{{ c.caseNumber }}</h1>
            <div style="font-size:1.1rem">{{ c.title }}</div>
          </div>
          <span class="badge" [ngClass]="c.status" style="font-size:0.85rem">{{ c.status }}</span>
        </div>

        <div class="card-section" style="margin-top:16px">
          <div class="meta-grid">
            <div><span class="lbl">Classification</span><span>{{ c.classificationCode }}</span></div>
            <div><span class="lbl">Office</span><span>{{ c.originatingOffice }}</span></div>
            <div><span class="lbl">Serial</span><span>{{ c.serialNumber }}</span></div>
            <div><span class="lbl">Created</span><span>{{ c.createdAt | date:'medium' }}</span></div>
            <div><span class="lbl">Opened</span><span>{{ c.openedAt ? (c.openedAt | date:'medium') : '—' }}</span></div>
            <div><span class="lbl">Closed</span><span>{{ c.closedAt ? (c.closedAt | date:'medium') : '—' }}</span></div>
          </div>
          @if (c.synopsis) { <p style="margin-top:16px">{{ c.synopsis }}</p> }

          @if (c.tags?.length) {
            <mat-chip-set>
              @for (t of c.tags; track t) { <mat-chip>{{ t }}</mat-chip> }
            </mat-chip-set>
          }
        </div>

        <!-- Actions -->
        <div class="card-section">
          <h3 style="margin-top:0">Workflow Actions</h3>
          <mat-form-field appearance="outline" class="w-full">
            <mat-label>Comments / reason</mat-label>
            <input matInput [(ngModel)]="comments">
          </mat-form-field>
          <div class="toolbar-gap">
            @if (c.status === 'DRAFT' && auth.hasAnyRole('AGENT','SUPERVISOR','SYSTEM_ADMIN')) {
              <button mat-raised-button color="primary" (click)="submit()">Submit for Approval</button>
            }
            @if (c.status === 'PENDING_APPROVAL' && auth.hasAnyRole('SUPERVISOR','SYSTEM_ADMIN')) {
              <button mat-raised-button color="primary" (click)="approve()">Approve</button>
              <button mat-stroked-button color="warn"  (click)="reject()">Reject</button>
            }
            @if (c.status === 'OPEN' && auth.hasAnyRole('AGENT','SUPERVISOR','SYSTEM_ADMIN')) {
              <button mat-stroked-button (click)="requestClosure()">Request Closure</button>
            }
            @if (c.status === 'CLOSURE_REVIEW' && auth.hasAnyRole('SUPERVISOR','SYSTEM_ADMIN')) {
              <button mat-raised-button color="primary" (click)="confirmClosure()">Confirm Closure</button>
            }
          </div>
          @if (actionMsg) { <p style="color:#047857; margin-top:12px">{{ actionMsg }}</p> }
        </div>

        <!-- Serial documents -->
        <div class="card-section">
          <div style="display:flex; align-items:center;">
            <h3 style="flex:1; margin:0">Serial Documents ({{ c.documents.length }})</h3>
          </div>

          <table mat-table [dataSource]="c.documents" class="w-100" style="margin-top:12px">
            <ng-container matColumnDef="idx">
              <th mat-header-cell *matHeaderCellDef>#</th>
              <td mat-cell *matCellDef="let d">{{ d.serialIndex }}</td>
            </ng-container>
            <ng-container matColumnDef="type">
              <th mat-header-cell *matHeaderCellDef>Type</th>
              <td mat-cell *matCellDef="let d"><span class="badge">{{ d.documentType }}</span></td>
            </ng-container>
            <ng-container matColumnDef="title">
              <th mat-header-cell *matHeaderCellDef>Title</th>
              <td mat-cell *matCellDef="let d">{{ d.title }}</td>
            </ng-container>
            <ng-container matColumnDef="file">
              <th mat-header-cell *matHeaderCellDef>File</th>
              <td mat-cell *matCellDef="let d">{{ d.originalFilename }} · {{ d.sizeBytes | number }} B</td>
            </ng-container>
            <ng-container matColumnDef="state">
              <th mat-header-cell *matHeaderCellDef>State</th>
              <td mat-cell *matCellDef="let d"><span class="badge">{{ d.processingStatus }}</span></td>
            </ng-container>
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef></th>
              <td mat-cell *matCellDef="let d">
                <button mat-icon-button (click)="download(d.id)"><mat-icon>download</mat-icon></button>
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="docCols"></tr>
            <tr mat-row *matRowDef="let row; columns: docCols"></tr>
          </table>

          @if (c.status !== 'CLOSED' && c.status !== 'ARCHIVED') {
            <mat-divider style="margin:20px 0"></mat-divider>
            <h4>Upload a new serial document</h4>
            <div style="display:flex; gap:12px; flex-wrap:wrap">
              <mat-form-field appearance="outline" style="flex:1 1 180px">
                <mat-label>Type</mat-label>
                <mat-select [(value)]="upType">
                  <mat-option value="FD_302">FD-302 Interview</mat-option>
                  <mat-option value="EC">Electronic Comm (EC)</mat-option>
                  <mat-option value="LHM">Letterhead Memo</mat-option>
                  <mat-option value="EVIDENCE_LOG">Evidence Log</mat-option>
                  <mat-option value="PHOTO">Photograph</mat-option>
                  <mat-option value="NOTE">Investigator Notes</mat-option>
                  <mat-option value="AFFIDAVIT">Affidavit</mat-option>
                  <mat-option value="LEGAL_PROCESS">Legal Process</mat-option>
                  <mat-option value="OTHER">Other</mat-option>
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="outline" style="flex:2 1 360px">
                <mat-label>Document title</mat-label>
                <input matInput [(ngModel)]="upTitle">
              </mat-form-field>
            </div>
            <input type="file" #f (change)="onFile($event)" style="margin-bottom:12px">
            <div>
              <button mat-raised-button color="primary"
                      [disabled]="!upFile || !upTitle || uploading" (click)="upload()">
                <mat-icon>upload</mat-icon>
                {{ uploading ? 'Uploading…' : 'Upload' }}
              </button>
            </div>
            @if (uploadMsg) { <p style="color:#047857; margin-top:8px">{{ uploadMsg }}</p> }
            @if (uploadErr) { <p style="color:#b91c1c; margin-top:8px">{{ uploadErr }}</p> }
          }
        </div>
      </div>
    } @else {
      <div class="page"><p>Loading…</p></div>
    }
  `,
  styles: [`
    .meta-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 14px; }
    .meta-grid .lbl { display:block; color:#6b7280; font-size:0.75rem; text-transform:uppercase;
                       letter-spacing:1px; margin-bottom:4px; }
    .w-full { width:100%; }
  `]
})
export class CaseDetailComponent implements OnInit {
  private api   = inject(ApiService);
  private route = inject(ActivatedRoute);
  auth          = inject(AuthService);

  data       = signal<CaseDetail | null>(null);
  docCols    = ['idx','type','title','file','state','actions'];
  comments   = '';
  actionMsg  = '';

  upType: DocumentType = 'FD_302';
  upTitle = '';
  upFile: File | null = null;
  uploading = false;
  uploadMsg = ''; uploadErr = '';

  ngOnInit() { this.reload(); }

  private caseId() { return this.route.snapshot.paramMap.get('id')!; }

  reload() { this.api.getCase(this.caseId()).subscribe(c => this.data.set(c)); }

  submit()          { this.doAction(this.api.submitForApproval(this.caseId(), this.comments), 'Submitted'); }
  approve()         { this.doAction(this.api.approve(this.caseId(), this.comments), 'Approved'); }
  reject()          { this.doAction(this.api.reject(this.caseId(), this.comments), 'Rejected'); }
  requestClosure()  { this.doAction(this.api.requestClosure(this.caseId(), this.comments || 'closure requested'), 'Closure requested'); }
  confirmClosure()  { this.doAction(this.api.confirmClosure(this.caseId(), this.comments || 'closure confirmed'), 'Closed'); }

  private doAction(obs: any, label: string) {
    obs.subscribe({
      next: () => { this.actionMsg = `${label} — workflow updated (may take a second to reflect).`;
                    setTimeout(() => this.reload(), 800); },
      error: (e: any) => this.actionMsg = e?.error?.detail || 'Action failed'
    });
  }

  onFile(ev: Event) { this.upFile = (ev.target as HTMLInputElement).files?.[0] || null; }

  upload() {
    if (!this.upFile || !this.upTitle) return;
    this.uploading = true; this.uploadErr = ''; this.uploadMsg = '';
    this.api.uploadDocument(this.caseId(),
      { documentType: this.upType, title: this.upTitle }, this.upFile).subscribe({
        next: () => { this.uploadMsg = 'Uploaded — processing in background.'; this.uploading = false;
                      this.upFile = null; this.upTitle = ''; setTimeout(() => this.reload(), 1000); },
        error: e => { this.uploadErr = e?.error?.detail || 'Upload failed'; this.uploading = false; }
      });
  }

  download(docId: string) {
    this.api.getDownloadUrl(docId).subscribe(r => window.open(r.url, '_blank'));
  }
}
