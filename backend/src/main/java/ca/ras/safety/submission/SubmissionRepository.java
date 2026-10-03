package ca.ras.safety.submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface SubmissionRepository extends JpaRepository<Submission,Long> {
    @EntityGraph(attributePaths={"worker","site"})
    List<Submission> findAllByWorkerIdOrderByWorkDateDescIdDesc(Long workerId);
    @Override @EntityGraph(attributePaths={"worker","site","photos"}) Optional<Submission> findById(Long id);
    @EntityGraph(attributePaths={"worker","site","photos"}) Optional<Submission> findByIdAndWorkerId(Long id,Long workerId);
    @EntityGraph(attributePaths={"worker","site"})
    @Query("select s from Submission s where (:siteId is null or s.site.id=:siteId) and (:workerId is null or s.worker.id=:workerId) and s.workDate>=coalesce(:fromDate,s.workDate) and s.workDate<=coalesce(:toDate,s.workDate) order by s.workDate desc,s.id desc")
    List<Submission> search(@Param("siteId") Long siteId,@Param("workerId") Long workerId,
        @Param("fromDate") LocalDate from,@Param("toDate") LocalDate to);
}
