package gov.fbi.casemgmt.workflow.activity;

import gov.fbi.casemgmt.model.CaseStatus;
import gov.fbi.casemgmt.service.CaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Spring-managed activity bean. {@code @Lazy} on the CaseService constructor
 * parameter breaks the dependency cycle that otherwise exists between
 *   CaseService -> WorkflowClient -> WorkerFactory registration -> this bean.
 * It must be a constructor-injected @Lazy (not a @Lazy field on a Lombok
 * @RequiredArgsConstructor) because Lombok does not forward field annotations
 * to generated constructor parameters.
 */
@Component
@Slf4j
public class CaseLifecycleActivitiesImpl implements CaseLifecycleActivities {

    private final CaseService caseService;

    public CaseLifecycleActivitiesImpl(@Lazy CaseService caseService) {
        this.caseService = caseService;
    }

    @Override
    public void applyStatus(String caseId, CaseStatus newStatus, String reason) {
        log.info("activity.applyStatus case={} status={} reason={}", caseId, newStatus, reason);
        caseService.applyStatus(UUID.fromString(caseId), newStatus, reason);
    }
}
