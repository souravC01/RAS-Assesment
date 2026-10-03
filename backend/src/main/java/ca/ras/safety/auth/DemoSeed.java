package ca.ras.safety.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ca.ras.safety.site.JobSite;
import ca.ras.safety.site.JobSiteRepository;

@Component
@ConditionalOnProperty(name = "demo.seed-enabled", havingValue = "true")
public class DemoSeed implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String adminPassword, framerAPassword, framerBPassword;
    private final JobSiteRepository sites;
    public DemoSeed(UserRepository users, PasswordEncoder encoder, JobSiteRepository sites,
        @Value("${demo.admin-password:}") String adminPassword,
        @Value("${demo.framer-a-password:}") String framerAPassword,
        @Value("${demo.framer-b-password:}") String framerBPassword) {
        this.users = users; this.encoder = encoder;
        this.sites = sites;
        this.adminPassword = adminPassword; this.framerAPassword = framerAPassword; this.framerBPassword = framerBPassword;
    }
    @Override @Transactional public void run(String... args) {
        seed("Demo Admin", "admin@example.test", adminPassword, User.Role.ADMIN);
        seed("Alex Morgan", "framer.a@example.test", framerAPassword, User.Role.FRAMER);
        seed("Taylor Reed", "framer.b@example.test", framerBPassword, User.Role.FRAMER);
        for (String name : java.util.List.of("Cedar Grove", "Harbour View", "Maple Court")) {
            if (sites.findByName(name).isEmpty()) sites.save(new JobSite(name));
        }
    }
    private void seed(String name, String email, String password, User.Role role) {
        if (users.findByEmail(email).isPresent()) return;
        if (password.length() < 12) throw new IllegalStateException("Demo passwords must have at least 12 characters.");
        users.save(new User(name, email, encoder.encode(password), role));
    }
}
