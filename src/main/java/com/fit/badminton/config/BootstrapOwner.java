package com.fit.badminton.config;
import com.fit.badminton.auth.*; import org.springframework.beans.factory.annotation.Value; import org.springframework.boot.ApplicationArguments; import org.springframework.boot.ApplicationRunner; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.stereotype.Component;
@Component
public class BootstrapOwner implements ApplicationRunner {
 private final UserAccountRepository repo; private final PasswordEncoder encoder;
 @Value("${app.bootstrap.owner-username:}") private String username; @Value("${app.bootstrap.owner-password:}") private String password;
 public BootstrapOwner(UserAccountRepository r,PasswordEncoder e){repo=r;encoder=e;}
 public void run(ApplicationArguments args){ if(username==null||username.isBlank()||password==null||password.isBlank()) return; repo.findByUsernameIgnoreCase(username).orElseGet(()->repo.save(new UserAccount(username,encoder.encode(password),UserRole.OWNER))); }
}
