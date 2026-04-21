package gov.fbi.casemgmt.controller;

import gov.fbi.casemgmt.model.AuditEvent;
import gov.fbi.casemgmt.repository.AuditEventRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Immutable audit log (read-only)")
public class AuditController {

    private final AuditEventRepository repo;

    @GetMapping
    @PreAuthorize("hasAnyRole('AUDITOR', 'SYSTEM_ADMIN')")
    @Operation(summary = "Browse the audit log")
    public Page<AuditEvent> list(@PageableDefault(size = 50, sort = "occurredAt") Pageable pageable) {
        return repo.findAll(pageable);
    }

    @GetMapping("/case/{caseNumber}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Audit trail for a specific case")
    public Page<AuditEvent> forCase(@PathVariable String caseNumber,
                                    @PageableDefault(size = 100) Pageable pageable) {
        return repo.findByCaseNumberOrderByOccurredAtDesc(caseNumber, pageable);
    }

    @GetMapping("/user/{username}")
    @PreAuthorize("hasAnyRole('AUDITOR', 'SYSTEM_ADMIN')")
    @Operation(summary = "Audit trail for a specific user")
    public Page<AuditEvent> forUser(@PathVariable String username,
                                    @PageableDefault(size = 100) Pageable pageable) {
        return repo.findByActorUsernameOrderByOccurredAtDesc(username, pageable);
    }
}
