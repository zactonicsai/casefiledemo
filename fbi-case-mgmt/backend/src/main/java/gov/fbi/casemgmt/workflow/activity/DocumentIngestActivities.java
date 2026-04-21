package gov.fbi.casemgmt.workflow.activity;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface DocumentIngestActivities {

    void markStatus(String documentId, String status);
    void markFailed(String documentId, String reason);

    void virusScan(String documentId, String s3Key);

    /** Returns S3 key of converted PDF, or the original key if already PDF. */
    String convertToPdfIfNeeded(String documentId, String s3Key, String contentType);

    ExtractionResult extractText(String documentId, String originalS3Key,
                                 String pdfS3Key, String contentType);

    void indexDocument(String documentId, String text, int pageCount, String pdfS3Key);

    record ExtractionResult(String text, int pageCount) {}
}
