import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { ApiService } from '../../core/api.service';
import { SearchResponse } from '../../core/api.models';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, MatChipsModule
  ],
  template: `
    <div class="page">
      <h1>Search</h1>
      <div class="subtitle">Full-text search over indexed documents (titles, descriptions, OCR text).</div>

      <div class="card-section">
        <form (ngSubmit)="run()" style="display:flex; gap:12px">
          <mat-form-field appearance="outline" style="flex:1">
            <mat-label>Search query</mat-label>
            <input matInput [(ngModel)]="q" name="q" placeholder="Enter keywords, phrases, case numbers…">
          </mat-form-field>
          <button mat-raised-button color="primary" type="submit" [disabled]="!q.trim()">
            <mat-icon>search</mat-icon> Search
          </button>
        </form>
      </div>

      @if (results(); as r) {
        <div class="card-section">
          <div class="muted">
            {{ r.totalHits }} matches — returned in {{ r.queryTimeMs }} ms
          </div>

          @if (r.classificationFacets?.length) {
            <div style="margin-top:12px"><strong>Classifications: </strong>
              <mat-chip-set>
                @for (b of r.classificationFacets; track b.key) {
                  <mat-chip>{{ b.key }} ({{ b.count }})</mat-chip>
                }
              </mat-chip-set>
            </div>
          }
          @if (r.typeFacets?.length) {
            <div style="margin-top:12px"><strong>Document types: </strong>
              <mat-chip-set>
                @for (b of r.typeFacets; track b.key) {
                  <mat-chip>{{ b.key }} ({{ b.count }})</mat-chip>
                }
              </mat-chip-set>
            </div>
          }
        </div>

        <div class="card-section">
          @if (r.hits.length === 0) {
            <p>No hits. Try broader terms.</p>
          } @else {
            @for (h of r.hits; track h.documentId) {
              <div class="search-hit">
                <div>
                  <a [routerLink]="['/cases', h.caseId]" class="link">{{ h.caseNumber }}</a>
                  · serial #{{ h.serialIndex }} · <span class="badge">{{ h.documentType }}</span>
                </div>
                <div style="font-weight:500; margin-top:4px">{{ h.title }}</div>
                @if (h.snippet) {
                  <div style="color:#4b5563; margin-top:4px" [innerHTML]="h.snippet"></div>
                }
                <div class="muted" style="margin-top:4px; font-size:0.8rem">
                  score {{ h.score | number:'1.2-2' }}
                </div>
              </div>
            }
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .link  { color:#1d4ed8; text-decoration:none; font-weight:500; }
    .link:hover { text-decoration: underline; }
    .muted { color:#6b7280; }
  `]
})
export class SearchComponent {
  private api = inject(ApiService);
  q = '';
  results = signal<SearchResponse | null>(null);

  run() {
    if (!this.q.trim()) return;
    this.api.search({ query: this.q, page: 0, size: 25 })
      .subscribe(r => this.results.set(r));
  }
}
