package gov.fbi.casemgmt.repository;

import gov.fbi.casemgmt.model.CaseApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CaseApprovalRepository extends JpaRepository<CaseApproval, UUID> {
    List<CaseApproval> findByCaseIdOrderByRequestedAtDesc(UUID caseId);
}
