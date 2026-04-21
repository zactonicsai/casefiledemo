package gov.fbi.casemgmt.workflow;

import gov.fbi.casemgmt.storage.SqsPublisher;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Orchestrates the post-upload processing pipeline for a single serial document:
 * <ol>
 *   <li>scan (placeholder for ClamAV integration)</li>
 *   <li>convert Office formats to PDF (LibreOffice)</li>
 *   <li>OCR images and PDFs (Tesseract)</li>
 *   <li>index text + metadata into Elasticsearch</li>
 * </ol>
 */
@WorkflowInterface
public interface DocumentIngestWorkflow {
    @WorkflowMethod
    void ingest(SqsPublisher.IngestMessage msg);
}
