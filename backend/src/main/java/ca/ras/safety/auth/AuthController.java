package ca.ras.safety.auth;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    private final UserRepository users;
    public AuthController(UserRepository users) { this.users = users; }
    public record Actor(Long id, String name, String email, String role) {}
    public record CsrfView(String headerName, String token) {}
    @GetMapping("/api/health") Map<String, String> health() { return Map.of("status", "UP"); }
    @GetMapping("/api/auth/csrf") CsrfView csrf(CsrfToken token) {
        return new CsrfView(token.getHeaderName(), token.getToken());
    }
    @GetMapping("/api/auth/me") public Actor me(Authentication authentication) {
        User user = users.findByEmail(authentication.getName()).orElseThrow();
        return new Actor(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }
}
