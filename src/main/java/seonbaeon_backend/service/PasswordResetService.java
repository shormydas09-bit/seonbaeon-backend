package seonbaeon_backend.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seonbaeon_backend.entity.PasswordResetToken;
import seonbaeon_backend.entity.User;
import seonbaeon_backend.repository.PasswordResetTokenRepository;
import seonbaeon_backend.repository.UserRepository;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class PasswordResetService {

    private static final int TOKEN_EXPIRY_MINUTES = 30;

    private final String frontendUrl =
            System.getenv("FRONTEND_URL") == null
                    ? "http://localhost:5173"
                    : System.getenv("FRONTEND_URL");

    private final String brevoApiKey =
            System.getenv("BREVO_API_KEY");

    private final String brevoSenderEmail =
            System.getenv("BREVO_SENDER_EMAIL") == null
                    ? "shormydas09@gmail.com"
                    : System.getenv("BREVO_SENDER_EMAIL");

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    private final SecureRandom secureRandom = new SecureRandom();

    private final HttpClient httpClient =
            HttpClient.newHttpClient();

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Transactional
    public void createResetRequest(String email) {
        String normalizedEmail =
                email == null ? "" : email.trim().toLowerCase();

        User user =
                userRepository.findByEmailIgnoreCase(normalizedEmail)
                        .orElse(null);

        if (user == null) {
            return;
        }

        passwordResetTokenRepository.deleteByUserId(user.getId());

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String token =
                Base64.getUrlEncoder()
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

        sendResetEmail(
                user.getEmail(),
                user.getName(),
                resetLink
        );
    }

    private void sendResetEmail(
            String recipientEmail,
            String userName,
            String resetLink
    ) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            throw new IllegalStateException("BREVO_API_KEY is not configured.");
        }

        String safeUserName = escapeHtml(userName);
        String safeResetLink = escapeHtml(resetLink);

        String html =
                "<html><body>"
                + "<p>Hello " + safeUserName + ",</p>"
                + "<p>We received a request to reset your Pick My Teacher password.</p>"
                + "<p>Click the button below to create a new password:</p>"
                + "<p><a href=\"" + safeResetLink + "\" "
                + "style=\"display:inline-block;padding:12px 20px;"
                + "background:#2563eb;color:#ffffff;text-decoration:none;"
                + "border-radius:8px;\">Reset My Password</a></p>"
                + "<p>This link will expire in 30 minutes and can only be used once.</p>"
                + "<p>If you did not request a password reset, you can safely ignore this email.</p>"
                + "<p>Pick My Teacher</p>"
                + "</body></html>";

        String json =
                "{"
                + "\"sender\":{"
                + "\"name\":\"Pick My Teacher\","
                + "\"email\":\"" + escapeJson(brevoSenderEmail) + "\""
                + "},"
                + "\"to\":[{"
                + "\"email\":\"" + escapeJson(recipientEmail) + "\","
                + "\"name\":\"" + escapeJson(userName) + "\""
                + "}],"
                + "\"subject\":\"Pick My Teacher - Reset Your Password\","
                + "\"htmlContent\":\"" + escapeJson(html) + "\""
                + "}";

        try {
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(
                                    "https://api.brevo.com/v3/smtp/email"
                            ))
                            .header("accept", "application/json")
                            .header("api-key", brevoApiKey)
                            .header("content-type", "application/json")
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            json,
                                            StandardCharsets.UTF_8
                                    )
                            )
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString(
                                    StandardCharsets.UTF_8
                            )
                    );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Brevo email failed. HTTP "
                        + response.statusCode()
                        + ": "
                        + response.body()
                );
            }

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to send password reset email through Brevo.",
                    e
            );
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    @Transactional
    public boolean resetPassword(
            String token,
            String newPassword
    ) {
        if (token == null
                || token.isBlank()
                || newPassword == null
                || newPassword.length() < 6) {
            return false;
        }

        PasswordResetToken resetToken =
                passwordResetTokenRepository
                        .findByToken(token)
                        .orElse(null);

        if (resetToken == null
                || resetToken.isUsed()
                || resetToken.getExpiresAt()
                        .isBefore(LocalDateTime.now())) {
            return false;
        }

        User user = resetToken.getUser();

        user.setPassword(
                passwordEncoder.encode(newPassword)
        );

        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        return true;
    }
}
