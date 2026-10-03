package ca.ras.safety.site;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController
public class SiteController {
    private final JobSiteRepository sites;
    public SiteController(JobSiteRepository sites) { this.sites=sites; }
    public record Site(Long id,String name) {}
    @GetMapping("/api/sites") List<Site> sites() {
        return sites.findAllByOrderByNameAsc().stream().map(s->new Site(s.getId(),s.getName())).toList();
    }
}
