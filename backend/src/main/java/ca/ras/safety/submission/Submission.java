package ca.ras.safety.submission;
import ca.ras.safety.auth.User;
import ca.ras.safety.site.JobSite;
import ca.ras.safety.photo.Photo;
import ca.ras.safety.submission.SubmissionDtos.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
@Entity @Table(name="submissions")
public class Submission {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private User worker;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private JobSite site;
    @Column(nullable=false) private LocalDate workDate;
    @Column(nullable=false) private Instant submittedAt;
    @Column(nullable=false,length=4000) private String notes;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=10) private Answer hardHat;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=10) private Answer highVisibilityVest;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=10) private Answer safetyBoots;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=10) private Answer eyeProtection;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=10) private Answer fallProtection;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=10) private Answer laddersScaffolds;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=10) private Answer toolsCords;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=10) private Answer hazardsControlled;
    @OneToMany(mappedBy="submission",cascade=CascadeType.ALL) @OrderBy("id ASC") private List<Photo> photos=new ArrayList<>();
    protected Submission() {}
    public Submission(User worker,JobSite site,CreateSubmission form,Instant submittedAt) {
        this.worker=worker;this.site=site;this.workDate=form.workDate();this.submittedAt=submittedAt;
        this.notes=form.notes()==null?"":form.notes().trim();
        var c=form.checklist(); hardHat=c.hardHat();highVisibilityVest=c.highVisibilityVest();
        safetyBoots=c.safetyBoots();eyeProtection=c.eyeProtection();fallProtection=c.fallProtection();
        laddersScaffolds=c.laddersScaffolds();toolsCords=c.toolsCords();hazardsControlled=c.hazardsControlled();
    }
    public void addPhoto(Photo photo) { photos.add(photo); }
    public Long getId() { return id; }
    public User getWorker() { return worker; }
    public JobSite getSite() { return site; }
    public LocalDate getWorkDate() { return workDate; }
    public Instant getSubmittedAt() { return submittedAt; }
    public String getNotes() { return notes; }
    public List<Photo> getPhotos() { return photos; }
    public Checklist getChecklist() { return new Checklist(hardHat,highVisibilityVest,safetyBoots,eyeProtection,
        fallProtection,laddersScaffolds,toolsCords,hazardsControlled); }
}
