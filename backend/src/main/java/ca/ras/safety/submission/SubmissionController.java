package ca.ras.safety.submission;
import ca.ras.safety.ApiErrors.Failure;
import ca.ras.safety.auth.UserRepository;
import ca.ras.safety.submission.SubmissionDtos.CreateSubmission;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("/api/submissions")
public class SubmissionController {
    private final SubmissionService submissions;
    private final UserRepository users;
    public SubmissionController(SubmissionService submissions,UserRepository users) { this.submissions=submissions;this.users=users; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    Map<String,Long> create(@RequestPart("form") @Valid CreateSubmission form,
        @RequestPart(value="photos",required=false) List<MultipartFile> photos,Authentication auth) {
        var actor=users.findByEmail(auth.getName()).orElseThrow(()->new Failure(401,"Please sign in."));
        return Map.of("id",submissions.create(actor,form,photos));
    }
}
