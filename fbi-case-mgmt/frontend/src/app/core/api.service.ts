import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  AuditEvent, CaseDetail, CaseStatus, CaseSummary, CreateCaseRequest,
  DashboardStats, Page, SearchRequest, SearchResponse
} from './api.models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private base = environment.apiBase;

  constructor(private http: HttpClient) {}

  /* ─── Dashboard ───────────────────────────────────────── */
  dashboardStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.base}/dashboard/stats`);
  }

  /* ─── Cases ───────────────────────────────────────────── */
  listCases(status?: CaseStatus, page = 0, size = 25): Observable<Page<CaseSummary>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<Page<CaseSummary>>(`${this.base}/cases`, { params });
  }
  getCase(id: string): Observable<CaseDetail> {
    return this.http.get<CaseDetail>(`${this.base}/cases/${id}`);
  }
  createCase(req: CreateCaseRequest): Observable<CaseDetail> {
    return this.http.post<CaseDetail>(`${this.base}/cases`, req);
  }
  submitForApproval(id: string, comments: string) {
    return this.http.post(`${this.base}/cases/${id}/submit-for-approval`, { comments });
  }
  approve(id: string, comments: string) {
    return this.http.post(`${this.base}/cases/${id}/approve`, { comments });
  }
  reject(id: string, comments: string) {
    return this.http.post(`${this.base}/cases/${id}/reject`, { comments });
  }
  requestClosure(id: string, closureReason: string) {
    return this.http.post(`${this.base}/cases/${id}/request-closure`, { closureReason });
  }
  confirmClosure(id: string, closureReason: string) {
    return this.http.post(`${this.base}/cases/${id}/confirm-closure`, { closureReason });
  }

  /* ─── Documents ───────────────────────────────────────── */
  uploadDocument(caseId: string, metadata: unknown, file: File) {
    const form = new FormData();
    form.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }));
    form.append('file', file);
    return this.http.post(`${this.base}/cases/${caseId}/documents`, form);
  }
  getDownloadUrl(docId: string) {
    return this.http.get<{ url: string; expiresAt: string }>(
      `${this.base}/documents/${docId}/download-url`);
  }

  /* ─── Search ──────────────────────────────────────────── */
  search(req: SearchRequest): Observable<SearchResponse> {
    return this.http.post<SearchResponse>(`${this.base}/search`, req);
  }

  /* ─── Audit ───────────────────────────────────────────── */
  auditForCase(caseNumber: string): Observable<Page<AuditEvent>> {
    return this.http.get<Page<AuditEvent>>(
      `${this.base}/audit/case/${encodeURIComponent(caseNumber)}`);
  }
  auditAll(page = 0, size = 50): Observable<Page<AuditEvent>> {
    return this.http.get<Page<AuditEvent>>(`${this.base}/audit`,
      { params: new HttpParams().set('page', page).set('size', size) });
  }
}
