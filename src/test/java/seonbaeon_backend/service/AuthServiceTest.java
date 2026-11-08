package seonbaeon_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import seonbaeon_backend.entity.AuthSession;
import seonbaeon_backend.entity.User;
import seonbaeon_backend.repository.AuthSessionRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private AuthSessionRepository sessions;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        sessions = mock(AuthSessionRepository.class);
        auth = new AuthService(sessions);
    }

    @Test
    void createsUniqueHighEntropyBearerTokensWithFourteenDayExpiry() {
        User user = new User("Student", "student@example.com", "hashed");
        String first = auth.create(user);
        String second = auth.create(user);

        assertNotNull(first);
        assertNotEquals(first, second);
        assertTrue(first.matches("[A-Za-z0-9_-]{64}"));
        ArgumentCaptor<AuthSession> captor = ArgumentCaptor.forClass(AuthSession.class);
        verify(sessions, times(2)).save(captor.capture());
        AuthSession saved = captor.getAllValues().get(0);
        assertSame(user, saved.getUser());
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now().plusDays(13)));
        assertTrue(saved.getExpiresAt().isBefore(LocalDateTime.now().plusDays(15)));
    }

    @Test
    void authenticatesOnlyValidUnexpiredActiveBearerSessions() {
        User user = new User("Student", "student@example.com", "hashed");
        AuthSession valid = new AuthSession("valid-token", user, LocalDateTime.now().plusMinutes(5));
        when(sessions.findByToken("valid-token")).thenReturn(Optional.of(valid));
        assertEquals(user, auth.authenticate("Bearer valid-token").orElseThrow());
        assertTrue(auth.authenticate(null).isEmpty());
        assertTrue(auth.authenticate("Basic valid-token").isEmpty());

        when(sessions.findByToken("expired-token")).thenReturn(Optional.of(
                new AuthSession("expired-token", user, LocalDateTime.now().minusSeconds(1))));
        assertTrue(auth.authenticate("Bearer expired-token").isEmpty());

        user.setActive(false);
        assertTrue(auth.authenticate("Bearer valid-token").isEmpty());
    }

    @Test
    void logoutDeletesOnlyPresentedBearerToken() {
        auth.logout("Bearer session-token");
        verify(sessions).deleteByToken("session-token");
        auth.logout(null);
        auth.logout("Basic session-token");
        verifyNoMoreInteractions(sessions);
    }
}
