package gov.fbi.casemgmt.audit;

import gov.fbi.casemgmt.model.AuditEvent;
import gov.fbi.casemgmt.repository.AuditEventRepository;
import gov.fbi.casemgmt.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

/**
 * Writes immutable audit events to the primary DB. Each call runs in a
 * REQUIRES_NEW transaction so audit rows are durably recorded even if the
 * caller's transaction later rolls back.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditEventRepository repo;
    private final ObjectProvider<HttpServletRequest> requestProvider;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String entityType, String entityId,
                       String caseNumber, String outcome, Map<String, Object> details) {
        AuditEvent event = AuditEvent.builder()
            .occurredAt(Instant.now())
            .actorUsername(SecurityUtils.currentUsername().orElse("system"))
            .actorRoles(String.join(",", SecurityUtils.currentRoles()))
            .actorIp(resolveIp())
            .action(action)
            .entityType(entityType)
            .entityId(entityId)
            .caseNumber(caseNumber)
            .outcome(outcome)
            .details(details)
            .requestId(MDC.get("requestId"))
            .build();
        repo.save(event);
        log.info("audit: {} {} {} by {}", action, entityType, entityId, event.getActorUsername());
    }

    private String resolveIp() {
        try {
            HttpServletRequest req = requestProvider.getIfAvailable();
            if (req == null) return null;
            String xff = req.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
            return req.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
