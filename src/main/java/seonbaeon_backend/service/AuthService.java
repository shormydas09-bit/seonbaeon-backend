package seonbaeon_backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seonbaeon_backend.entity.AuthSession;
import seonbaeon_backend.entity.User;
import seonbaeon_backend.repository.AuthSessionRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class AuthService {
    private final AuthSessionRepository sessions;
    private final SecureRandom random = new SecureRandom();
    public AuthService(AuthSessionRepository sessions) { this.sessions = sessions; }
    public String create(User user) {
        byte[] bytes = new byte[48]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.save(new AuthSession(token, user, LocalDateTime.now().plusDays(14)));
        return token;
    }
    public Optional<User> authenticate(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return Optional.empty();
        return sessions.findByToken(authorization.substring(7).trim())
            .filter(s -> s.getExpiresAt().isAfter(LocalDateTime.now()))
            .map(AuthSession::getUser)
            .filter(User::isActive);
    }
    @Transactional
    public void logout(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer "))
            sessions.deleteByToken(authorization.substring(7).trim());
    }
}
