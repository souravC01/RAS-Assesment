package ca.ras.safety.submission;

import ca.ras.safety.ApiErrors.Failure;
import ca.ras.safety.auth.User;
import ca.ras.safety.photo.*;
import ca.ras.safety.site.JobSiteRepository;
import ca.ras.safety.submission.SubmissionDtos.*;
import jakarta.validation.Validator;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SubmissionService {
    private final SubmissionRepository submissions;
    private final JobSiteRepository sites;
    private final PhotoStorage storage;
    private final Clock clock;
    private final Validator validator;
    private final TransactionTemplate transaction;
    public SubmissionService(SubmissionRepository submissions,JobSiteRepository sites,PhotoStorage storage,
        Clock clock,Validator validator,PlatformTransactionManager manager) {
        this.submissions=submissions;this.sites=sites;this.storage=storage;this.clock=clock;
        this.validator=validator;this.transaction=new TransactionTemplate(manager);
    }
    public Long create(User actor,CreateSubmission form,List<MultipartFile> photos) {
        if(actor.getRole()!=User.Role.FRAMER) throw new Failure(403,"Only framers can create submissions.");
        var errors=new LinkedHashMap<String,String>();
        validator.validate(form).forEach(e->errors.put(e.getPropertyPath().toString(),e.getMessage()));
        if(!errors.isEmpty()) throw new Failure(400,"Please complete the required fields.",errors);
        if(form.workDate().isAfter(LocalDate.now(clock))) invalid("workDate","Choose today or an earlier date.");
        if(form.checklist().hasIssue() && (form.notes()==null || form.notes().isBlank())) invalid("notes","Explain the reported issue.");
        var site=sites.findById(form.siteId()).orElseThrow(()->new Failure(400,"Choose an available job site.",Map.of("siteId","Site not found.")));
        if(photos==null || photos.isEmpty() || photos.size()>5) invalid("photos","Add one to five photos.");
        var contents=new ArrayList<byte[]>();
        for(var photo:photos) contents.add(validatePhoto(photo));
        var keys=new ArrayList<String>();
        try {
            for(int i=0;i<photos.size();i++) {
                String key="submissions/"+UUID.randomUUID();
                keys.add(key);
                storage.put(key,contents.get(i),photos.get(i).getContentType());
            }
            return transaction.execute(tx->{
                var submission=new Submission(actor,site,form,clock.instant());
                for(int i=0;i<photos.size();i++) submission.addPhoto(new Photo(submission,keys.get(i),
                    photos.get(i).getContentType(),contents.get(i).length));
                return submissions.saveAndFlush(submission).getId();
            });
        } catch(RuntimeException ex) {
            // ponytail: Process death can leave an orphan object; a storage lifecycle sweep is the future remedy.
            for(String key:keys) try { storage.delete(key); } catch(RuntimeException cleanup) { ex.addSuppressed(cleanup); }
            for(Throwable cause=ex;cause!=null;cause=cause.getCause()) {
                if(cause instanceof org.hibernate.exception.ConstraintViolationException constraint
                    && "uq_worker_site_date".equals(constraint.getConstraintName()))
                    throw new Failure(409,"You already submitted a form for this site and date.");
            }
            throw new Failure(503,"The submission could not be saved. Please check your history before trying again.");
        }
    }
    private byte[] validatePhoto(MultipartFile photo) {
        if(photo.getSize()>5_000_000) throw new Failure(413,"Each photo must be at most 5 MB.",Map.of("photos","Photo too large."));
        if(photo.isEmpty() || !("image/jpeg".equals(photo.getContentType()) || "image/png".equals(photo.getContentType())))
            invalid("photos","Use a valid JPEG or PNG image.");
        try {
            byte[] bytes=photo.getBytes();
            try(var source=new java.io.ByteArrayInputStream(bytes);var input=ImageIO.createImageInputStream(source)) {
                var readers=ImageIO.getImageReaders(input);
                if(!readers.hasNext()) invalid("photos","The image could not be read.");
                var reader=readers.next();
                try {
                    reader.setInput(input);
                    String expected="image/jpeg".equals(photo.getContentType())?"jpeg":"png";
                    if(!expected.equalsIgnoreCase(reader.getFormatName())) invalid("photos","Image contents do not match the file type.");
                    if((long)reader.getWidth(0)*reader.getHeight(0)>25_000_000) invalid("photos","Use an image with at most 25 million pixels.");
                    if(reader.read(0)==null) invalid("photos","The image could not be read.");
                } finally {reader.dispose();}
            }
            return bytes;
        } catch(IOException ex) { throw new Failure(400,"The image could not be read.",Map.of("photos","Invalid image.")); }
    }
    private static void invalid(String field,String message) { throw new Failure(400,message,Map.of(field,message)); }
    @Transactional(readOnly=true) public List<SubmissionRow> history(User actor) {
        return submissions.findAllByWorkerIdOrderByWorkDateDescIdDesc(actor.getId()).stream().map(this::row).toList();
    }
    @Transactional(readOnly=true) public SubmissionDetail detail(User actor,Long id) {
        var submission=(actor.getRole()==User.Role.ADMIN?submissions.findById(id):submissions.findByIdAndWorkerId(id,actor.getId()))
            .orElseThrow(()->new Failure(404,"Submission not found."));
        var row=row(submission);
        return new SubmissionDetail(row.id(),row.worker(),row.site(),row.workDate(),row.submittedAt(),row.status(),
            submission.getChecklist(),submission.getNotes(),submission.getPhotos().stream()
                .map(p->new PhotoInfo(p.getId(),p.getContentType(),p.getByteSize())).toList());
    }
    @Transactional(readOnly=true) public AdminResult search(SubmissionFilter filter) {
        if(filter.from()!=null && filter.to()!=null && filter.from().isAfter(filter.to())) invalid("from","Start date must be on or before end date.");
        // ponytail: Loads the assessment dataset; paginate and aggregate in SQL if volume grows.
        var items=submissions.search(filter.siteId(),filter.workerId(),filter.from(),filter.to()).stream().map(this::row).toList();
        var counts=new LinkedHashMap<Long,SiteCount>();
        for(var item:items) {
            var prior=counts.get(item.site().id());
            counts.put(item.site().id(),new SiteCount(item.site().id(),item.site().name(),prior==null?1:prior.count()+1));
        }
        return new AdminResult(items,List.copyOf(counts.values()));
    }
    private SubmissionRow row(Submission s) { return new SubmissionRow(s.getId(),new Reference(s.getWorker().getId(),s.getWorker().getName()),
        new Reference(s.getSite().getId(),s.getSite().getName()),s.getWorkDate(),s.getSubmittedAt(),"Submitted"); }
}
