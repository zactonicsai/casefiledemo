package gov.fbi.casemgmt.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

/**
 * AWS SDK client beans. When {@code app.aws.endpoint} is set (e.g. LocalStack),
 * clients override the endpoint and force path-style S3 addressing.
 */
@Configuration
public class AwsConfig {

    @Value("${app.aws.region}")
    private String region;

    @Value("${app.aws.endpoint:}")
    private String endpoint;

    @Bean
    S3Client s3Client() {
        var builder = S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create());

        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                   .forcePathStyle(true)
                   .serviceConfiguration(S3Configuration.builder()
                       .pathStyleAccessEnabled(true).build());
        }
        return builder.build();
    }

    @Bean
    S3Presigner s3Presigner() {
        var builder = S3Presigner.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create());

        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                   .serviceConfiguration(S3Configuration.builder()
                       .pathStyleAccessEnabled(true).build());
        }
        return builder.build();
    }

    @Bean
    SqsClient sqsClient() {
        var builder = SqsClient.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create());

        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }
}
