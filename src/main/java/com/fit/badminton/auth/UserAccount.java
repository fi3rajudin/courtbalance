package com.fit.badminton.auth;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_account")
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 100)
    private String username;
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected UserAccount() {
    }

    public UserAccount(String u, String p, UserRole r) {
        username = u;
        passwordHash = p;
        role = r;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public void setPasswordHash(String v) {
        passwordHash = v;
        updatedAt = Instant.now();
    }

    public void setRole(UserRole v) {
        role = v;
        updatedAt = Instant.now();
    }

    public void setActive(boolean v) {
        active = v;
        updatedAt = Instant.now();
    }
}
