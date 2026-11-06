package seonbaeon_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_notifications")
public class UserNotification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long recipientUserId;
    @Column(nullable = false, length = 120) private String title;
    @Column(nullable = false, length = 1000) private String message;
    @Column(nullable = false, length = 40) private String type;
    @Column(nullable = false) private boolean read = false;
    @Column(nullable = false) private LocalDateTime createdAt;

    public UserNotification() {}
    public UserNotification(Long recipientUserId, String title, String message, String type) {
        this.recipientUserId=recipientUserId; this.title=title; this.message=message; this.type=type;
    }
    @PrePersist public void onCreate(){ if(createdAt==null) createdAt=LocalDateTime.now(); }
    public Long getId(){return id;} public Long getRecipientUserId(){return recipientUserId;}
    public String getTitle(){return title;} public String getMessage(){return message;}
    public String getType(){return type;} public boolean isRead(){return read;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public void setRead(boolean value){read=value;}
}