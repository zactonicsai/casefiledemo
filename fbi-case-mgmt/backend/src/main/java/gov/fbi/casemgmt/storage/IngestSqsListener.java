package gov.fbi.casemgmt.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.fbi.casemgmt.config.TemporalConfig;
import gov.fbi.casemgmt.workflow.DocumentIngestWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.time.Duration;
import java.util.List;

/**
 * Long-polling SQS listener. On each message, starts a {@link DocumentIngestWorkflow}
 * in Temporal to orchestrate conversion + OCR + indexing.
 *
 * <p>Runs a single consumer thread — for production scale, replace with the AWS
 * SDK async consumer or spring-cloud-aws-messaging. Kept simple here for clarity.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IngestSqsListener {

    private final SqsClient sqs;
    private final ObjectMapper objectMapper;
    private final WorkflowClient workflowClient;

    @Value("${app.sqs.ingest-queue}")
    private String queueName;

    @Value("${app.sqs.max-poll:10}")
    private int maxPoll;

    private volatile boolean running = true;
    private String queueUrl;

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void start() {
        try {
            queueUrl = sqs.getQueueUrl(GetQueueUrlRequest.builder()
                .queueName(queueName).build()).queueUrl();
        } catch (Exception e) {
            log.warn("SQS ingest queue not available — listener paused: {}", e.getMessage());
            return;
        }
        log.info("SQS ingest listener started on {}", queueUrl);
        while (running) {
            try { pollOnce(); }
            catch (Exception e) {
                log.error("Ingest poll failure", e);
                sleep(5000);
            }
        }
    }

    private void pollOnce() {
        ReceiveMessageResponse resp = sqs.receiveMessage(ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(maxPoll)
            .waitTimeSeconds(20)
            .visibilityTimeout(300)
            .build());
        List<Message> messages = resp.messages();
        if (messages.isEmpty()) return;

        for (Message m : messages) {
            boolean handled = handle(m);
            if (handled) {
                sqs.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl).receiptHandle(m.receiptHandle()).build());
            }
        }
    }

    private boolean handle(Message m) {
        try {
            SqsPublisher.IngestMessage msg =
                objectMapper.readValue(m.body(), SqsPublisher.IngestMessage.class);

            String workflowId = "ingest-" + msg.documentId();
            DocumentIngestWorkflow wf = workflowClient.newWorkflowStub(
                DocumentIngestWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setTaskQueue(TemporalConfig.DOCUMENT_INGEST_QUEUE)
                    .setWorkflowId(workflowId)
                    .setWorkflowExecutionTimeout(Duration.ofHours(2))
                    .build());

            WorkflowClient.start(wf::ingest, msg);
            log.info("Started ingest workflow {} for document {}", workflowId, msg.documentId());
            return true;
        } catch (io.temporal.client.WorkflowExecutionAlreadyStarted already) {
            log.info("Ingest workflow already running for message {}", m.messageId());
            return true;
        } catch (Exception e) {
            log.error("Failed to handle ingest message {}: {}", m.messageId(), e.getMessage(), e);
            return false;
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
