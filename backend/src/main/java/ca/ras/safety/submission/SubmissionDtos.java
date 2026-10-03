package ca.ras.safety.submission;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.Instant;
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
    public record Reference(Long id,String name) {}
    public record SubmissionRow(Long id,Reference worker,Reference site,LocalDate workDate,Instant submittedAt,String status) {}
    public record PhotoInfo(Long id,String contentType,Long byteSize) {}
    public record SubmissionDetail(Long id,Reference worker,Reference site,LocalDate workDate,Instant submittedAt,
        String status,Checklist checklist,String notes,List<PhotoInfo> photos) {}
    public record SubmissionFilter(Long siteId,Long workerId,LocalDate from,LocalDate to) {}
    public record SiteCount(Long siteId,String siteName,long count) {}
    public record AdminResult(List<SubmissionRow> items,List<SiteCount> countsBySite) {}
}
