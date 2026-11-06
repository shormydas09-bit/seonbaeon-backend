package seonbaeon_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import seonbaeon_backend.entity.User;
import seonbaeon_backend.entity.UserNotification;
import seonbaeon_backend.repository.UserNotificationRepository;
import seonbaeon_backend.service.AuthService;
import java.util.*;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {
    private final AuthService auth;
    private final UserNotificationRepository notifications;
    public NotificationController(AuthService auth, UserNotificationRepository notifications) {
        this.auth=auth; this.notifications=notifications;
    }
    @GetMapping
    public ResponseEntity<?> list(@RequestHeader(value="Authorization", required=false) String authorization) {
        User user=auth.authenticate(authorization).orElse(null);
        if(user==null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","Please log in again."));
        List<UserNotification> items=notifications.findTop50ByRecipientUserIdOrderByCreatedAtDesc(user.getId());
        return ResponseEntity.ok(Map.of("items",items,"unreadCount",notifications.countByRecipientUserIdAndReadFalse(user.getId())));
    }
    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markRead(@RequestHeader(value="Authorization", required=false) String authorization,
            @PathVariable Long id) {
        User user=auth.authenticate(authorization).orElse(null);
        if(user==null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","Please log in again."));
        UserNotification item=notifications.findById(id).orElse(null);
        if(item==null || !Objects.equals(item.getRecipientUserId(),user.getId())) return ResponseEntity.notFound().build();
        item.setRead(true);
        return ResponseEntity.ok(notifications.save(item));
    }
    @PatchMapping("/read-all")
    public ResponseEntity<?> markAllRead(@RequestHeader(value="Authorization", required=false) String authorization) {
        User user=auth.authenticate(authorization).orElse(null);
        if(user==null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","Please log in again."));
        List<UserNotification> items=notifications.findTop50ByRecipientUserIdOrderByCreatedAtDesc(user.getId());
        items.forEach(item->item.setRead(true));
        notifications.saveAll(items);
        return ResponseEntity.ok(Map.of("message","Notifications marked as read."));
    }
}