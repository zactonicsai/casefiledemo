package gov.fbi.casemgmt.controller;

import gov.fbi.casemgmt.model.CaseStatus;
import gov.fbi.casemgmt.repository.CaseRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Case statistics and analytics")
public class DashboardController {

    private final CaseRepository caseRepo;

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Case counts by status for dashboard tiles")
    public DashboardStats stats() {
        Map<CaseStatus, Long> byStatus = new EnumMap<>(CaseStatus.class);
        for (CaseStatus s : CaseStatus.values()) byStatus.put(s, 0L);
        caseRepo.countByStatus().forEach(r -> byStatus.put(r.getStatus(), r.getCount()));

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long open  = byStatus.getOrDefault(CaseStatus.OPEN, 0L);
        long pend  = byStatus.getOrDefault(CaseStatus.PENDING_APPROVAL, 0L);
        long close = byStatus.getOrDefault(CaseStatus.CLOSED, 0L)
                   + byStatus.getOrDefault(CaseStatus.ARCHIVED, 0L);

        return new DashboardStats(total, open, pend, close, byStatus);
    }

    public record DashboardStats(
        long totalCases, long openCases, long pendingApproval, long closedCases,
        Map<CaseStatus, Long> byStatus
    ) {}
}
