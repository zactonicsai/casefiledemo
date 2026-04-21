package gov.fbi.casemgmt.workflow.impl;

import gov.fbi.casemgmt.model.CaseStatus;
import gov.fbi.casemgmt.workflow.CaseLifecycleWorkflow;
import gov.fbi.casemgmt.workflow.activity.CaseLifecycleActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

/**
 * Implementation of the case lifecycle. All mutable state is local to the
 * workflow (Temporal persists it in the workflow history) — no workflow code
 * should touch the database directly; only activities may.
 */
public class CaseLifecycleWorkflowImpl implements CaseLifecycleWorkflow {

    private final CaseLifecycleActivities activities = Workflow.newActivityStub(
        CaseLifecycleActivities.class,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .setRetryOptions(RetryOptions.newBuilder()
                .setInitialInterval(Duration.ofSeconds(1))
                .setMaximumInterval(Duration.ofMinutes(1))
                .setMaximumAttempts(5)
                .build())
            .build());

    private String caseId;
    private CaseStatus phase = CaseStatus.DRAFT;

    // Signal flags.
    private boolean approvalRequested;
    private boolean approved;
    private boolean rejected;
    private boolean closureRequested;
    private boolean closureConfirmed;
    private boolean suspendRequested;
    private boolean resumeRequested;

    private String lastComments = "";

    @Override
    public void manageLifecycle(String caseId) {
        this.caseId = caseId;

        // DRAFT: wait for approval request.
        Workflow.await(() -> approvalRequested || rejected);

        while (true) {
            if (approvalRequested) {
                phase = CaseStatus.PENDING_APPROVAL;
                activities.applyStatus(caseId, CaseStatus.PENDING_APPROVAL, lastComments);
                approvalRequested = false;

                Workflow.await(() -> approved || rejected);

                if (rejected) {
                    phase = CaseStatus.DRAFT;
                    activities.applyStatus(caseId, CaseStatus.DRAFT, lastComments);
                    rejected = false;
                    continue;                                                         // back to wait state
                }
                if (approved) {
                    phase = CaseStatus.OPEN;
                    activities.applyStatus(caseId, CaseStatus.OPEN, lastComments);
                    approved = false;
                }
            }

            // OPEN main loop: can be suspended, closed, etc.
            while (phase == CaseStatus.OPEN || phase == CaseStatus.SUSPENDED) {
                Workflow.await(() -> closureRequested || suspendRequested || resumeRequested);

                if (suspendRequested && phase == CaseStatus.OPEN) {
                    phase = CaseStatus.SUSPENDED;
                    activities.applyStatus(caseId, CaseStatus.SUSPENDED, lastComments);
                    suspendRequested = false;
                    continue;
                }
                if (resumeRequested && phase == CaseStatus.SUSPENDED) {
                    phase = CaseStatus.OPEN;
                    activities.applyStatus(caseId, CaseStatus.OPEN, lastComments);
                    resumeRequested = false;
                    continue;
                }
                if (closureRequested) {
                    phase = CaseStatus.CLOSURE_REVIEW;
                    activities.applyStatus(caseId, CaseStatus.CLOSURE_REVIEW, lastComments);
                    closureRequested = false;

                    Workflow.await(() -> closureConfirmed);
                    phase = CaseStatus.CLOSED;
                    activities.applyStatus(caseId, CaseStatus.CLOSED, lastComments);
                    closureConfirmed = false;
                    return;                                                           // workflow complete
                }
            }
        }
    }

    @Override public void requestApproval(String comments)  { lastComments = comments; approvalRequested = true; }
    @Override public void approve(String comments)          { lastComments = comments; approved          = true; }
    @Override public void reject(String comments)           { lastComments = comments; rejected          = true; }
    @Override public void requestClosure(String reason)     { lastComments = reason;   closureRequested  = true; }
    @Override public void confirmClosure(String reason)     { lastComments = reason;   closureConfirmed  = true; }
    @Override public void suspend(String reason)            { lastComments = reason;   suspendRequested  = true; }
    @Override public void resume(String reason)             { lastComments = reason;   resumeRequested   = true; }

    @Override public String currentPhase() { return phase.name(); }
}
