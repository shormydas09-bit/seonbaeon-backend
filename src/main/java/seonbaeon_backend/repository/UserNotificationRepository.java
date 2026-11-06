package seonbaeon_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import seonbaeon_backend.entity.UserNotification;
import java.util.List;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    List<UserNotification> findTop50ByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);
    long countByRecipientUserIdAndReadFalse(Long recipientUserId);
    @Modifying
    @Query("update UserNotification n set n.read = true where n.recipientUserId = :userId and n.read = false")
    int markAllReadByRecipient(@Param("userId") Long userId);
}