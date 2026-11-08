package seonbaeon_backend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import seonbaeon_backend.entity.ConsultationRequest;
import seonbaeon_backend.entity.Teacher;
import seonbaeon_backend.entity.User;
import seonbaeon_backend.repository.ConsultationRequestRepository;
import seonbaeon_backend.repository.TeacherRepository;
import seonbaeon_backend.repository.UserRepository;
import seonbaeon_backend.service.AuthService;
import seonbaeon_backend.service.NotificationService;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConsultationControllerTest {
    private ConsultationRequestRepository requests;
    private TeacherRepository teachers;
    private UserRepository users;
    private NotificationService notifications;
    private AuthService auth;
    private ConsultationController controller;
    private User student;
    private Teacher teacher;

    @BeforeEach
    void setUp() {
        requests = mock(ConsultationRequestRepository.class);
        teachers = mock(TeacherRepository.class);
        users = mock(UserRepository.class);
        notifications = mock(NotificationService.class);
        auth = mock(AuthService.class);
        controller = new ConsultationController(requests, teachers, users, notifications, auth);
        student = new User("Student", "student@example.com", "hashed");
        student.setRole("STUDENT");
        teacher = new Teacher();
        teacher.setName("English Teacher");
        teacher.setVerified(true);
        teacher.setAvailable(true);
        when(auth.authenticate("Bearer student-token")).thenReturn(Optional.of(student));
        when(teachers.findById(7L)).thenReturn(Optional.of(teacher));
        when(requests.save(any(ConsultationRequest.class))).thenAnswer(invocation -> {
            ConsultationRequest saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 123L);
            return saved;
        });
    }

    @Test
    void requiresAuthenticatedStudent() {
        when(auth.authenticate(null)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.UNAUTHORIZED, controller.create(null, Map.of()).getStatusCode());
        User teacherUser = new User("Teacher", "teacher@example.com", "hashed");
        teacherUser.setRole("TEACHER");
        when(auth.authenticate("Bearer teacher-token")).thenReturn(Optional.of(teacherUser));
        assertEquals(HttpStatus.FORBIDDEN,
                controller.create("Bearer teacher-token", Map.of()).getStatusCode());
        verifyNoInteractions(requests);
    }

    @Test
    void rejectsEmailMismatchAndUnapprovedTeacher() {
        var mismatch = controller.create("Bearer student-token",
                Map.of("teacherId", 7, "studentName", "Student", "email", "other@example.com"));
        assertEquals(HttpStatus.BAD_REQUEST, mismatch.getStatusCode());
        teacher.setVerified(false);
        var unapproved = controller.create("Bearer student-token",
                Map.of("teacherId", 7, "studentName", "Student", "email", "student@example.com"));
        assertEquals(HttpStatus.CONFLICT, unapproved.getStatusCode());
        verify(requests, never()).save(any());
    }

    @Test
    void acceptsOptionalDateAndTimeForAuthenticatedStudent() {
        var response = controller.create("Bearer student-token",
                Map.of("teacherId", 7, "studentName", "Student", "email", "student@example.com"));
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(requests).save(argThat(request ->
                request.getPreferredDate() == null && request.getPreferredTime() == null
                        && "PENDING".equals(request.getStatus())
                        && "student@example.com".equals(request.getEmail())));
    }

    @Test
    void rejectsPastDatesAndMalformedDatesWithoutSaving() {
        var past = controller.create("Bearer student-token",
                Map.of("teacherId", 7, "studentName", "Student", "email", "student@example.com",
                        "preferredDate", "2000-01-01"));
        assertEquals(HttpStatus.BAD_REQUEST, past.getStatusCode());
        var malformed = controller.create("Bearer student-token",
                Map.of("teacherId", 7, "studentName", "Student", "email", "student@example.com",
                        "preferredDate", "not-a-date"));
        assertEquals(HttpStatus.BAD_REQUEST, malformed.getStatusCode());
        verify(requests, never()).save(any());
    }
    @Test
    void markupInConsultationMessageIsHandledAsPlainJsonData() {
        String markup = "<img src=x onerror=alert(1)>";
        var response = controller.create("Bearer student-token",
                Map.of("teacherId", 7, "studentName", "Student",
                        "email", "student@example.com", "message", markup));
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(requests).save(argThat(request -> markup.equals(request.getMessage())));
        // The API returns JSON data; UI rendering must use normal React text interpolation,
        // never dangerouslySetInnerHTML for user-controlled profile or message fields.
    }

    @Test
    void consultationRejectsOversizedAndMalformedEmailInputs() {
        var oversized = controller.create("Bearer student-token",
                Map.of("teacherId", 7, "studentName", "Student", "email", "student@example.com",
                        "message", "x".repeat(2001)));
        var malformed = controller.create("Bearer student-token",
                Map.of("teacherId", 7, "studentName", "Student", "email", "not-an-email"));
        assertEquals(HttpStatus.BAD_REQUEST, oversized.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, malformed.getStatusCode());
        verify(requests, never()).save(any());
    }
}
