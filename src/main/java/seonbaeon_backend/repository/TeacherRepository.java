package seonbaeon_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import seonbaeon_backend.entity.Teacher;
import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByOwnerUserId(Long ownerUserId);
    List<Teacher> findByVerifiedFalse();

    @Query("select t from Teacher t where t.available = true and t.category in :categories and (t.ownerUserId is null or t.verified = true)")
    List<Teacher> findPublicAvailableTeachersByCategories(@Param("categories") List<String> categories);
}
