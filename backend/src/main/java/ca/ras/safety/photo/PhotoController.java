package ca.ras.safety.photo;
import ca.ras.safety.ApiErrors.Failure;
import ca.ras.safety.auth.User;
import ca.ras.safety.auth.UserRepository;
import java.net.URI;
import java.time.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
@RestController
public class PhotoController {
    private final PhotoRepository photos;private final UserRepository users;private final PhotoStorage storage;private final Clock clock;
    public PhotoController(PhotoRepository photos,UserRepository users,PhotoStorage storage,Clock clock) {
        this.photos=photos;this.users=users;this.storage=storage;this.clock=clock;
    }
    public record PhotoUrl(URI url,Instant expiresAt) {}
    @GetMapping("/api/photos/{id}/url") @Transactional(readOnly=true)
    PhotoUrl url(@PathVariable Long id,Authentication auth) {
        var actor=users.findByEmail(auth.getName()).orElseThrow(()->new Failure(401,"Please sign in."));
        var photo=(actor.getRole()==User.Role.ADMIN?photos.findById(id):photos.findByIdAndSubmissionWorkerId(id,actor.getId()))
            .orElseThrow(()->new Failure(404,"Photo not found."));
        var ttl=Duration.ofMinutes(5);
        try {return new PhotoUrl(storage.presign(photo.getStorageKey(),ttl),clock.instant().plus(ttl));}
        catch(RuntimeException ex) {throw new Failure(503,"The photo is temporarily unavailable. Please try again.");}
    }
}
