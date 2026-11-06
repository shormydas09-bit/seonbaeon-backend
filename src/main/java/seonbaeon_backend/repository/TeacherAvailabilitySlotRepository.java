package seonbaeon_backend.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import seonbaeon_backend.entity.TeacherAvailabilitySlot;
public interface TeacherAvailabilitySlotRepository extends JpaRepository<TeacherAvailabilitySlot,Long> {
    List<TeacherAvailabilitySlot> findByTeacherUserIdAndStartsAtBetweenOrderByStartsAtAsc(Long teacherUserId,LocalDateTime from,LocalDateTime to);
    List<TeacherAvailabilitySlot> findByTeacherUserIdOrderByStartsAtAsc(Long teacherUserId);
}