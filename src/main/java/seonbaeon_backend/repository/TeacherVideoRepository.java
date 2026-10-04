package seonbaeon_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import seonbaeon_backend.entity.TeacherVideo;

import java.util.List;

public interface TeacherVideoRepository extends JpaRepository<TeacherVideo, Long> {

    List<TeacherVideo> findByTeacherId(Long teacherId);
}