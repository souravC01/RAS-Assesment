package ca.ras.safety.submission;
import ca.ras.safety.ApiErrors.Failure;
import ca.ras.safety.auth.UserRepository;
import ca.ras.safety.submission.SubmissionDtos.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import ca.ras.safety.auth.User;
@RestController
public class SubmissionController {
    private final SubmissionService submissions;
    private final UserRepository users;
    public SubmissionController(SubmissionService submissions,UserRepository users) { this.submissions=submissions;this.users=users; }
    @PostMapping("/api/submissions") @ResponseStatus(HttpStatus.CREATED)
    Map<String,Long> create(@RequestPart("form") @Valid CreateSubmission form,
        @RequestPart(value="photos",required=false) List<MultipartFile> photos,
        @RequestHeader(value="X-Expected-Actor",required=false) Long expectedActor,Authentication auth) {
        var signedIn=actor(auth);
        if(expectedActor==null) throw new Failure(400,"Refresh the page before starting a safety form.");
        if(!signedIn.getId().equals(expectedActor))
            throw new Failure(412,"Your account changed. Start a new form under the current account.");
        return Map.of("id",submissions.create(signedIn,form,photos));
    }
    @GetMapping("/api/submissions") List<SubmissionRow> history(Authentication auth) { return submissions.history(actor(auth)); }
    @GetMapping("/api/submissions/{id}") SubmissionDetail detail(@PathVariable Long id,Authentication auth) { return submissions.detail(actor(auth),id); }
    @GetMapping("/api/admin/workers") List<Reference> workers() {
        return users.findByRoleOrderByNameAsc(User.Role.FRAMER).stream().map(u->new Reference(u.getId(),u.getName())).toList();
    }
    @GetMapping("/api/admin/submissions") AdminResult search(@RequestParam(required=false) Long siteId,
        @RequestParam(required=false) Long workerId,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to) {
        return submissions.search(new SubmissionFilter(siteId,workerId,from,to));
    }
    private User actor(Authentication auth) { return users.findByEmail(auth.getName()).orElseThrow(()->new Failure(401,"Please sign in.")); }
}
