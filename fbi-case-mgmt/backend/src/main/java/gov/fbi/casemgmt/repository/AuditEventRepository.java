package gov.fbi.casemgmt.repository;

import gov.fbi.casemgmt.model.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditEventRepository
        extends JpaRepository<AuditEvent, UUID>, JpaSpecificationExecutor<AuditEvent> {

    Page<AuditEvent> findByCaseNumberOrderByOccurredAtDesc(String caseNumber, Pageable pageable);

    Page<AuditEvent> findByActorUsernameOrderByOccurredAtDesc(String actor, Pageable pageable);
}
