package seonbaeon_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auth_sessions", indexes = @Index(name = "idx_auth_sessions_token", columnList = "token", unique = true))
public class AuthSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 100)
    private String token;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    public AuthSession() {}
    public AuthSession(String token, User user, LocalDateTime expiresAt) {
        this.token = token; this.user = user; this.expiresAt = expiresAt;
    }
    public Long getId() { return id; }
    public String getToken() { return token; }
    public User getUser() { return user; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
