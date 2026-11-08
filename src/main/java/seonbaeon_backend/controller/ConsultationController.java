package seonbaeon_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import seonbaeon_backend.entity.ConsultationRequest;
import seonbaeon_backend.entity.Teacher;
import seonbaeon_backend.repository.ConsultationRequestRepository;
import seonbaeon_backend.repository.TeacherRepository;
import seonbaeon_backend.repository.UserRepository;
import seonbaeon_backend.service.NotificationService;
import seonbaeon_backend.service.AuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/consultations")
@CrossOrigin(origins = {"https://seonbaeon-frontend.onrender.com", "http://localhost:5173", "http://127.0.0.1:5173"})
public class ConsultationController {

    private final ConsultationRequestRepository consultationRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuthService auth;

    public ConsultationController(
            ConsultationRequestRepository consultationRepository,
            TeacherRepository teacherRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            AuthService auth
    ) {
        this.consultationRepository = consultationRepository;
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auth = auth;
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body
    ) {
        var authenticatedStudent = auth.authenticate(authorization).orElse(null);
        if (authenticatedStudent == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please log in with your Student account to request a consultation."));
        }
        if (!"STUDENT".equals(authenticatedStudent.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Only Student / Parent accounts can request a consultation."));
        }
        try {
            Long teacherId = Long.valueOf(String.valueOf(body.get("teacherId")));
            Teacher teacher = teacherRepository.findById(teacherId).orElse(null);

            if (teacher == null) {
                return ResponseEntity.notFound().build();
            }
            if (!teacher.isVerified() || !teacher.isAvailable()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "This teacher is not currently accepting lesson requests."));
            }

            String studentName = String.valueOf(body.getOrDefault("studentName", "")).trim();
            String email = String.valueOf(body.getOrDefault("email", "")).trim();
            String phone = String.valueOf(body.getOrDefault("phone", "")).trim();
            String date = String.valueOf(body.getOrDefault("preferredDate", ""));
            String time = String.valueOf(body.getOrDefault("preferredTime", ""));
            String message = String.valueOf(body.getOrDefault("message", "")).trim();

            if (studentName.isBlank() || email.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Student name and email are required."));
            }
            if (studentName.length() > 100 || email.length() > 254
                    || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                    || phone.length() > 25 || message.length() > 2000) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Please check the name, email, phone and message lengths and formats."));
            }

            if (!authenticatedStudent.getEmail().equalsIgnoreCase(email)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Use the email address connected to your Student account."));
            }
            ConsultationRequest request = new ConsultationRequest();
            request.setTeacher(teacher);
            request.setStudentName(studentName);
            request.setEmail(email);
            request.setPhone(phone);
            if (!date.isBlank()) {
                java.time.LocalDate preferredDate = java.time.LocalDate.parse(date);
                if (preferredDate.isBefore(java.time.LocalDate.now())) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Choose today or a future date for your consultation."));
                }
                request.setPreferredDate(preferredDate);
            }
            if (!time.isBlank()) {
                request.setPreferredTime(java.time.LocalTime.parse(time));
            }
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
