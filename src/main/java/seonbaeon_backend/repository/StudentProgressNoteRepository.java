package seonbaeon_backend.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import seonbaeon_backend.entity.StudentProgressNote;
public interface StudentProgressNoteRepository extends JpaRepository<StudentProgressNote,Long> {
    List<StudentProgressNote> findByStudentUserIdOrderByUpdatedAtDesc(Long studentUserId);
    List<StudentProgressNote> findByTeacherUserIdOrderByUpdatedAtDesc(Long teacherUserId);
    List<StudentProgressNote> findByTeacherUserIdAndStudentUserIdOrderByUpdatedAtDesc(Long teacherUserId,Long studentUserId);
}