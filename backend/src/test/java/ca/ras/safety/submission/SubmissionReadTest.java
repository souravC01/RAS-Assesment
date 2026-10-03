package ca.ras.safety.submission;
import ca.ras.safety.TestcontainersConfiguration;
import ca.ras.safety.auth.UserRepository;
import ca.ras.safety.photo.PhotoStorage;
import ca.ras.safety.site.JobSiteRepository;
import ca.ras.safety.submission.SubmissionDtos.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.*;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"demo.seed-enabled=true","demo.admin-password=TestAdmin123!",
    "demo.framer-a-password=TestFramer123!","demo.framer-b-password=TestOther123!"})
@AutoConfigureMockMvc @Import(TestcontainersConfiguration.class)
class SubmissionReadTest {
    @Autowired MockMvc mvc; @Autowired JdbcTemplate db;
    @Autowired SubmissionService service; @Autowired UserRepository users; @Autowired JobSiteRepository sites;
    @MockitoBean PhotoStorage storage; @MockitoBean Clock clock;
    long aId,bId,bPhoto,siteId,workerId;
    @BeforeEach void seed() throws Exception {
        when(clock.instant()).thenReturn(Instant.parse("2026-10-04T06:30:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("America/Vancouver"));
        when(storage.presign(anyString(),any())).thenReturn(URI.create("https://example.test/signed"));
        db.execute("delete from photos"); db.execute("delete from submissions");
        var a=users.findByEmail("framer.a@example.test").orElseThrow();
        var b=users.findByEmail("framer.b@example.test").orElseThrow();
        var jobSites=sites.findAllByOrderByNameAsc(); siteId=jobSites.get(0).getId();workerId=a.getId();
        var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(8,8,BufferedImage.TYPE_INT_RGB),"jpeg",out);
        var photo=new MockMultipartFile("photos","photo.jpg","image/jpeg",out.toByteArray());
        var checks=new Checklist(Answer.PASS,Answer.PASS,Answer.PASS,Answer.PASS,Answer.NA,Answer.PASS,Answer.PASS,Answer.PASS);
        aId=service.create(a,new CreateSubmission(siteId,LocalDate.of(2026,10,3),checks,""),List.of(photo));
        bId=service.create(b,new CreateSubmission(siteId,LocalDate.of(2026,10,3),checks,""),List.of(photo));
        service.create(a,new CreateSubmission(jobSites.get(1).getId(),LocalDate.of(2026,10,1),checks,""),List.of(photo));
        service.create(b,new CreateSubmission(siteId,LocalDate.of(2026,10,2),checks,""),List.of(photo));
        bPhoto=db.queryForObject("select id from photos where submission_id=?",Long.class,bId);
        clearInvocations(storage);
    }
    @Test void enforcesOwnershipAndInclusiveFilters() throws Exception {
        var a=user("framer.a@example.test").roles("FRAMER");var admin=user("admin@example.test").roles("ADMIN");
        mvc.perform(get("/api/submissions").with(a)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].id").value(aId));
        mvc.perform(get("/api/submissions/"+bId).with(a)).andExpect(status().isNotFound());
        mvc.perform(get("/api/photos/"+bPhoto+"/url").with(a)).andExpect(status().isNotFound());
        verify(storage,never()).presign(anyString(),any());
        mvc.perform(get("/api/admin/submissions").with(a)).andExpect(status().isForbidden());
        mvc.perform(get("/api/submissions/"+bId).with(admin)).andExpect(status().isOk())
            .andExpect(jsonPath("$.photos.length()").value(1)).andExpect(jsonPath("$.checklist.hardHat").value("PASS"))
            .andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(get("/api/photos/"+bPhoto+"/url").with(admin)).andExpect(status().isOk())
            .andExpect(jsonPath("$.url").value("https://example.test/signed"))
            .andExpect(jsonPath("$.expiresAt").value("2026-10-04T06:35:00Z"))
            .andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(get("/api/photos/"+bPhoto+"/url").with(a)).andExpect(status().isNotFound());
        verify(storage,times(1)).presign(anyString(),any());
        mvc.perform(get("/api/admin/workers").with(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/admin/submissions").with(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(4));
        mvc.perform(get("/api/admin/submissions").param("from","2026-10-03").param("to","2026-10-03").with(admin))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(2)).andExpect(jsonPath("$.countsBySite[0].count").value(2));
        mvc.perform(get("/api/admin/submissions").param("workerId",Long.toString(workerId)).with(admin)).andExpect(jsonPath("$.items.length()").value(2));
        mvc.perform(get("/api/admin/submissions").param("siteId",Long.toString(siteId)).with(admin)).andExpect(jsonPath("$.items.length()").value(3));
        mvc.perform(get("/api/admin/submissions").param("from","2026-10-02").with(admin)).andExpect(jsonPath("$.items.length()").value(3));
        mvc.perform(get("/api/admin/submissions").param("to","2026-10-02").with(admin)).andExpect(jsonPath("$.items.length()").value(2));
        mvc.perform(get("/api/admin/submissions").param("workerId",Long.toString(workerId)).param("siteId",Long.toString(siteId))
            .param("from","2026-10-03").param("to","2026-10-03").with(admin)).andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.countsBySite[0].count").value(1));
        mvc.perform(get("/api/admin/submissions").param("siteId","999999").with(admin))
            .andExpect(jsonPath("$.items.length()").value(0)).andExpect(jsonPath("$.countsBySite.length()").value(0));
        mvc.perform(get("/api/admin/submissions").param("workerId","999999").with(admin)).andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(get("/api/admin/submissions").param("from","2026-10-04").with(admin)).andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(get("/api/admin/submissions").param("from","2026-10-03").param("to","2026-10-01").with(admin)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/submissions/"+aId)).andExpect(status().isUnauthorized());
    }
}
