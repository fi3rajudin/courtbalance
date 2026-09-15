package com.fit.badminton.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class AccountControllerTest {

    @Test
    void authenticatedUserCanChangeOwnPassword() {
        UserAccountService users = mock(UserAccountService.class);

        AccountController controller = new AccountController(users);

        var auth = UsernamePasswordAuthenticationToken.authenticated(
                "adam",
                null,
                java.util.List.of()
        );

        var response = controller.changePassword(
                auth,
                new AccountController.ChangePasswordRequest(
                        "OldPassword123",
                        "NewPassword456"
                )
        );

        verify(users).changeOwnPassword(
                "adam",
                "OldPassword123",
                "NewPassword456"
        );

        assertEquals(
                Map.of("status", "password_changed"),
                response
        );
    }
}