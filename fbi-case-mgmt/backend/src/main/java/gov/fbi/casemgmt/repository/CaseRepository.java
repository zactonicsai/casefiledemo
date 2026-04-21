package gov.fbi.casemgmt.repository;

import gov.fbi.casemgmt.model.Case;
import gov.fbi.casemgmt.model.CaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaseRepository
        extends JpaRepository<Case, UUID>, JpaSpecificationExecutor<Case> {

    Optional<Case> findByCaseNumber(String caseNumber);

    boolean existsByCaseNumber(String caseNumber);

    Page<Case> findByStatus(CaseStatus status, Pageable pageable);

    Page<Case> findByAssignedAgentId(UUID agentId, Pageable pageable);

    @Query("""
           SELECT c.status AS status, COUNT(c) AS count
             FROM Case c
            GROUP BY c.status
           """)
    java.util.List<StatusCount> countByStatus();

    interface StatusCount {
        CaseStatus getStatus();
        long getCount();
    }
}
