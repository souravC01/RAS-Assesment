package ca.ras.safety.photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;
public interface PhotoRepository extends JpaRepository<Photo,Long> {
    @Override @EntityGraph(attributePaths={"submission","submission.worker"}) Optional<Photo> findById(Long id);
    @EntityGraph(attributePaths={"submission","submission.worker"}) Optional<Photo> findByIdAndSubmissionWorkerId(Long id,Long workerId);
}
