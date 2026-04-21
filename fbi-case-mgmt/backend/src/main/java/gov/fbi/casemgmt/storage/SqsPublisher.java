package gov.fbi.casemgmt.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Publishes ingest-pipeline events to SQS. Listeners live in
 * {@link gov.fbi.casemgmt.storage.IngestSqsListener}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SqsPublisher {

    private final SqsClient sqs;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, String> queueUrlCache = new ConcurrentHashMap<>();

    @Value("${app.sqs.ingest-queue}")
    private String ingestQueue;

    public void publishIngest(IngestMessage msg) {
        publish(ingestQueue, msg);
    }

    public <T> void publish(String queueName, T message) {
        String url = queueUrlCache.computeIfAbsent(queueName, this::resolveUrl);
        try {
            String body = objectMapper.writeValueAsString(message);
            sqs.sendMessage(SendMessageRequest.builder()
                .queueUrl(url)
                .messageBody(body)
                .build());
            log.debug("sqs.send queue={} body.len={}", queueName, body.length());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize SQS message", e);
        }
    }

    private String resolveUrl(String queueName) {
        return sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).queueUrl();
    }

    /** Ingest pipeline event — sent when a new document is uploaded. */
    public record IngestMessage(
        String caseId,
        String caseNumber,
        String documentId,
        String s3Key,
        String contentType,
        long   sizeBytes
    ) {}
}
