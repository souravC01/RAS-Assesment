package ca.ras.safety.submission;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
public class SubmissionDtos {
    public enum Answer { PASS,ISSUE,NA }
    public record Checklist(@NotNull Answer hardHat,@NotNull Answer highVisibilityVest,
        @NotNull Answer safetyBoots,@NotNull Answer eyeProtection,@NotNull Answer fallProtection,
        @NotNull Answer laddersScaffolds,@NotNull Answer toolsCords,@NotNull Answer hazardsControlled) {
        public boolean hasIssue() { return List.of(hardHat,highVisibilityVest,safetyBoots,eyeProtection,
            fallProtection,laddersScaffolds,toolsCords,hazardsControlled).contains(Answer.ISSUE); }
    }
    @JsonIgnoreProperties(ignoreUnknown=true)
    public record CreateSubmission(@NotNull Long siteId,@NotNull LocalDate workDate,
        @NotNull @Valid Checklist checklist,@Size(max=4000) String notes) {}
}
