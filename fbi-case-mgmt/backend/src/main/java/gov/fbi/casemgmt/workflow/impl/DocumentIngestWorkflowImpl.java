package gov.fbi.casemgmt.workflow.impl;

import gov.fbi.casemgmt.storage.SqsPublisher;
import gov.fbi.casemgmt.workflow.DocumentIngestWorkflow;
import gov.fbi.casemgmt.workflow.activity.DocumentIngestActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

public class DocumentIngestWorkflowImpl implements DocumentIngestWorkflow {

    private static final Logger log = Workflow.getLogger(DocumentIngestWorkflowImpl.class);

    private final DocumentIngestActivities activities = Workflow.newActivityStub(
        DocumentIngestActivities.class,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(10))
            .setHeartbeatTimeout(Duration.ofMinutes(2))
            .setRetryOptions(RetryOptions.newBuilder()
                .setInitialInterval(Duration.ofSeconds(2))
                .setMaximumInterval(Duration.ofMinutes(2))
                .setBackoffCoefficient(2.0)
                .setMaximumAttempts(3)
                .build())
            .build());

    @Override
    public void ingest(SqsPublisher.IngestMessage msg) {
        log.info("ingest start document={} case={}", msg.documentId(), msg.caseNumber());

        try {
            activities.markStatus(msg.documentId(), "VIRUS_SCANNING");
            activities.virusScan(msg.documentId(), msg.s3Key());

            activities.markStatus(msg.documentId(), "CONVERTING");
            String pdfKey = activities.convertToPdfIfNeeded(
                msg.documentId(), msg.s3Key(), msg.contentType());

            activities.markStatus(msg.documentId(), "OCR_PENDING");
            DocumentIngestActivities.ExtractionResult extraction =
                activities.extractText(msg.documentId(), msg.s3Key(),
                    pdfKey, msg.contentType());

            activities.markStatus(msg.documentId(), "OCR_COMPLETE");
            activities.indexDocument(msg.documentId(),
                extraction.text(), extraction.pageCount(), pdfKey);

            activities.markStatus(msg.documentId(), "INDEXED");
            log.info("ingest complete document={}", msg.documentId());
        } catch (Exception e) {
            log.error("ingest failed document={} cause={}", msg.documentId(), e.getMessage());
            activities.markFailed(msg.documentId(), e.getMessage());
            throw e;
        }
    }
}
