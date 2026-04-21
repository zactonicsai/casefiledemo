package gov.fbi.casemgmt.workflow.activity;

import gov.fbi.casemgmt.model.SerialDocument;
import gov.fbi.casemgmt.repository.SerialDocumentRepository;
import gov.fbi.casemgmt.search.IndexedDocument;
import gov.fbi.casemgmt.search.SearchService;
import gov.fbi.casemgmt.ocr.ConversionService;
import gov.fbi.casemgmt.ocr.OcrService;
import gov.fbi.casemgmt.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentIngestActivitiesImpl implements DocumentIngestActivities {

    private final SerialDocumentRepository docRepo;
    private final StorageService storage;
    private final ConversionService conversion;
    private final OcrService ocr;
    private final SearchService search;

    @Override
    @Transactional
    public void markStatus(String documentId, String status) {
        docRepo.updateProcessingStatus(
            UUID.fromString(documentId),
            SerialDocument.ProcessingStatus.valueOf(status));
    }

    @Override
    @Transactional
    public void markFailed(String documentId, String reason) {
        docRepo.findById(UUID.fromString(documentId)).ifPresent(d -> {
            d.setProcessingStatus(SerialDocument.ProcessingStatus.FAILED);
            d.getMetadata().put("failure_reason", reason);
        });
    }

    @Override
    public void virusScan(String documentId, String s3Key) {
        // Placeholder. In production, shell out to ClamAV (clamdscan)
        // or call a dedicated scanning service. Throw to abort the pipeline
        // on an infected file.
        log.debug("virus-scan OK (stub) doc={} key={}", documentId, s3Key);
    }

    @Override
    public String convertToPdfIfNeeded(String documentId, String s3Key, String contentType) {
        if ("application/pdf".equals(contentType) || !conversion.isConvertible(contentType)) {
            return "application/pdf".equals(contentType) ? s3Key : null;
        }
        byte[] bytes = storage.get(s3Key);
        byte[] pdf;
        try {
            pdf = conversion.toPdf(extractName(s3Key), bytes);
        } catch (Exception e) {
            log.warn("Conversion failed for {} ({}): {}", documentId, contentType, e.getMessage());
            return null;
        }
        var docIdUuid = UUID.fromString(documentId);
        SerialDocument d = docRepo.findById(docIdUuid).orElseThrow();
        String pdfKey = storage.putDerived(
            d.getCaseFile().getId(), docIdUuid, "converted.pdf",
            "application/pdf", pdf);
        d.setPdfS3Key(pdfKey);
        docRepo.save(d);
        return pdfKey;
    }

    @Override
    public ExtractionResult extractText(String documentId, String originalS3Key,
                                        String pdfS3Key, String contentType) {
        String sourceKey = pdfS3Key != null ? pdfS3Key : originalS3Key;
        String sourceType = pdfS3Key != null ? "application/pdf" : contentType;
        byte[] bytes = storage.get(sourceKey);

        String text = "";
        int pages = 0;

        if ("application/pdf".equals(sourceType)) {
            text = conversion.extractPdfText(bytes);
            // If the embedded-text extraction yielded little, fall back to OCR.
            if (text == null || text.strip().length() < 40) {
                OcrService.OcrResult r = ocr.ocr("application/pdf", bytes);
                text = r.text();
                pages = r.pageCount();
            }
        } else if (sourceType != null && sourceType.startsWith("image/")) {
            OcrService.OcrResult r = ocr.ocr(sourceType, bytes);
            text = r.text();
            pages = r.pageCount();
        } else if (sourceType != null && sourceType.startsWith("text/")) {
            text = new String(bytes);
        }
        return new ExtractionResult(text == null ? "" : text, pages);
    }

    @Override
    @Transactional
    public void indexDocument(String documentId, String text, int pageCount, String pdfS3Key) {
        SerialDocument d = docRepo.findById(UUID.fromString(documentId)).orElseThrow();

        d.setExtractedText(text);
        d.setOcrPageCount(pageCount);
        d.setIndexedAt(Instant.now());
        if (pdfS3Key != null && d.getPdfS3Key() == null) d.setPdfS3Key(pdfS3Key);

        IndexedDocument idx = IndexedDocument.builder()
            .id(d.getId().toString())
            .caseId(d.getCaseFile().getId().toString())
            .caseNumber(d.getCaseFile().getCaseNumber())
            .serialIndex(d.getSerialIndex())
            .classificationCode(d.getCaseFile().getClassificationCode())
            .originatingOffice(d.getCaseFile().getOriginatingOffice())
            .caseStatus(d.getCaseFile().getStatus())
            .documentType(d.getDocumentType())
            .title(d.getTitle())
            .description(d.getDescription())
            .body(text)
            .tags(new ArrayList<>(d.getCaseFile().getTags()))
            .uploadedAt(d.getUploadedAt())
            .uploadedBy(d.getUploadedBy())
            .build();

        search.index(idx);
        log.info("indexed document {} chars={} pages={}",
            d.getId(), text == null ? 0 : text.length(), pageCount);
    }

    private String extractName(String s3Key) {
        int slash = s3Key.lastIndexOf('/');
        return slash < 0 ? s3Key : s3Key.substring(slash + 1);
    }
}
