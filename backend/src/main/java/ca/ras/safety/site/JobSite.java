package ca.ras.safety.site;
import jakarta.persistence.*;
@Entity @Table(name="job_sites")
public class JobSite {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true) private String name;
    protected JobSite() {}
    public JobSite(String name) { this.name=name; }
    public Long getId() { return id; }
    public String getName() { return name; }
}
