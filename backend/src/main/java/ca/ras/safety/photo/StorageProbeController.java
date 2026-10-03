package ca.ras.safety.photo;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController @Profile("storage-probe")
@RequestMapping("/api/admin/storage-probe")
public class StorageProbeController {
    private final PhotoStorage storage;
    public StorageProbeController(PhotoStorage storage) { this.storage = storage; }
    record ProbePhoto(String key, URI url) {}
    @PostMapping List<ProbePhoto> upload(@RequestParam(value="photos", required=false) List<MultipartFile> photos) throws IOException {
        if (photos == null || photos.isEmpty() || photos.size() > 5) badRequest("Provide one to five photos.");
        for (var photo : photos) {
            if (photo.getSize() > 5_000_000) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
            if (!List.of("image/jpeg", "image/png").contains(photo.getContentType())) badRequest("Use JPEG or PNG.");
            try (var input = ImageIO.createImageInputStream(photo.getInputStream())) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) badRequest("Invalid image.");
                var reader = readers.next();
                try {
                    reader.setInput(input);
                    String format = reader.getFormatName();
                    if (!(format.equalsIgnoreCase("jpeg") || format.equalsIgnoreCase("png"))) badRequest("Invalid format.");
                    if ((long) reader.getWidth(0) * reader.getHeight(0) > 25_000_000) badRequest("Image dimensions are too large.");
                    reader.read(0);
                } finally { reader.dispose(); }
            } catch (IOException ex) { badRequest("Invalid image."); }
        }
        var result = new ArrayList<ProbePhoto>();
        var keys = new ArrayList<String>();
        try {
            for (var photo : photos) {
                String key = "probe/" + UUID.randomUUID();
                keys.add(key);
                storage.put(key, photo.getBytes(), photo.getContentType());
                result.add(new ProbePhoto(key, storage.presign(key, Duration.ofMinutes(5))));
            }
            return result;
        } catch (Exception ex) {
            for (String key : keys) try { storage.delete(key); } catch (Exception cleanup) { ex.addSuppressed(cleanup); }
            throw ex;
        }
    }
    @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT)
    void cleanup(@RequestBody List<String> keys) {
        if (keys == null || keys.size() > 5 || keys.stream().anyMatch(k -> k == null || !k.matches("probe/[0-9a-f-]{36}")))
            badRequest("Only probe objects can be removed.");
        keys.forEach(storage::delete);
    }
    private static void badRequest(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
