package gov.fbi.casemgmt.controller;

import gov.fbi.casemgmt.dto.CaseDtos;
import gov.fbi.casemgmt.model.CaseStatus;
import gov.fbi.casemgmt.service.CaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cases")
@RequiredArgsConstructor
@Tag(name = "Cases", description = "Case file management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class CaseController {

    private final CaseService cases;

    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT', 'SUPERVISOR', 'SYSTEM_ADMIN')")
    @Operation(summary = "Create a new case in DRAFT status")
    public ResponseEntity<CaseDtos.CaseDetail> create(
            @Valid @RequestBody CaseDtos.CreateCaseRequest req) {
        CaseDtos.CaseDetail created = cases.create(req);
        return ResponseEntity
            .created(URI.create("/api/v1/cases/" + created.id()))
            .body(created);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List cases (optionally filtered by status)")
    public Page<CaseDtos.CaseSummary> list(
            @RequestParam(required = false) CaseStatus status,
            @PageableDefault(size = 25, sort = "createdAt") Pageable pageable) {
        return cases.list(status, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a case by ID")
    public CaseDtos.CaseDetail get(@PathVariable UUID id) {
        return cases.findDetail(id);
    }

    @GetMapping("/by-number/{caseNumber}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a case by its FBI-style case number")
    public CaseDtos.CaseDetail getByNumber(@PathVariable String caseNumber) {
        return cases.findByCaseNumber(caseNumber);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPERVISOR', 'SYSTEM_ADMIN')")
    @Operation(summary = "Update case metadata")
    public CaseDtos.CaseDetail update(
            @PathVariable UUID id,
            @Valid @RequestBody CaseDtos.UpdateCaseRequest req) {
        return cases.update(id, req);
    }

    @PostMapping("/{id}/submit-for-approval")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPERVISOR', 'SYSTEM_ADMIN')")
    @Operation(summary = "Agent submits a DRAFT case for supervisor approval")
    public ResponseEntity<Void> submit(@PathVariable UUID id,
                                       @Valid @RequestBody CaseDtos.TransitionRequest req) {
        cases.submitForApproval(id, req.comments());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'SYSTEM_ADMIN')")
    @Operation(summary = "Supervisor approves a pending case → OPEN")
    public ResponseEntity<Void> approve(@PathVariable UUID id,
                                        @Valid @RequestBody CaseDtos.TransitionRequest req) {
        cases.approve(id, req.comments());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'SYSTEM_ADMIN')")
    @Operation(summary = "Supervisor rejects a pending case → DRAFT")
    public ResponseEntity<Void> reject(@PathVariable UUID id,
                                       @Valid @RequestBody CaseDtos.TransitionRequest req) {
        cases.reject(id, req.comments());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/request-closure")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPERVISOR', 'SYSTEM_ADMIN')")
    @Operation(summary = "Request case closure → CLOSURE_REVIEW")
    public ResponseEntity<Void> requestClosure(@PathVariable UUID id,
                                               @Valid @RequestBody CaseDtos.CloseCaseRequest req) {
        cases.requestClosure(id, req.closureReason());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/confirm-closure")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'SYSTEM_ADMIN')")
    @Operation(summary = "Confirm case closure → CLOSED")
    public ResponseEntity<Void> confirmClosure(@PathVariable UUID id,
                                               @Valid @RequestBody CaseDtos.CloseCaseRequest req) {
        cases.confirmClosure(id, req.closureReason());
        return ResponseEntity.accepted().build();
    }
}
