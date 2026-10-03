package ca.ras.safety.submission;

import ca.ras.safety.ApiErrors.Failure;
import ca.ras.safety.TestcontainersConfiguration;
import ca.ras.safety.auth.UserRepository;
import ca.ras.safety.photo.PhotoStorage;
import ca.ras.safety.site.JobSiteRepository;
import ca.ras.safety.submission.SubmissionDtos.*;
import jakarta.persistence.EntityManagerFactory;
import jakarta.validation.Validator;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.*;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.DefaultTransactionStatus;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties={"demo.seed-enabled=true","demo.admin-password=TestAdmin123!",
    "demo.framer-a-password=TestFramer123!","demo.framer-b-password=TestOther123!"})
@Import(TestcontainersConfiguration.class)
class SubmissionCommitOutcomeTest {
    @Autowired SubmissionRepository submissions;
    @Autowired JobSiteRepository sites;
    @Autowired UserRepository users;
    @Autowired EntityManagerFactory factory;
    @Autowired Validator validator;
    @Autowired JdbcTemplate db;
    @MockitoBean PhotoStorage storage;
    @MockitoBean Clock clock;
    @BeforeEach void setup() {
        when(clock.instant()).thenReturn(Instant.parse("2026-10-04T06:30:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("America/Vancouver"));
        db.execute("delete from photos");db.execute("delete from submissions");
    }
    private void uncertainCommit(boolean committed) throws Exception {
        var manager=new JpaTransactionManager(factory) {
            @Override protected void doCommit(DefaultTransactionStatus status) {
                if(committed) super.doCommit(status); else super.doRollback(status);
                // Inject lost acknowledgement after an actual PostgreSQL outcome.
                throw new TransactionSystemException("Commit acknowledgement unavailable");
            }
        };
        var service=new SubmissionService(submissions,sites,storage,clock,validator,manager);
        var checks=new Checklist(Answer.PASS,Answer.PASS,Answer.PASS,Answer.PASS,
            Answer.NA,Answer.PASS,Answer.PASS,Answer.PASS);
        var form=new CreateSubmission(sites.findAll().getFirst().getId(),LocalDate.of(2026,10,3),checks,"");
        var bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(8,8,BufferedImage.TYPE_INT_RGB),"jpeg",bytes);
        var photo=new MockMultipartFile("photos","test.jpg","image/jpeg",bytes.toByteArray());
        assertThatThrownBy(()->service.create(users.findByEmail("framer.a@example.test").orElseThrow(),form,List.of(photo)))
            .isInstanceOf(Failure.class).extracting("status").isEqualTo(503);
        assertThat(db.queryForObject("select count(*) from submissions",Long.class)).isEqualTo(committed?1:0);
        assertThat(db.queryForObject("select count(*) from photos",Long.class)).isEqualTo(committed?1:0);
        verify(storage).put(anyString(),any(byte[].class),eq("image/jpeg"));
        verify(storage,never()).delete(anyString());
    }
    @Test void committedSubmissionRetainsPhotosWhenAcknowledgementIsLost() throws Exception { uncertainCommit(true); }
    @Test void unknownCommitRetainsPhotosEvenWhenNoRowsAreVisible() throws Exception { uncertainCommit(false); }
}
