import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { ApiService } from '../../core/api.service';

@Component({
  selector: 'app-case-create',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatChipsModule
  ],
  template: `
    <div class="page" style="max-width:720px">
      <h1>New Case</h1>
      <div class="subtitle">A draft case file is created. Submit for supervisor approval to open.</div>

      <div class="card-section">
        <form (ngSubmit)="create()" #f="ngForm">
          <mat-form-field appearance="outline" style="width:48%">
            <mat-label>Classification</mat-label>
            <mat-select name="classification" [(ngModel)]="classification" required>
              <mat-option value="066">066 — Administrative Matter</mat-option>
              <mat-option value="089">089 — Drug Investigation</mat-option>
              <mat-option value="091">091 — Bank Robbery</mat-option>
              <mat-option value="111">111 — Violent Crime</mat-option>
              <mat-option value="174">174 — Bombing Matters</mat-option>
              <mat-option value="196">196 — Fraud Against the Government</mat-option>
              <mat-option value="245">245 — Domestic Terrorism</mat-option>
              <mat-option value="281">281 — Foreign Counterintelligence</mat-option>
              <mat-option value="288">288 — Cyber Intrusion</mat-option>
              <mat-option value="305">305 — Public Corruption</mat-option>
            </mat-select>
          </mat-form-field>
          &nbsp;
          <mat-form-field appearance="outline" style="width:48%">
            <mat-label>Originating Office</mat-label>
            <mat-select name="office" [(ngModel)]="office" required>
              <mat-option value="HQ">HQ — Headquarters</mat-option>
              <mat-option value="NY">NY — New York</mat-option>
              <mat-option value="LA">LA — Los Angeles</mat-option>
              <mat-option value="WF">WF — Washington Field</mat-option>
              <mat-option value="CH">CH — Chicago</mat-option>
              <mat-option value="MM">MM — Miami</mat-option>
              <mat-option value="SF">SF — San Francisco</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline" class="w-full">
            <mat-label>Title</mat-label>
            <input matInput name="title" [(ngModel)]="title" required minlength="3" maxlength="255">
          </mat-form-field>

          <mat-form-field appearance="outline" class="w-full">
            <mat-label>Synopsis</mat-label>
            <textarea matInput rows="5" name="synopsis" [(ngModel)]="synopsis" maxlength="10000"></textarea>
          </mat-form-field>

          <mat-form-field appearance="outline" class="w-full">
            <mat-label>Tags (comma-separated)</mat-label>
            <input matInput name="tagsRaw" [(ngModel)]="tagsRaw"
                   placeholder="e.g. organized-crime, Chicago, RICO">
          </mat-form-field>

          <div class="fab-footer">
            <a mat-stroked-button routerLink="/cases">Cancel</a>&nbsp;
            <button mat-raised-button color="primary" type="submit" [disabled]="f.invalid || busy">
              <mat-icon>add</mat-icon> Create Case
            </button>
          </div>

          @if (error) { <p style="color:#b91c1c; margin-top:12px">{{ error }}</p> }
        </form>
      </div>
    </div>
  `,
  styles: [`.w-full { width: 100%; }`]
})
export class CaseCreateComponent {
  private api    = inject(ApiService);
  private router = inject(Router);

  classification = '111';
  office         = 'HQ';
  title          = '';
  synopsis       = '';
  tagsRaw        = '';
  busy = false;
  error = '';

  create() {
    this.busy  = true;
    this.error = '';
    const tags = this.tagsRaw
      .split(',').map(t => t.trim()).filter(Boolean);
    this.api.createCase({
      classificationCode: this.classification,
      originatingOffice:  this.office,
      title:              this.title,
      synopsis:           this.synopsis || undefined,
      tags
    }).subscribe({
      next: c => this.router.navigate(['/cases', c.id]),
      error: e => { this.busy = false; this.error = e?.error?.detail || 'Failed to create case'; }
    });
  }
}
