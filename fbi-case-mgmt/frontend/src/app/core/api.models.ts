export type CaseStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'OPEN' | 'SUSPENDED'
                       | 'CLOSURE_REVIEW' | 'CLOSED' | 'ARCHIVED';

export type DocumentType = 'FD_302' | 'EC' | 'LHM' | 'EVIDENCE_LOG' | 'PHOTO'
                         | 'NOTE' | 'AFFIDAVIT' | 'LEGAL_PROCESS' | 'OTHER';

export type ProcessingStatus = 'UPLOADED' | 'VIRUS_SCANNING' | 'CONVERTING'
                             | 'OCR_PENDING' | 'OCR_COMPLETE' | 'INDEXED' | 'FAILED';

export interface CaseSummary {
  id: string;
  caseNumber: string;
  classificationCode: string;
  originatingOffice: string;
  serialNumber: number;
  title: string;
  status: CaseStatus;
  assignedAgentId?: string;
  createdAt: string;
  openedAt?: string;
  documentCount: number;
}

export interface DocumentSummary {
  id: string;
  serialIndex: number;
  documentType: DocumentType;
  title: string;
  originalFilename?: string;
  contentType?: string;
  sizeBytes?: number;
  processingStatus: ProcessingStatus;
  uploadedAt: string;
  uploadedBy?: string;
}

export interface CaseDetail {
  id: string;
  caseNumber: string;
  classificationCode: string;
  originatingOffice: string;
  serialNumber: number;
  title: string;
  synopsis?: string;
  status: CaseStatus;
  assignedAgentId?: string;
  supervisorId?: string;
  tags: string[];
  metadata: Record<string, unknown>;
  workflowId?: string;
  createdAt: string;
  createdBy?: string;
  updatedAt?: string;
  updatedBy?: string;
  openedAt?: string;
  closedAt?: string;
  closureReason?: string;
  documents: DocumentSummary[];
}

export interface CreateCaseRequest {
  classificationCode: string;
  originatingOffice: string;
  title: string;
  synopsis?: string;
  tags?: string[];
  metadata?: Record<string, unknown>;
}

export interface SearchRequest {
  query: string;
  classifications?: string[];
  offices?: string[];
  statuses?: CaseStatus[];
  documentTypes?: DocumentType[];
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}

export interface SearchHit {
  documentId: string;
  caseId: string;
  caseNumber: string;
  serialIndex: number;
  documentType: DocumentType;
  title: string;
  snippet?: string;
  score: number;
}

export interface FacetBucket { key: string; count: number; }

export interface SearchResponse {
  totalHits: number;
  page: number;
  size: number;
  queryTimeMs: number;
  hits: SearchHit[];
  classificationFacets: FacetBucket[];
  officeFacets: FacetBucket[];
  typeFacets: FacetBucket[];
}

export interface DashboardStats {
  totalCases: number;
  openCases: number;
  pendingApproval: number;
  closedCases: number;
  byStatus: Record<CaseStatus, number>;
}

export interface AuditEvent {
  id: string;
  occurredAt: string;
  actorUsername: string;
  actorRoles?: string;
  action: string;
  entityType?: string;
  entityId?: string;
  caseNumber?: string;
  outcome?: string;
  details?: Record<string, unknown>;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
