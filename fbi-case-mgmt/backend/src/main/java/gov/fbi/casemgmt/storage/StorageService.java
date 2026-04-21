package gov.fbi.casemgmt.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Thin wrapper over S3 for document binary storage. Generates S3 keys of the
 * form {@code cases/{caseId}/{documentId}/{filename}} so objects stay grouped
 * by case for access-policy scoping.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final S3Client s3;
    private final S3Presigner presigner;

    @Value("${app.s3.bucket}")
    private String bucket;

    @Value("${app.s3.presigned-url-ttl-seconds:300}")
    private long presignTtlSec;

    public record StoredObject(String key, long size, String sha256) {}

    /** Upload bytes and return the key + sha256. */
    public StoredObject put(UUID caseId, UUID documentId, String filename,
                            String contentType, byte[] bytes) {
        String key = buildKey(caseId, documentId, filename);
        String sha256 = sha256Hex(bytes);

        PutObjectRequest put = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .contentLength((long) bytes.length)
            .serverSideEncryption(ServerSideEncryption.AES256)
            .metadata(Map.of(
                "case-id",     caseId.toString(),
                "document-id", documentId.toString(),
                "sha256",      sha256
            ))
            .build();
        s3.putObject(put, RequestBody.fromBytes(bytes));
        log.info("s3.put bucket={} key={} size={} sha256={}", bucket, key, bytes.length, sha256);
        return new StoredObject(key, bytes.length, sha256);
    }

    /** Download bytes for internal processing (OCR, conversion). */
    public byte[] get(String key) {
        try (var resp = s3.getObject(GetObjectRequest.builder()
                .bucket(bucket).key(key).build())) {
            return resp.readAllBytes();
        } catch (IOException e) {
            throw new StorageException("Failed to read s3://" + bucket + "/" + key, e);
        }
    }

    public InputStream stream(String key) {
        return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
    }

    /** Store a derived artifact (converted PDF, OCR text) alongside the original. */
    public String putDerived(UUID caseId, UUID documentId, String suffix,
                             String contentType, byte[] bytes) {
        String key = String.format("cases/%s/%s/derived/%s", caseId, documentId, suffix);
        s3.putObject(PutObjectRequest.builder()
                .bucket(bucket).key(key)
                .contentType(contentType)
                .serverSideEncryption(ServerSideEncryption.AES256)
                .build(),
            RequestBody.fromBytes(bytes));
        return key;
    }

    /** Time-limited URL a browser can use to download directly from S3. */
    public PresignResult presignDownload(String key, String filename) {
        GetObjectRequest get = GetObjectRequest.builder()
            .bucket(bucket).key(key)
            .responseContentDisposition("attachment; filename=\"" + sanitize(filename) + "\"")
            .build();
        var req = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(presignTtlSec))
            .getObjectRequest(get).build();
        var signed = presigner.presignGetObject(req);
        return new PresignResult(signed.url().toString(),
                Instant.now().plusSeconds(presignTtlSec));
    }

    public record PresignResult(String url, Instant expiresAt) {}

    public void delete(String key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    private static String buildKey(UUID caseId, UUID documentId, String filename) {
        return "cases/%s/%s/%s".formatted(caseId, documentId, sanitize(filename));
    }

    private static String sanitize(String filename) {
        if (filename == null || filename.isBlank()) return "document";
        return filename.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (Exception e) {
            throw new StorageException("SHA-256 unavailable", e);
        }
    }

    public static class StorageException extends RuntimeException {
        public StorageException(String m, Throwable t) { super(m, t); }
    }
}
