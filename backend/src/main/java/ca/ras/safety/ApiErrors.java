package ca.ras.safety;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiErrors {
    public record Error(String message,Map<String,String> fieldErrors) {}
    public static class Failure extends RuntimeException {
        public final int status;
        public final Map<String,String> fields;
        public Failure(int status,String message) { this(status,message,Map.of()); }
        public Failure(int status,String message,Map<String,String> fields) {
            super(message);this.status=status;this.fields=fields;
        }
    }
    @ExceptionHandler(Failure.class) ResponseEntity<Error> failure(Failure ex) {
        return ResponseEntity.status(ex.status).body(new Error(ex.getMessage(),ex.fields));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<Error> validation(MethodArgumentNotValidException ex) {
        var fields=new LinkedHashMap<String,String>();
        ex.getBindingResult().getFieldErrors().forEach(e->fields.put(e.getField(),e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new Error("Please complete the required fields.",fields));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class) ResponseEntity<Error> oversized() {
        return ResponseEntity.status(413).body(new Error("Use up to five photos, each at most 5 MB.",Map.of("photos","File size limit exceeded.")));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class,MissingServletRequestPartException.class,
        MethodArgumentTypeMismatchException.class,MultipartException.class}) ResponseEntity<Error> malformed() {
        return ResponseEntity.badRequest().body(new Error("The request contains missing or invalid data.",Map.of()));
    }
    @ExceptionHandler(ResponseStatusException.class) ResponseEntity<Error> status(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(new Error("The request could not be completed.",Map.of()));
    }
}
