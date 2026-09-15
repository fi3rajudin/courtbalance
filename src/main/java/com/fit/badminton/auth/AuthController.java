package com.fit.badminton.auth;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.*;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthenticationManager am;
  private final UserAccountService users;
  private final HttpSessionSecurityContextRepository repo = new HttpSessionSecurityContextRepository();

  public AuthController(AuthenticationManager am, UserAccountService users) {
    this.am = am;
    this.users = users;
  }

  public record LoginRequest(@NotBlank String username, @NotBlank String password) {
  }

  @PostMapping("/login")
  public Map<String, Object> login(@Valid @RequestBody LoginRequest b, HttpServletRequest req,
      HttpServletResponse res) {
    Authentication auth = am
        .authenticate(UsernamePasswordAuthenticationToken.unauthenticated(b.username(), b.password()));
    req.getSession(true);
    req.changeSessionId();
    SecurityContext ctx = SecurityContextHolder.createEmptyContext();
    ctx.setAuthentication(auth);
    SecurityContextHolder.setContext(ctx);
    repo.saveContext(ctx, req, res);
    UserAccount u = users.getByUsername(auth.getName());
    return Map.of("username", u.getUsername(), "role", u.getRole().name());
  }

  @PostMapping("/logout")
  public Map<String, String> logout(HttpServletRequest req, HttpServletResponse res) {
    SecurityContextHolder.clearContext();
    HttpSession s = req.getSession(false);
    if (s != null)
      s.invalidate();
    return Map.of("status", "logged_out");
  }

  @GetMapping("/csrf")
  public Map<String, String> csrf(org.springframework.security.web.csrf.CsrfToken token) {
    return Map.of("token", token.getToken());
  }

  @GetMapping("/me")
public Map<String, Object> me(Authentication a) {
    if (a == null || !a.isAuthenticated()) {
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED,
                "Not authenticated"
        );
    }

    UserAccount u = users.getByUsername(a.getName());

    return Map.of(
            "username", u.getUsername(),
            "role", u.getRole().name(),
            "authorities", a.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .toList()
    );
}
}
