package gov.fbi.casemgmt.service;

import gov.fbi.casemgmt.audit.AuditService;
import gov.fbi.casemgmt.dto.CaseMapper;
import gov.fbi.casemgmt.dto.DocumentDtos;
import gov.fbi.casemgmt.exception.InvalidStateException;
import gov.fbi.casemgmt.exception.NotFoundException;
import gov.fbi.casemgmt.model.Case;
import gov.fbi.casemgmt.model.CaseStatus;
import gov.fbi.casemgmt.model.SerialDocument;
import gov.fbi.casemgmt.repository.CaseRepository;
import gov.fbi.casemgmt.repository.SerialDocumentRepository;
import gov.fbi.casemgmt.storage.SqsPublisher;
import gov.fbi.casemgmt.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final CaseRepository caseRepo;
    private final SerialDocumentRepository docRepo;
    private final StorageService storage;
    private final SqsPublisher sqs;
    private final AuditService audit;
    private final CaseMapper mapper;

    private final Tika tika = new Tika();

    @Transactional
    public DocumentDtos.DocumentDetail upload(UUID caseId,
                                              DocumentDtos.UploadDocumentRequest req,
                                              MultipartFile file) throws IOException {
        Case c = caseRepo.findById(caseId)
            .orElseThrow(() -> new NotFoundException("Case not found: " + caseId));

        if (c.getStatus() == CaseStatus.CLOSED || c.getStatus() == CaseStatus.ARCHIVED) {
            throw new InvalidStateException("Cannot file documents into a closed case");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        byte[] bytes = file.getBytes();
        String contentType = resolveContentType(file, bytes);
        String originalName = file.getOriginalFilename();

        // Allocate the next serial index atomically using MAX+1.
        int next = Optional.ofNullable(docRepo.findMaxSerialIndex(caseId)).orElse(0) + 1;

        UUID docId = UUID.randomUUID();
        StorageService.StoredObject stored = storage.put(
            caseId, docId, originalName, contentType, bytes);

        SerialDocument doc = SerialDocument.builder()
            .id(docId)
            .caseFile(c)
            .serialIndex(next)
            .documentType(req.documentType())
            .title(req.title())
            .description(req.description())
            .s3Key(stored.key())
            .originalFilename(originalName)
            .contentType(contentType)
            .sizeBytes(stored.size())
            .sha256(stored.sha256())
            .processingStatus(SerialDocument.ProcessingStatus.UPLOADED)
            .metadata(req.metadata() == null ? new HashMap<>() : new HashMap<>(req.metadata()))
            .build();

        SerialDocument saved = docRepo.save(doc);

        // Fire pipeline event: conversion -> OCR -> index.
        sqs.publishIngest(new SqsPublisher.IngestMessage(
            caseId.toString(), c.getCaseNumber(), saved.getId().toString(),
            saved.getS3Key(), contentType, stored.size()));

        audit.record("UPLOAD_DOCUMENT", "SerialDocument", saved.getId().toString(),
            c.getCaseNumber(),
            "uploaded serial #" + next,
            Map.of(
                "type",     req.documentType().name(),
                "filename", Objects.toString(originalName, ""),
                "sha256",   stored.sha256(),
                "bytes",    stored.size()
            ));

        return mapper.toDocDetail(saved);
    }

    @Transactional(readOnly = true)
    public DocumentDtos.DocumentDetail find(UUID docId) {
        SerialDocument d = docRepo.findById(docId)
            .orElseThrow(() -> new NotFoundException("Document not found: " + docId));
        audit.record("VIEW_DOCUMENT", "SerialDocument", d.getId().toString(),
            d.getCaseFile().getCaseNumber(), "metadata viewed", Map.of());
        return mapper.toDocDetail(d);
    }

    @Transactional(readOnly = true)
    public DocumentDtos.DownloadUrlResponse getDownloadUrl(UUID docId) {
        SerialDocument d = docRepo.findById(docId)
            .orElseThrow(() -> new NotFoundException("Document not found: " + docId));

        StorageService.PresignResult r =
            storage.presignDownload(d.getS3Key(), d.getOriginalFilename());

        audit.record("DOWNLOAD_DOCUMENT", "SerialDocument", d.getId().toString(),
            d.getCaseFile().getCaseNumber(),
            "presigned URL issued",
            Map.of("expiresAt", r.expiresAt().toString()));

        return new DocumentDtos.DownloadUrlResponse(r.url(), r.expiresAt());
    }

    private String resolveContentType(MultipartFile file, byte[] bytes) {
        String ct = file.getContentType();
        if (ct == null || ct.isBlank() || "application/octet-stream".equals(ct)) {
            ct = tika.detect(bytes);
        }
        return ct;
    }
}
