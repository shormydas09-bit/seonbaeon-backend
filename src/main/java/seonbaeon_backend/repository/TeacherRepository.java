package seonbaeon_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import seonbaeon_backend.entity.Teacher;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
}
