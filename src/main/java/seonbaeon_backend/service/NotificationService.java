package seonbaeon_backend.service;

import org.springframework.stereotype.Service;
import seonbaeon_backend.entity.UserNotification;
import seonbaeon_backend.repository.UserNotificationRepository;

@Service
public class NotificationService {
    private final UserNotificationRepository notifications;
    public NotificationService(UserNotificationRepository notifications){this.notifications=notifications;}
    public void notifyUser(Long userId, String title, String message, String type) {
        if(userId != null) notifications.save(new UserNotification(userId, title, message, type));
    }
}