package ca.ras.safety.photo;

import java.net.URI;
import java.time.Duration;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Component
public class PhotoStorage implements AutoCloseable {
    private final String bucket;
    private final S3Client client;
    private final S3Presigner signer;
    public PhotoStorage(@Value("${S3_ENDPOINT:}") String endpoint,
        @Value("${S3_REGION:us-east-2}") String region,
        @Value("${S3_BUCKET:images}") String bucket,
        @Value("${S3_ACCESS_KEY_ID:}") String accessKey,
        @Value("${S3_SECRET_ACCESS_KEY:}") String secretKey) {
        this.bucket = bucket;
        // Login can start before storage credentials are configured; uploads cannot.
        if (endpoint.isBlank()) { client = null; signer = null; return; }
        var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        client = S3Client.builder().endpointOverride(URI.create(endpoint)).region(Region.of(region))
            .credentialsProvider(credentials).forcePathStyle(true)
            .overrideConfiguration(c -> c.apiCallTimeout(Duration.ofSeconds(45))
                .apiCallAttemptTimeout(Duration.ofSeconds(20))).build();
        signer = S3Presigner.builder().endpointOverride(URI.create(endpoint)).region(Region.of(region))
            .credentialsProvider(credentials)
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build()).build();
    }
    public void put(String key, byte[] bytes, String contentType) {
        requireConfigured();
        client.putObject(r -> r.bucket(bucket).key(key).contentType(contentType)
            .cacheControl("private, no-store"), RequestBody.fromBytes(bytes));
    }
    public void delete(String key) {
        requireConfigured();
        client.deleteObject(r -> r.bucket(bucket).key(key));
    }
    public URI presign(String key, Duration ttl) {
        requireConfigured();
        return URI.create(signer.presignGetObject(r -> r.signatureDuration(ttl)
            .getObjectRequest(g -> g.bucket(bucket).key(key))).url().toString());
    }
    private void requireConfigured() {
        if (client == null) throw new IllegalStateException("Photo storage is not configured.");
    }
    @Override @PreDestroy public void close() {
        if (client != null) client.close();
        if (signer != null) signer.close();
    }
}
