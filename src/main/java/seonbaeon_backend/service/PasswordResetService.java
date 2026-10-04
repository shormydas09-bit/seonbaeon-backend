package seonbaeon_backend.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seonbaeon_backend.entity.PasswordResetToken;
import seonbaeon_backend.entity.User;
import seonbaeon_backend.repository.PasswordResetTokenRepository;
import seonbaeon_backend.repository.UserRepository;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class PasswordResetService {

    private static final int TOKEN_EXPIRY_MINUTES = 30;

    private final String frontendUrl =
            System.getenv("FRONTEND_URL") == null
                    ? "http://localhost:5173"
                    : System.getenv("FRONTEND_URL");

    private final String resendApiKey = System.getenv("RESEND_API_KEY");

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    private final SecureRandom secureRandom = new SecureRandom();

    private final HttpClient httpClient =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

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
        if (resendApiKey == null || resendApiKey.isBlank()) {
            throw new IllegalStateException(
                    "RESEND_API_KEY is not configured."
            );
        }

        String safeName = escapeHtml(userName);
        String safeLink = escapeHtml(resetLink);

        String html =
                "<!DOCTYPE html>"
                + "<html>"
                + "<body style=\"font-family: Arial, sans-serif; line-height: 1.6;\">"
                + "<h2>Reset Your Password</h2>"
                + "<p>Hello " + safeName + ",</p>"
                + "<p>We received a request to reset your "
                + "Pick My Teacher password.</p>"
                + "<p>Click the button below to create a new password:</p>"
                + "<p>"
                + "<a href=\"" + safeLink + "\" "
                + "style=\"display:inline-block;"
                + "padding:12px 20px;"
                + "background:#111827;"
                + "color:#ffffff;"
                + "text-decoration:none;"
                + "border-radius:8px;\">"
                + "Reset Password"
                + "</a>"
                + "</p>"
                + "<p>Or copy and paste this link into your browser:</p>"
                + "<p>" + safeLink + "</p>"
                + "<p>This link will expire in 30 minutes "
                + "and can only be used once.</p>"
                + "<p>If you did not request a password reset, "
                + "you can safely ignore this email.</p>"
                + "<p>Pick My Teacher</p>"
                + "</body>"
                + "</html>";

        String json =
                "{"
                + "\"from\":\"onboarding@resend.dev\","
                + "\"to\":[\"" + escapeJson(recipientEmail) + "\"],"
                + "\"subject\":\"Pick My Teacher - Reset Your Password\","
                + "\"html\":\"" + escapeJson(html) + "\""
                + "}";

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create("https://api.resend.com/emails"))
                        .timeout(Duration.ofSeconds(15))
                        .header("Authorization", "Bearer " + resendApiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                throw new IllegalStateException(
                        "Resend email failed. HTTP "
                        + response.statusCode()
                        + ": "
                        + response.body()
                );
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Resend email request was interrupted.",
                    e
            );

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not connect to Resend email API.",
                    e
            );
        }
    }

    private String escapeJson(String value) {
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
