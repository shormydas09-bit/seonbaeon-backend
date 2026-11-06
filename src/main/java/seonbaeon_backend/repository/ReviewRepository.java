package seonbaeon_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import seonbaeon_backend.entity.Review;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);
    boolean existsByTeacherIdAndReviewerUserId(Long teacherId, Long reviewerUserId);
    long countByTeacherId(Long teacherId);

    @Query("select avg(r.rating) from Review r where r.teacher.id = :teacherId")
    Double findAverageRatingByTeacherId(@Param("teacherId") Long teacherId);
}
