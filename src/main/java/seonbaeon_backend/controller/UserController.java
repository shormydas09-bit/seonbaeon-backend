package seonbaeon_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import seonbaeon_backend.entity.User;
import seonbaeon_backend.repository.UserRepository;
import seonbaeon_backend.service.PasswordResetService;
import seonbaeon_backend.service.AuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"https://seonbaeon-frontend.onrender.com", "http://localhost:5173", "http://127.0.0.1:5173"})
public class UserController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final PasswordResetService passwordResetService;
    private final AuthService authService;

    public UserController(
            UserRepository userRepository,
            PasswordResetService passwordResetService,
            AuthService authService
    ) {
        this.userRepository = userRepository;
        this.passwordResetService = passwordResetService;
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        if (user.getName() == null || user.getName().isBlank()
                || user.getEmail() == null || user.getEmail().isBlank()
                || user.getName().trim().length() > 100
                || user.getEmail().trim().length() > 254
                || !user.getEmail().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
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
        String requestedRole = user.getRole() == null ? "STUDENT" : user.getRole().trim().toUpperCase();
        if (!requestedRole.equals("STUDENT") && !requestedRole.equals("TEACHER")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Only Student or Teacher accounts can be created here."));
        }
        user.setRole(requestedRole);

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

        if (!user.isActive()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "This account has been suspended. Please contact support."));
        }

        String requestedRole = loginRequest.getRole();
        if (requestedRole != null && !requestedRole.isBlank()
                && !requestedRole.trim().equalsIgnoreCase(user.getRole() == null ? "STUDENT" : user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "This account is not registered as the selected account type."));
        }

        String token = authService.create(user);
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole() == null ? "STUDENT" : user.getRole(),
                "token", token
        ));
    }
    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return authService.authenticate(authorization)
                .map(user -> ResponseEntity.ok(Map.of(
                        "id", user.getId(), "name", user.getName(),
                        "email", user.getEmail(), "role", user.getRole()
                )))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Please log in again.")));
    }


    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, String> request) {
        User user = authService.authenticate(authorization).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Please log in again."));
        }
        String name = request.get("name");
        if (name == null || name.isBlank() || name.trim().length() > 100) {
            return ResponseEntity.badRequest().body(Map.of("message", "Please enter a name (up to 100 characters)."));
        }
        user.setName(name.trim());
        User saved = userRepository.save(user);
        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "name", saved.getName(),
                "email", saved.getEmail(),
                "role", saved.getRole()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
        return ResponseEntity.ok(Map.of("message", "Logged out."));
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

