package gov.fbi.casemgmt.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.fbi.casemgmt.dto.DocumentDtos;
import gov.fbi.casemgmt.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Serial documents filed into cases")
@SecurityRequirement(name = "bearer-jwt")
public class DocumentController {

    private final DocumentService docs;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/cases/{caseId}/documents",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('AGENT', 'SUPERVISOR', 'RECORDS_CLERK', 'SYSTEM_ADMIN')")
    @Operation(summary = "Upload a serial document into a case")
    public ResponseEntity<DocumentDtos.DocumentDetail> upload(
            @PathVariable UUID caseId,
            @RequestPart("metadata") String metadataJson,
            @RequestPart("file") MultipartFile file) throws IOException {
        DocumentDtos.UploadDocumentRequest req =
            objectMapper.readValue(metadataJson, DocumentDtos.UploadDocumentRequest.class);
        DocumentDtos.DocumentDetail out = docs.upload(caseId, req, file);
        return ResponseEntity
            .created(URI.create("/api/v1/documents/" + out.id()))
            .body(out);
    }

    @GetMapping("/documents/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Fetch document metadata")
    public DocumentDtos.DocumentDetail get(@PathVariable UUID id) {
        return docs.find(id);
    }

    @GetMapping("/documents/{id}/download-url")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Issue a short-lived pre-signed S3 download URL")
    public DocumentDtos.DownloadUrlResponse downloadUrl(@PathVariable UUID id) {
        return docs.getDownloadUrl(id);
    }
}
