package seonbaeon_backend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import seonbaeon_backend.entity.ConsultationRequest;
import seonbaeon_backend.entity.Teacher;
import seonbaeon_backend.entity.User;
import seonbaeon_backend.repository.*;
import seonbaeon_backend.service.AuthService;
import seonbaeon_backend.service.NotificationService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Controller-level end-to-end regression for the student request -> teacher approval -> teacher roster path. */
class StudentTeacherWorkflowTest {
    private AuthService auth;
    private ConsultationRequestRepository requests;
    private TeacherRepository teachers;
    private UserRepository users;
    private NotificationService notifications;
    private User student;
    private User teacherUser;
    private Teacher teacher;
    private AtomicReference<ConsultationRequest> saved;
    private ConsultationController consultationController;
    private WorkspaceExtrasController workspaceController;

    @BeforeEach
    void setUp() {
        auth = mock(AuthService.class);
        requests = mock(ConsultationRequestRepository.class);
        teachers = mock(TeacherRepository.class);
        users = mock(UserRepository.class);
        notifications = mock(NotificationService.class);
        student = new User("Test Student", "qa-student@example.test", "hash");
        student.setRole("STUDENT");
        ReflectionTestUtils.setField(student, "id", 101L);
        teacherUser = new User("Test Teacher", "qa-teacher@example.test", "hash");
        teacherUser.setRole("TEACHER");
        ReflectionTestUtils.setField(teacherUser, "id", 202L);
        teacher = new Teacher();
        ReflectionTestUtils.setField(teacher, "id", 303L);
        teacher.setName("QA English Teacher");
        teacher.setVerified(true);
        teacher.setAvailable(true);
        teacher.setOwnerUserId(202L);
        when(auth.authenticate("Bearer student-qa")).thenReturn(Optional.of(student));
        when(auth.authenticate("Bearer teacher-qa")).thenReturn(Optional.of(teacherUser));
        when(teachers.findById(303L)).thenReturn(Optional.of(teacher));
        when(users.findByEmailIgnoreCase("qa-student@example.test")).thenReturn(Optional.of(student));
        saved = new AtomicReference<>();
        when(requests.save(any(ConsultationRequest.class))).thenAnswer(invocation -> {
            ConsultationRequest request = invocation.getArgument(0);
            ReflectionTestUtils.setField(request, "id", 404L);
            saved.set(request);
            return request;
        });
        when(requests.findById(404L)).thenAnswer(invocation -> Optional.ofNullable(saved.get()));
        when(requests.findByTeacherOwnerUserId(202L)).thenAnswer(invocation ->
                saved.get() == null ? List.of() : List.of(saved.get()));

        consultationController = new ConsultationController(requests, teachers, users, notifications, auth);
        workspaceController = new WorkspaceExtrasController(auth, users, teachers, requests,
                mock(LessonRepository.class), mock(ReviewRepository.class),
                mock(StudentProgressNoteRepository.class), mock(TeacherAvailabilitySlotRepository.class),
                mock(jakarta.persistence.EntityManager.class));
    }

    @Test
    void studentCanRequestTeacherAndTeacherCanApproveAndSeeStudentConnection() {
        var submitted = consultationController.create("Bearer student-qa", Map.of(
                "teacherId", 303, "studentName", "Test Student",
                "email", "qa-student@example.test", "message", "I want to improve speaking."));
        assertEquals(HttpStatus.CREATED, submitted.getStatusCode());
        assertEquals("PENDING", saved.get().getStatus());

        var approved = workspaceController.updateTeacherConsultation("Bearer teacher-qa", 404L,
                Map.of("status", "APPROVED"));
        assertEquals(HttpStatus.OK, approved.getStatusCode());
        assertEquals("APPROVED", saved.get().getStatus());

        var roster = workspaceController.teacherStudents("Bearer teacher-qa");
        assertEquals(HttpStatus.OK, roster.getStatusCode());
        List<?> students = (List<?>) roster.getBody();
        assertNotNull(students);
        assertEquals(1, students.size());
        assertTrue(students.get(0).toString().contains("qa-student@example.test"));
        verify(requests, times(2)).save(any(ConsultationRequest.class));
    }

    @Test
    void studentCannotRequestUnapprovedTeacherOrSpoofAnotherEmail() {
        teacher.setVerified(false);
        var unapproved = consultationController.create("Bearer student-qa", Map.of(
                "teacherId", 303, "studentName", "Test Student",
                "email", "qa-student@example.test"));
        assertEquals(HttpStatus.CONFLICT, unapproved.getStatusCode());

        teacher.setVerified(true);
        var spoofed = consultationController.create("Bearer student-qa", Map.of(
                "teacherId", 303, "studentName", "Test Student",
                "email", "victim@example.test"));
        assertEquals(HttpStatus.BAD_REQUEST, spoofed.getStatusCode());
        verify(requests, never()).save(any());
    }
}
