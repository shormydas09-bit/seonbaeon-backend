package seonbaeon_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import seonbaeon_backend.entity.UserNotification;
import java.util.List;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    List<UserNotification> findTop50ByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);
    long countByRecipientUserIdAndReadFalse(Long recipientUserId);
}