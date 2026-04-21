package gov.fbi.casemgmt.workflow;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Long-running workflow that orchestrates a case file's lifecycle from DRAFT
 * through CLOSED. Signals drive transitions; the workflow calls activities to
 * persist the corresponding status change.
 */
@WorkflowInterface
public interface CaseLifecycleWorkflow {

    /** The main workflow loop. Blocks on signals until the case is closed. */
    @WorkflowMethod
    void manageLifecycle(String caseId);

    /** Agent requests supervisor approval to open the case. */
    @SignalMethod
    void requestApproval(String comments);

    /** Supervisor approves — case moves to OPEN. */
    @SignalMethod
    void approve(String comments);

    /** Supervisor rejects — case returns to DRAFT. */
    @SignalMethod
    void reject(String comments);

    /** Agent requests closure — case moves to CLOSURE_REVIEW. */
    @SignalMethod
    void requestClosure(String reason);

    /** Supervisor confirms closure — case moves to CLOSED. */
    @SignalMethod
    void confirmClosure(String reason);

    /** Supervisor suspends the case (e.g., pending court action). */
    @SignalMethod
    void suspend(String reason);

    /** Resume a suspended case. */
    @SignalMethod
    void resume(String reason);

    /** Report the current phase. */
    @QueryMethod
    String currentPhase();
}
