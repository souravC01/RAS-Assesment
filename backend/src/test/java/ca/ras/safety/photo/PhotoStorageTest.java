package ca.ras.safety.photo;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "STORAGE_SMOKE", matches = "true")
class PhotoStorageTest {
    @Test void privateObjectRoundTrip() throws Exception {
        String endpoint = System.getenv("AWS_ENDPOINT_URL_S3");
        String bucket = "images";
        String key = "probe/" + UUID.randomUUID() + ".jpg";
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "jpeg", bytes);
        byte[] uploadedBytes = bytes.toByteArray();
        var storage = new PhotoStorage(endpoint, System.getenv("AWS_REGION"), bucket,
            System.getenv("AWS_ACCESS_KEY_ID"), System.getenv("AWS_SECRET_ACCESS_KEY"));
        boolean uploaded = false;
        try (var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()) {
            storage.put(key, uploadedBytes, "image/jpeg");
            uploaded = true;
            var unsigned = http.send(HttpRequest.newBuilder(URI.create(endpoint + "/" + bucket + "/" + key))
                .timeout(Duration.ofSeconds(30)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            assertThat(unsigned.statusCode()).isIn(401, 403, 404);
            var signed = http.send(HttpRequest.newBuilder(storage.presign(key, Duration.ofMinutes(5)))
                .timeout(Duration.ofSeconds(30)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            assertThat(signed.statusCode()).isEqualTo(200);
            assertThat(signed.body()).isEqualTo(uploadedBytes);
        } finally {
            try { if (uploaded) storage.delete(key); }
            finally { storage.close(); }
        }
    }
}
