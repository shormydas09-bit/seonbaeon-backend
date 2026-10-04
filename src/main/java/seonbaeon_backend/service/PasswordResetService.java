package seonbaeon_backend.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seonbaeon_backend.entity.PasswordResetToken;
import seonbaeon_backend.entity.User;
import seonbaeon_backend.repository.PasswordResetTokenRepository;
import seonbaeon_backend.repository.UserRepository;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class PasswordResetService {

    private static final int TOKEN_EXPIRY_MINUTES = 30;
    private final String frontendUrl = System.getenv("FRONTEND_URL") == null ? "http://localhost:5173" : System.getenv("FRONTEND_URL");

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JavaMailSender mailSender;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            JavaMailSender mailSender
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.mailSender = mailSender;
    }

    @Transactional
    public void createResetRequest(String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);

        if (user == null) {
            return;
        }

        passwordResetTokenRepository.deleteByUserId(user.getId());

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        LocalDateTime expiresAt =
                LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES);

        PasswordResetToken resetToken =
                new PasswordResetToken(token, user, expiresAt);

        passwordResetTokenRepository.save(resetToken);

        String encodedToken =
                URLEncoder.encode(token, StandardCharsets.UTF_8);

        String resetLink =
                frontendUrl + "/reset-password?token=" + encodedToken;

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(System.getenv("MAIL_USERNAME"));
        message.setTo(user.getEmail());
        message.setSubject("Pick My Teacher - Reset Your Password");

        message.setText(
                "Hello " + user.getName() + ",\n\n"
                + "We received a request to reset your Pick My Teacher password.\n\n"
                + "Use the link below to create a new password:\n\n"
                + resetLink + "\n\n"
                + "This link will expire in 30 minutes and can only be used once.\n\n"
                + "If you did not request a password reset, you can safely ignore this email.\n\n"
                + "Pick My Teacher"
        );

        mailSender.send(message);
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()
                || newPassword == null || newPassword.length() < 6) {
            return false;
        }

        PasswordResetToken resetToken =
                passwordResetTokenRepository.findByToken(token).orElse(null);

        if (resetToken == null
                || resetToken.isUsed()
                || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }

        User user = resetToken.getUser();

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        return true;
    }
}
