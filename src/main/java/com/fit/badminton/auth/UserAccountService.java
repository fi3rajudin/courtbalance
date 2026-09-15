package com.fit.badminton.auth;

import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class UserAccountService implements UserDetailsService {
    private final UserAccountRepository repo;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserAccountRepository r, PasswordEncoder passwordEncoder) {
    repo = r;
    this.passwordEncoder = passwordEncoder;
}

    @Override
    public UserDetails loadUserByUsername(String username) {
        UserAccount a = repo.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        return User.withUsername(a.getUsername()).password(a.getPasswordHash()).disabled(!a.isActive())
                .authorities(new SimpleGrantedAuthority("ROLE_" + a.getRole().name())).build();
    }

    public UserAccount getByUsername(String u) {
        return repo.findByUsernameIgnoreCase(u).orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    }

    public UserAccount createAdmin(String username, String rawPassword) {
    if (repo.findByUsernameIgnoreCase(username).isPresent()) {
        throw new IllegalArgumentException("Username already exists");
    }

    String passwordHash = passwordEncoder.encode(rawPassword);

    UserAccount admin = new UserAccount(
            username,
            passwordHash,
            UserRole.ADMIN
    );

    return repo.save(admin);
}

public void changeOwnPassword(String username, String currentPassword, String newPassword) {
    UserAccount account = getByUsername(username);

    if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
        throw new IllegalArgumentException("Current password is incorrect");
    }

    account.setPasswordHash(passwordEncoder.encode(newPassword));
    repo.save(account);
}

public void resetPassword(Long accountId, String newPassword) {
    UserAccount account = repo.findById(accountId)
            .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));

    if (account.getRole() != UserRole.ADMIN) {
        throw new IllegalArgumentException("Only ADMIN accounts can be reset here");
    }

    account.setPasswordHash(passwordEncoder.encode(newPassword));
    repo.save(account);
}

public void setAdminActive(Long accountId, boolean active) {
    UserAccount account = repo.findById(accountId)
            .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));

    if (account.getRole() != UserRole.ADMIN) {
        throw new IllegalArgumentException("Only ADMIN accounts can be managed here");
    }

    account.setActive(active);
    repo.save(account);
}

public java.util.List<UserAccount> listAdmins() {
    return repo.findAll().stream()
            .filter(account -> account.getRole() == UserRole.ADMIN)
            .toList();
}
}
