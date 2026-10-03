package ca.ras.safety.photo;
import ca.ras.safety.submission.Submission;
import jakarta.persistence.*;
@Entity @Table(name="photos")
public class Photo {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private Submission submission;
    @Column(nullable=false,unique=true) private String storageKey;
    @Column(nullable=false,length=50) private String contentType;
    @Column(nullable=false) private Long byteSize;
    protected Photo() {}
    public Photo(Submission submission,String key,String type,long size) {
        this.submission=submission;storageKey=key;contentType=type;byteSize=size;
    }
    public Long getId() { return id; }
    public Submission getSubmission() { return submission; }
    public String getStorageKey() { return storageKey; }
    public String getContentType() { return contentType; }
    public Long getByteSize() { return byteSize; }
}
