package seonbaeon_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import seonbaeon_backend.entity.ConsultationRequest;

public interface ConsultationRequestRepository extends JpaRepository<ConsultationRequest, Long> {
}
