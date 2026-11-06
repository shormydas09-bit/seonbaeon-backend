package seonbaeon_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import seonbaeon_backend.entity.Lesson;
import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByStudentUserIdOrderByScheduledAtDesc(Long studentUserId);
    List<Lesson> findByTeacherUserIdOrderByScheduledAtDesc(Long teacherUserId);
    Optional<Lesson> findByConsultationId(Long consultationId);
    boolean existsByConsultationId(Long consultationId);
}
