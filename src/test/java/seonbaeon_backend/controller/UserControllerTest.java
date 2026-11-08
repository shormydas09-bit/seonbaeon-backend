package seonbaeon_backend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import seonbaeon_backend.entity.User;
import seonbaeon_backend.repository.UserRepository;
import seonbaeon_backend.service.AuthService;
import seonbaeon_backend.service.PasswordResetService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserControllerTest {
    private UserRepository users;
    private PasswordResetService passwordReset;
    private AuthService auth;
    private UserController controller;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        passwordReset = mock(PasswordResetService.class);
        auth = mock(AuthService.class);
        controller = new UserController(users, passwordReset, auth);
    }

    @Test
    void signupRejectsMissingFieldsAndShortPasswords() {
        User missing = new User();
        missing.setEmail("student@example.com");
        missing.setPassword("123456");
        assertEquals(HttpStatus.BAD_REQUEST, controller.signup(missing).getStatusCode());

        User shortPassword = new User("Student", "student@example.com", "123");
        assertEquals(HttpStatus.BAD_REQUEST, controller.signup(shortPassword).getStatusCode());
        verifyNoInteractions(users);
    }

    @Test
    void signupRejectsPrivilegedRoleEscalation() {
        User adminAttempt = new User("Attacker", "attacker@example.com", "password123");
        adminAttempt.setRole("ADMIN");
        assertEquals(HttpStatus.BAD_REQUEST, controller.signup(adminAttempt).getStatusCode());
        verify(users, never()).save(any());
    }

    @Test
    void signupRejectsDuplicateEmailCaseInsensitively() {
        when(users.existsByEmailIgnoreCase("student@example.com")).thenReturn(true);
        User duplicate = new User("Student", " Student@Example.com ", "password123");
        assertEquals(HttpStatus.CONFLICT, controller.signup(duplicate).getStatusCode());
        verify(users, never()).save(any());
    }

    @Test
    void loginRejectsWrongPasswordAndSuspendedAccounts() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        User user = new User("Student", "student@example.com", encoder.encode("correct-password"));
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(user));

        User wrong = new User();
        wrong.setEmail("student@example.com");
        wrong.setPassword("incorrect-password");
        assertEquals(HttpStatus.UNAUTHORIZED, controller.login(wrong).getStatusCode());
        verifyNoInteractions(auth);

        user.setActive(false);
        User correct = new User();
        correct.setEmail("student@example.com");
        correct.setPassword("correct-password");
        assertEquals(HttpStatus.FORBIDDEN, controller.login(correct).getStatusCode());
        verifyNoInteractions(auth);
    }

    @Test
    void loginRejectsMismatchedAccountRole() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        User teacher = new User("Teacher", "teacher@example.com", encoder.encode("correct-password"));
        teacher.setRole("TEACHER");
        when(users.findByEmailIgnoreCase("teacher@example.com")).thenReturn(Optional.of(teacher));

        User request = new User();
        request.setEmail("teacher@example.com");
        request.setPassword("correct-password");
        request.setRole("STUDENT");
        assertEquals(HttpStatus.FORBIDDEN, controller.login(request).getStatusCode());
        verifyNoInteractions(auth);
    }

    @Test
    void forgotPasswordUsesNonEnumeratingResponse() {
        var response = controller.forgotPassword(java.util.Map.of("email", "unknown@example.com"));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("If an account exists"));
        verify(passwordReset).createResetRequest("unknown@example.com");
    }
}
