package seonbaeon_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import seonbaeon_backend.entity.PasswordResetToken;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    void deleteByUserId(Long userId);
}
