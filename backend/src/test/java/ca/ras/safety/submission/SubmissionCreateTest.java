package ca.ras.safety.submission;

import ca.ras.safety.TestcontainersConfiguration;
import ca.ras.safety.photo.PhotoStorage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"demo.seed-enabled=true","demo.admin-password=TestAdmin123!",
    "demo.framer-a-password=TestFramer123!","demo.framer-b-password=TestOther123!"})
@AutoConfigureMockMvc @Import(TestcontainersConfiguration.class)
class SubmissionCreateTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @MockitoBean PhotoStorage storage;
    @MockitoBean Clock clock;
    @BeforeEach void setup() {
        when(clock.instant()).thenReturn(Instant.parse("2026-10-04T06:30:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("America/Vancouver"));
        if (db.queryForObject("select to_regclass('submissions')",String.class)!=null) {
            db.execute("delete from photos"); db.execute("delete from submissions");
        }
    }
    private MockMultipartFile jpeg() throws Exception {
        var bytes=new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8,8,BufferedImage.TYPE_INT_RGB),"jpeg",bytes);
        return new MockMultipartFile("photos","test.jpg","image/jpeg",bytes.toByteArray());
    }
    private String form(long site, String date) {
        return """
            {"siteId":%d,"workDate":"%s","checklist":{"hardHat":"ISSUE","highVisibilityVest":"PASS",
            "safetyBoots":"PASS","eyeProtection":"PASS","fallProtection":"NA","laddersScaffolds":"PASS",
            "toolsCords":"PASS","hazardsControlled":"PASS"},"notes":"Replacement helmet requested.","workerId":999999}
            """.formatted(site,date);
    }
    private MockMultipartHttpServletRequestBuilder request(String json, MockMultipartFile... photos) {
        var request=multipart("/api/submissions").file(new MockMultipartFile("form","","application/json",json.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        for(var photo:photos) request.file(photo);
        return request.header("X-Expected-Actor",db.queryForObject("select id from app_users where email='framer.a@example.test'",Long.class))
            .with(user("framer.a@example.test").roles("FRAMER")).with(csrf());
    }
    private long site(int index) {
        return db.queryForList("select id from job_sites order by name",Long.class).get(index);
    }
    @Test void staleOrMissingActorCannotCreateUnderAnotherSession() throws Exception {
        String valid=form(site(0),"2026-10-03");
        mvc.perform(request(valid,jpeg()).with(user("framer.b@example.test").roles("FRAMER")))
            .andExpect(status().isPreconditionFailed());
        var missing=multipart("/api/submissions").file(new MockMultipartFile("form","","application/json",valid.getBytes(java.nio.charset.StandardCharsets.UTF_8))).file(jpeg());
        mvc.perform(missing.with(user("framer.a@example.test").roles("FRAMER")).with(csrf()))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(storage);
        assertThat(db.queryForObject("select count(*) from submissions",Long.class)).isZero();
    }
    @Test void rejectsInvalidFormsAndAcceptsReportedIssues() throws Exception {
        // Read-only lookup is also an assessment requirement.
        mvc.perform(get("/api/sites").with(user("framer.a@example.test").roles("FRAMER")))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").isNumber());
        long site=site(0);
        String valid=form(site,"2026-10-03");
        mvc.perform(request(valid,jpeg())).andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber());
        assertThat(db.queryForObject("select count(*) from submissions",Long.class)).isEqualTo(1);
        assertThat(db.queryForObject("select worker_id from submissions",Long.class))
            .isEqualTo(db.queryForObject("select id from app_users where email='framer.a@example.test'",Long.class));
        assertThat(db.queryForObject("select work_date::text from submissions",String.class)).isEqualTo("2026-10-03");
        assertThat(db.queryForObject("select count(*) from photos",Long.class)).isEqualTo(1);
        clearInvocations(storage);
        for(String invalid:List.of(valid.replace("\"hardHat\":\"ISSUE\",",""), valid.replace("\"ISSUE\"","\"UNKNOWN\""),
            form(999999,"2026-10-03"),form(site,"2026-10-04"),valid.replace("Replacement helmet requested.","   "),
            valid.replace("Replacement helmet requested.","a".repeat(4001)),"{broken-json")) {
            mvc.perform(request(invalid,jpeg())).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isString());
        }
        mvc.perform(request(valid)).andExpect(status().isBadRequest());
        mvc.perform(request(valid,jpeg(),jpeg(),jpeg(),jpeg(),jpeg(),jpeg())).andExpect(status().isBadRequest());
        mvc.perform(request(valid,new MockMultipartFile("photos","fake.jpg","image/jpeg",new byte[]{1,2,3}))).andExpect(status().isBadRequest());
        mvc.perform(request(valid,new MockMultipartFile("photos","large.jpg","image/jpeg",new byte[5_000_001]))).andExpect(status().isPayloadTooLarge());
        mvc.perform(request(valid,jpeg()).with(user("admin@example.test").roles("ADMIN"))).andExpect(status().isForbidden());
        verifyNoInteractions(storage);
        assertThat(db.queryForObject("select count(*) from submissions",Long.class)).isEqualTo(1);
    }
    @Test void duplicateAndFailureLeaveNoPartialSubmission() throws Exception {
        mvc.perform(get("/api/sites").with(user("framer.a@example.test").roles("FRAMER"))).andExpect(status().isOk());
        String valid=form(site(0),"2026-10-03");
        var barrier=new CyclicBarrier(2);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var results=new ArrayList<java.util.concurrent.Future<Integer>>();
            for(int i=0;i<2;i++) results.add(pool.submit(()->{barrier.await(10,TimeUnit.SECONDS);return mvc.perform(request(valid,jpeg())).andReturn().getResponse().getStatus();}));
            var statuses=new ArrayList<Integer>(); for(var result:results) statuses.add(result.get(30,TimeUnit.SECONDS));
            assertThat(statuses).containsExactlyInAnyOrder(201,409);
        }
        assertThat(db.queryForObject("select count(*) from submissions",Long.class)).isEqualTo(1);
        mvc.perform(request(form(site(1),"2026-10-03"),jpeg())).andExpect(status().isCreated());
        clearInvocations(storage);
        doNothing().doThrow(new IllegalStateException("storage offline")).when(storage).put(anyString(),any(byte[].class),anyString());
        mvc.perform(request(form(site(0),"2026-10-02"),jpeg(),jpeg())).andExpect(status().isServiceUnavailable());
        verify(storage,atLeastOnce()).delete(anyString());
        assertThat(db.queryForObject("select count(*) from submissions",Long.class)).isEqualTo(2);
        assertThat(db.queryForObject("select count(*) from photos",Long.class)).isEqualTo(2);
        reset(storage);
        db.execute("CREATE FUNCTION fail_submission_commit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'test commit failure' USING ERRCODE='23514'; END $$");
        db.execute("CREATE CONSTRAINT TRIGGER forced_commit_failure AFTER INSERT ON submissions DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION fail_submission_commit()");
        try {
            mvc.perform(request(form(site(0),"2026-10-01"),jpeg())).andExpect(status().isServiceUnavailable());
            verify(storage).delete(anyString());
            assertThat(db.queryForObject("select count(*) from submissions",Long.class)).isEqualTo(2);
            assertThat(db.queryForObject("select count(*) from photos",Long.class)).isEqualTo(2);
        } finally {
            db.execute("DROP TRIGGER forced_commit_failure ON submissions"); db.execute("DROP FUNCTION fail_submission_commit()");
        }
    }
}
