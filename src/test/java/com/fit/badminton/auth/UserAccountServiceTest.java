package com.fit.badminton.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserAccountServiceTest {

    @Test
    void ownerCanCreateAdminAccount() {
        UserAccountRepository repo = mock(UserAccountRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);

        when(repo.findByUsernameIgnoreCase("adam"))
                .thenReturn(java.util.Optional.empty());

        when(encoder.encode("Password123"))
                .thenReturn("hashed-password");

        when(repo.save(any(UserAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserAccountService service = new UserAccountService(repo, encoder);

        UserAccount admin = service.createAdmin("adam", "Password123");

        assertEquals("adam", admin.getUsername());
        assertEquals(UserRole.ADMIN, admin.getRole());
        assertEquals("hashed-password", admin.getPasswordHash());
        assertTrue(admin.isActive());

        verify(repo).save(any(UserAccount.class));
    }

    @Test
void cannotCreateAdminWithExistingUsername() {
    UserAccountRepository repo = mock(UserAccountRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);

    UserAccount existing = new UserAccount(
            "adam",
            "existing-hash",
            UserRole.ADMIN
    );

    when(repo.findByUsernameIgnoreCase("adam"))
            .thenReturn(java.util.Optional.of(existing));

    UserAccountService service = new UserAccountService(repo, encoder);

    org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> service.createAdmin("adam", "Password123")
    );

    verify(repo, never()).save(any());
}

@Test
void adminCanChangeOwnPasswordWithCorrectCurrentPassword() {
    UserAccountRepository repo = mock(UserAccountRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);

    UserAccount admin = new UserAccount(
            "adam",
            "old-hash",
            UserRole.ADMIN
    );

    when(repo.findByUsernameIgnoreCase("adam"))
            .thenReturn(java.util.Optional.of(admin));

    when(encoder.matches("OldPassword123", "old-hash"))
            .thenReturn(true);

    when(encoder.encode("NewPassword456"))
            .thenReturn("new-hash");

    UserAccountService service = new UserAccountService(repo, encoder);

    service.changeOwnPassword(
            "adam",
            "OldPassword123",
            "NewPassword456"
    );

    assertEquals("new-hash", admin.getPasswordHash());
    verify(repo).save(admin);
}

@Test
void cannotChangeOwnPasswordWithWrongCurrentPassword() {
    UserAccountRepository repo = mock(UserAccountRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);

    UserAccount admin = new UserAccount(
            "adam",
            "old-hash",
            UserRole.ADMIN
    );

    when(repo.findByUsernameIgnoreCase("adam"))
            .thenReturn(java.util.Optional.of(admin));

    when(encoder.matches("WrongPassword", "old-hash"))
            .thenReturn(false);

    UserAccountService service = new UserAccountService(repo, encoder);

    org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> service.changeOwnPassword(
                    "adam",
                    "WrongPassword",
                    "NewPassword456"
            )
    );

    verify(repo, never()).save(any());
}

@Test
void ownerCanResetAdminPassword() {
    UserAccountRepository repo = mock(UserAccountRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);

    UserAccount admin = new UserAccount(
            "adam",
            "old-hash",
            UserRole.ADMIN
    );

    when(repo.findById(2L))
            .thenReturn(java.util.Optional.of(admin));

    when(encoder.encode("ResetPassword789"))
            .thenReturn("reset-hash");

    UserAccountService service = new UserAccountService(repo, encoder);

    service.resetPassword(2L, "ResetPassword789");

    assertEquals("reset-hash", admin.getPasswordHash());
    verify(repo).save(admin);
}

@Test
void ownerCanDeactivateAdminAccount() {
    UserAccountRepository repo = mock(UserAccountRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);

    UserAccount admin = new UserAccount(
            "adam",
            "hash",
            UserRole.ADMIN
    );

    when(repo.findById(2L))
            .thenReturn(java.util.Optional.of(admin));

    UserAccountService service = new UserAccountService(repo, encoder);

    service.setAdminActive(2L, false);

    assertTrue(!admin.isActive());
    verify(repo).save(admin);
}

@Test
void ownerCanListAdminAccounts() {
    UserAccountRepository repo = mock(UserAccountRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);

    UserAccount adam = new UserAccount(
            "adam",
            "hash-1",
            UserRole.ADMIN
    );

    UserAccount ali = new UserAccount(
            "ali",
            "hash-2",
            UserRole.ADMIN
    );

    when(repo.findAll())
            .thenReturn(java.util.List.of(adam, ali));

    UserAccountService service = new UserAccountService(repo, encoder);

    java.util.List<UserAccount> admins = service.listAdmins();

    assertEquals(2, admins.size());
    assertEquals("adam", admins.get(0).getUsername());
    assertEquals("ali", admins.get(1).getUsername());
}

@Test
void maintainerListDoesNotIncludeOwnerAccount() {
    UserAccountRepository repo = mock(UserAccountRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);

    UserAccount owner = new UserAccount(
            "fit",
            "owner-hash",
            UserRole.OWNER
    );

    UserAccount admin = new UserAccount(
            "adam",
            "admin-hash",
            UserRole.ADMIN
    );

    when(repo.findAll())
            .thenReturn(java.util.List.of(owner, admin));

    UserAccountService service = new UserAccountService(repo, encoder);

    java.util.List<UserAccount> admins = service.listAdmins();

    assertEquals(1, admins.size());
    assertEquals("adam", admins.get(0).getUsername());
    assertEquals(UserRole.ADMIN, admins.get(0).getRole());
}
}