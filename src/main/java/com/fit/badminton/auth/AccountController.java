package com.fit.badminton.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final UserAccountService users;

    public AccountController(UserAccountService users) {
        this.users = users;
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank String newPassword) {
    }

    @PutMapping("/password")
    public Map<String, String> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {

        users.changeOwnPassword(
                authentication.getName(),
                request.currentPassword(),
                request.newPassword());

        return Map.of("status", "password_changed");
    }
}