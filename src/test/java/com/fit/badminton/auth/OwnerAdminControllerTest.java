package com.fit.badminton.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

class OwnerAdminControllerTest {

    @Test
    void ownerCanCreateAdminThroughApi() {
        UserAccountService users = mock(UserAccountService.class);

        UserAccount admin = new UserAccount(
                "adam",
                "hashed-password",
                UserRole.ADMIN
        );

        when(users.createAdmin("adam", "Password123"))
                .thenReturn(admin);

        OwnerAdminController controller = new OwnerAdminController(users);

        var response = controller.createAdmin(
                new OwnerAdminController.CreateAdminRequest(
                        "adam",
                        "Password123"
                )
        );

        assertEquals("adam", response.get("username"));
        assertEquals("ADMIN", response.get("role"));
        assertEquals(true, response.get("active"));

        verify(users).createAdmin("adam", "Password123");
    }

   @Test
void ownerCanListAdminAccounts() {
    UserAccountService users = mock(UserAccountService.class);

    UserAccount adam = mock(UserAccount.class);
    when(adam.getId()).thenReturn(2L);
    when(adam.getUsername()).thenReturn("adam");
    when(adam.getRole()).thenReturn(UserRole.ADMIN);
    when(adam.isActive()).thenReturn(true);

    when(users.listAdmins())
            .thenReturn(java.util.List.of(adam));

    OwnerAdminController controller = new OwnerAdminController(users);

    var response = controller.listAdmins();

    assertEquals(1, response.size());
    assertEquals(2L, response.get(0).get("id"));
    assertEquals("adam", response.get(0).get("username"));
}

@Test
void ownerCanDeactivateAdminAccount() {
    UserAccountService users = mock(UserAccountService.class);

    OwnerAdminController controller = new OwnerAdminController(users);

    controller.setActive(
            2L,
            new OwnerAdminController.SetActiveRequest(false)
    );

    verify(users).setAdminActive(2L, false);
}
@Test
void ownerCanResetAdminPassword() {
    UserAccountService users = mock(UserAccountService.class);

    OwnerAdminController controller = new OwnerAdminController(users);

    controller.resetPassword(
            2L,
            new OwnerAdminController.ResetPasswordRequest("ResetPassword789")
    );

    verify(users).resetPassword(2L, "ResetPassword789");
}


}