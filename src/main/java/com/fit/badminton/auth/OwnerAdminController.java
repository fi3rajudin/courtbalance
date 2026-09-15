package com.fit.badminton.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/owner/admins")
public class OwnerAdminController {

  private final UserAccountService users;

  public OwnerAdminController(UserAccountService users) {
    this.users = users;
  }

  public record CreateAdminRequest(
      @NotBlank String username,
      @NotBlank String password) {
  }

  @PostMapping
  public Map<String, Object> createAdmin(
      @Valid @RequestBody CreateAdminRequest request) {

    UserAccount admin = users.createAdmin(
        request.username(),
        request.password());

    return Map.of(
        "username", admin.getUsername(),
        "role", admin.getRole().name(),
        "active", admin.isActive());
  }

  @GetMapping
  public java.util.List<Map<String, Object>> listAdmins() {
    return users.listAdmins().stream()
        .map(admin -> Map.<String, Object>of(
            "id", admin.getId(),
            "username", admin.getUsername(),
            "role", admin.getRole().name(),
            "active", admin.isActive()))
        .toList();
  }

  public record SetActiveRequest(boolean active) {
  }

  @PutMapping("/{id}/active")
  public Map<String, Object> setActive(
      @PathVariable Long id,
      @RequestBody SetActiveRequest request) {

    users.setAdminActive(id, request.active());

    return Map.of(
        "status", "updated",
        "active", request.active());
  }

  public record ResetPasswordRequest(
      @NotBlank String newPassword) {
  }

  @PutMapping("/{id}/password")
  public Map<String, String> resetPassword(
      @PathVariable Long id,
      @Valid @RequestBody ResetPasswordRequest request) {

    users.resetPassword(id, request.newPassword());

    return Map.of("status", "password_reset");
  }
}