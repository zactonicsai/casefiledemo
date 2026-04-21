package gov.fbi.casemgmt.service;

import gov.fbi.casemgmt.audit.AuditService;
import gov.fbi.casemgmt.dto.CaseDtos;
import gov.fbi.casemgmt.dto.CaseMapper;
import gov.fbi.casemgmt.exception.InvalidStateException;
import gov.fbi.casemgmt.exception.NotFoundException;
import gov.fbi.casemgmt.model.*;
import gov.fbi.casemgmt.repository.CaseRepository;
import gov.fbi.casemgmt.repository.CaseSerialSequenceRepository;
import gov.fbi.casemgmt.security.SecurityUtils;
import gov.fbi.casemgmt.workflow.CaseLifecycleWorkflow;
import gov.fbi.casemgmt.config.TemporalConfig;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseService {

    private final CaseRepository caseRepo;
    private final CaseSerialSequenceRepository serialRepo;
    private final CaseMapper mapper;
    private final AuditService audit;
    private final WorkflowClient workflowClient;

    /** Create a new case in DRAFT state with auto-generated case number. */
    @Transactional
    public CaseDtos.CaseDetail create(CaseDtos.CreateCaseRequest req) {
        // Validate classification exists in our enum.
        CaseClassification.fromCode(req.classificationCode());

        long serial = serialRepo.nextSerial(req.classificationCode(), req.originatingOffice());
        String caseNumber = "%s-%s-%d".formatted(
            req.classificationCode(), req.originatingOffice(), serial);

        Case c = Case.builder()
            .caseNumber(caseNumber)
            .classificationCode(req.classificationCode())
            .originatingOffice(req.originatingOffice())
            .serialNumber(serial)
            .title(req.title())
            .synopsis(req.synopsis())
            .status(CaseStatus.DRAFT)
            .tags(req.tags() == null ? new HashSet<>() : new HashSet<>(req.tags()))
            .metadata(req.metadata() == null ? new HashMap<>() : new HashMap<>(req.metadata()))
            .build();

        // Agent who creates the case gets self-assigned.
        String username = SecurityUtils.currentUsername().orElse("system");

        Case saved = caseRepo.save(c);

        // Start the long-running Temporal workflow that orchestrates this case's
        // full lifecycle — approvals, closure reviews, reminders.
        String workflowId = "case-" + saved.getId();
        CaseLifecycleWorkflow wf = workflowClient.newWorkflowStub(
            CaseLifecycleWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(TemporalConfig.CASE_LIFECYCLE_QUEUE)
                .setWorkflowId(workflowId)
                .setWorkflowExecutionTimeout(Duration.ofDays(3650))
                .build());
        WorkflowClient.start(wf::manageLifecycle, saved.getId().toString());

        saved.setWorkflowId(workflowId);
        saved = caseRepo.save(saved);

        audit.record("CREATE_CASE", "Case", saved.getId().toString(),
            saved.getCaseNumber(), "DRAFT created",
            Map.of("title", saved.getTitle(), "by", username));

        return mapper.toDetail(saved);
    }

    @Transactional(readOnly = true)
    public CaseDtos.CaseDetail findDetail(UUID id) {
        Case c = caseRepo.findById(id)
            .orElseThrow(() -> new NotFoundException("Case not found: " + id));
        audit.record("VIEW_CASE", "Case", c.getId().toString(),
            c.getCaseNumber(), "detail viewed", Map.of());
        return mapper.toDetail(c);
    }

    @Transactional(readOnly = true)
    public CaseDtos.CaseDetail findByCaseNumber(String caseNumber) {
        Case c = caseRepo.findByCaseNumber(caseNumber)
            .orElseThrow(() -> new NotFoundException("Case not found: " + caseNumber));
        audit.record("VIEW_CASE", "Case", c.getId().toString(),
            c.getCaseNumber(), "detail viewed", Map.of());
        return mapper.toDetail(c);
    }

    @Transactional(readOnly = true)
    public Page<CaseDtos.CaseSummary> list(CaseStatus status, Pageable pageable) {
        Page<Case> p = status == null ? caseRepo.findAll(pageable)
                                      : caseRepo.findByStatus(status, pageable);
        return p.map(mapper::toSummary);
    }

    @Transactional
    public CaseDtos.CaseDetail update(UUID id, CaseDtos.UpdateCaseRequest req) {
        Case c = caseRepo.findById(id).orElseThrow(() ->
            new NotFoundException("Case not found: " + id));

        if (c.getStatus() == CaseStatus.CLOSED || c.getStatus() == CaseStatus.ARCHIVED) {
            throw new InvalidStateException("Closed/archived cases cannot be modified");
        }

        if (req.title() != null)            c.setTitle(req.title());
        if (req.synopsis() != null)         c.setSynopsis(req.synopsis());
        if (req.tags() != null)             c.setTags(new HashSet<>(req.tags()));
        if (req.metadata() != null)         c.setMetadata(new HashMap<>(req.metadata()));
        if (req.assignedAgentId() != null)  c.setAssignedAgentId(req.assignedAgentId());
        if (req.supervisorId() != null)     c.setSupervisorId(req.supervisorId());

        audit.record("UPDATE_CASE", "Case", c.getId().toString(),
            c.getCaseNumber(), "fields updated", Map.of());
        return mapper.toDetail(c);
    }

    /** Agent signals the workflow to submit case for supervisor approval. */
    @Transactional
    public void submitForApproval(UUID id, String comments) {
        Case c = requireState(id, CaseStatus.DRAFT);
        sendSignal(c, CaseLifecycleWorkflow::requestApproval, comments);
        audit.record("SUBMIT_FOR_APPROVAL", "Case", c.getId().toString(),
            c.getCaseNumber(), "pending approval", Map.of("comments", comments));
    }

    /** Supervisor approves — workflow transitions to OPEN. */
    @Transactional
    public void approve(UUID id, String comments) {
        Case c = requireState(id, CaseStatus.PENDING_APPROVAL);
        sendSignal(c, CaseLifecycleWorkflow::approve, comments);
        audit.record("APPROVE_CASE", "Case", c.getId().toString(),
            c.getCaseNumber(), "approved", Map.of("comments", comments));
    }

    @Transactional
    public void reject(UUID id, String comments) {
        Case c = requireState(id, CaseStatus.PENDING_APPROVAL);
        sendSignal(c, CaseLifecycleWorkflow::reject, comments);
        audit.record("REJECT_CASE", "Case", c.getId().toString(),
            c.getCaseNumber(), "rejected", Map.of("comments", comments));
    }

    @Transactional
    public void requestClosure(UUID id, String reason) {
        Case c = requireState(id, CaseStatus.OPEN);
        sendSignal(c, CaseLifecycleWorkflow::requestClosure, reason);
        audit.record("REQUEST_CLOSURE", "Case", c.getId().toString(),
            c.getCaseNumber(), "closure requested", Map.of("reason", reason));
    }

    @Transactional
    public void confirmClosure(UUID id, String reason) {
        Case c = requireState(id, CaseStatus.CLOSURE_REVIEW);
        sendSignal(c, CaseLifecycleWorkflow::confirmClosure, reason);
        audit.record("CONFIRM_CLOSURE", "Case", c.getId().toString(),
            c.getCaseNumber(), "closed", Map.of("reason", reason));
    }

    /** Applied by the workflow itself via CaseLifecycleActivities. */
    @Transactional
    public void applyStatus(UUID caseId, CaseStatus newStatus, String reason) {
        Case c = caseRepo.findById(caseId).orElseThrow(() ->
            new NotFoundException("Case not found: " + caseId));

        CaseStatus prev = c.getStatus();
        c.setStatus(newStatus);
        if (newStatus == CaseStatus.OPEN && c.getOpenedAt() == null) {
            c.setOpenedAt(Instant.now());
        }
        if (newStatus == CaseStatus.CLOSED) {
            c.setClosedAt(Instant.now());
            c.setClosureReason(reason);
        }
        log.info("case {} status {} -> {} reason={}", c.getCaseNumber(), prev, newStatus, reason);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Case requireState(UUID id, CaseStatus... allowed) {
        Case c = caseRepo.findById(id).orElseThrow(() ->
            new NotFoundException("Case not found: " + id));
        Set<CaseStatus> allowedSet = Set.of(allowed);
        if (!allowedSet.contains(c.getStatus())) {
            throw new InvalidStateException(
                "Case " + c.getCaseNumber() + " is " + c.getStatus()
                + "; required one of " + allowedSet);
        }
        return c;
    }

    @FunctionalInterface
    private interface SignalCall {
        void invoke(CaseLifecycleWorkflow wf, String comments);
    }

    private void sendSignal(Case c, SignalCall call, String comments) {
        if (c.getWorkflowId() == null) {
            throw new InvalidStateException("Case has no workflow attached");
        }
        CaseLifecycleWorkflow wf = workflowClient.newWorkflowStub(
            CaseLifecycleWorkflow.class, c.getWorkflowId());
        call.invoke(wf, comments == null ? "" : comments);
    }
}
