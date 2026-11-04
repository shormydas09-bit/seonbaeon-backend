package seonbaeon_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import seonbaeon_backend.entity.AuthSession;
import java.util.Optional;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
    Optional<AuthSession> findByToken(String token);
    void deleteByToken(String token);
}
