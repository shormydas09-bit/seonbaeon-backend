package seonbaeon_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import seonbaeon_backend.entity.User;
import seonbaeon_backend.repository.UserRepository;
import seonbaeon_backend.service.PasswordResetService;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final PasswordResetService passwordResetService;

    public UserController(
            UserRepository userRepository,
            PasswordResetService passwordResetService
    ) {
        this.userRepository = userRepository;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        if (user.getName() == null || user.getName().isBlank()
                || user.getEmail() == null || user.getEmail().isBlank()
                || user.getPassword() == null || user.getPassword().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Name, email and a password of at least 6 characters are required."));
        }

        String email = user.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "An account with this email already exists."));
        }

        user.setName(user.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("STUDENT");

        User saved = userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", saved.getId(),
                "name", saved.getName(),
                "email", saved.getEmail(),
                "role", saved.getRole()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginRequest) {
        if (loginRequest.getEmail() == null || loginRequest.getPassword() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email and password are required."));
        }

        User user = userRepository.findByEmailIgnoreCase(loginRequest.getEmail().trim())
                .orElse(null);

        if (user == null || user.getPassword() == null
                || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Incorrect email or password."));
        }

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole() == null ? "STUDENT" : user.getRole()
        ));
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email is required."));
        }

        passwordResetService.createResetRequest(email);

        return ResponseEntity.ok(Map.of(
                "message",
                "If an account exists with this email, a password reset link has been sent."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");

        if (token == null || token.isBlank()
                || newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "A valid reset token and a password of at least 6 characters are required."
                    ));
        }

        boolean success = passwordResetService.resetPassword(token, newPassword);

        if (!success) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "This reset link is invalid or has expired."
                    ));
        }

        return ResponseEntity.ok(Map.of(
                "message",
                "Password reset successfully. You can now log in."
        ));
    }
}
