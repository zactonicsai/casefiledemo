package gov.fbi.casemgmt.dto;

import gov.fbi.casemgmt.model.DocumentType;
import gov.fbi.casemgmt.model.SerialDocument;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class DocumentDtos {
    private DocumentDtos() {}

    public record UploadDocumentRequest(
        @NotNull DocumentType documentType,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 10_000) String description,
        Map<String, Object> metadata
    ) {}

    public record DocumentSummary(
        UUID         id,
        int          serialIndex,
        DocumentType documentType,
        String       title,
        String       originalFilename,
        String       contentType,
        Long         sizeBytes,
        SerialDocument.ProcessingStatus processingStatus,
        Instant      uploadedAt,
        String       uploadedBy
    ) {}

    public record DocumentDetail(
        UUID         id,
        UUID         caseId,
        String       caseNumber,
        int          serialIndex,
        DocumentType documentType,
        String       title,
        String       description,
        String       s3Key,
        String       pdfS3Key,
        String       originalFilename,
        String       contentType,
        Long         sizeBytes,
        String       sha256,
        SerialDocument.ProcessingStatus processingStatus,
        Integer      ocrPageCount,
        Map<String, Object> metadata,
        Instant      uploadedAt,
        String       uploadedBy,
        Instant      indexedAt
    ) {}

    public record DownloadUrlResponse(
        String url,
        Instant expiresAt
    ) {}
}
