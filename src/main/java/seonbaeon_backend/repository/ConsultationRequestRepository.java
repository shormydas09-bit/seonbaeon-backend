package seonbaeon_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import seonbaeon_backend.entity.ConsultationRequest;
import java.util.List;

public interface ConsultationRequestRepository extends JpaRepository<ConsultationRequest, Long> {
    List<ConsultationRequest> findByTeacherOwnerUserId(Long ownerUserId);
    List<ConsultationRequest> findByEmailIgnoreCaseOrderByCreatedAtDesc(String email);
}
