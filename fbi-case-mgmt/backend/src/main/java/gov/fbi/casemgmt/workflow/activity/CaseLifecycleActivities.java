package gov.fbi.casemgmt.workflow.activity;

import gov.fbi.casemgmt.model.CaseStatus;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface CaseLifecycleActivities {

    void applyStatus(String caseId, CaseStatus newStatus, String reason);
}
