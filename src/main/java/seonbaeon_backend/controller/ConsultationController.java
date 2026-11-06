package seonbaeon_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import seonbaeon_backend.entity.ConsultationRequest;
import seonbaeon_backend.entity.Teacher;
import seonbaeon_backend.repository.ConsultationRequestRepository;
import seonbaeon_backend.repository.TeacherRepository;
import seonbaeon_backend.repository.UserRepository;
import seonbaeon_backend.service.NotificationService;

import java.util.Map;

@RestController
@RequestMapping("/api/consultations")
@CrossOrigin(origins = "*")
public class ConsultationController {

    private final ConsultationRequestRepository consultationRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ConsultationController(
            ConsultationRequestRepository consultationRepository,
            TeacherRepository teacherRepository,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.consultationRepository = consultationRepository;
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        try {
            Long teacherId = Long.valueOf(String.valueOf(body.get("teacherId")));
            Teacher teacher = teacherRepository.findById(teacherId).orElse(null);

            if (teacher == null) {
                return ResponseEntity.notFound().build();
            }

            String studentName = String.valueOf(body.getOrDefault("studentName", "")).trim();
            String email = String.valueOf(body.getOrDefault("email", "")).trim();
            String phone = String.valueOf(body.getOrDefault("phone", "")).trim();
            String date = String.valueOf(body.getOrDefault("preferredDate", ""));
            String time = String.valueOf(body.getOrDefault("preferredTime", ""));
            String message = String.valueOf(body.getOrDefault("message", "")).trim();

            if (studentName.isBlank() || email.isBlank() || date.isBlank() || time.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Name, email, preferred date and preferred time are required."));
            }

            ConsultationRequest request = new ConsultationRequest();
            request.setTeacher(teacher);
            request.setStudentName(studentName);
            request.setEmail(email);
            request.setPhone(phone);
            request.setPreferredDate(java.time.LocalDate.parse(date));
            request.setPreferredTime(java.time.LocalTime.parse(time));
            request.setMessage(message);
            request.setStatus("PENDING");

            ConsultationRequest saved = consultationRepository.save(request);
            if (teacher.getOwnerUserId() != null) {
                notificationService.notifyUser(teacher.getOwnerUserId(), "New consultation request",
                        studentName + " requested a consultation for " + date + " at " + time + ".",
                        "CONSULTATION_REQUEST");
            }
            userRepository.findByEmailIgnoreCase(email).ifPresent(student ->
                    notificationService.notifyUser(student.getId(), "Request submitted",
                            "Your consultation request to " + teacher.getName() + " was submitted.",
                            "CONSULTATION_REQUEST"));

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "id", saved.getId(),
                    "status", saved.getStatus(),
                    "message", "Consultation request saved successfully."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Please check the consultation form and try again."));
        }
    }
}
