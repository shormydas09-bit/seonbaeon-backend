package seonbaeon_backend.controller;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import seonbaeon_backend.entity.*;
import seonbaeon_backend.repository.*;
import seonbaeon_backend.service.AuthService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkspaceExtrasControllerTest {
    private AuthService auth;
    private UserRepository users;
    private TeacherRepository teachers;
    private ConsultationRequestRepository consultations;
    private LessonRepository lessons;
    private ReviewRepository reviews;
    private StudentProgressNoteRepository progress;
    private TeacherAvailabilitySlotRepository availability;
    private WorkspaceExtrasController controller;

    @BeforeEach
    void setUp() {
        auth = mock(AuthService.class);
        users = mock(UserRepository.class);
        teachers = mock(TeacherRepository.class);
        consultations = mock(ConsultationRequestRepository.class);
        lessons = mock(LessonRepository.class);
        reviews = mock(ReviewRepository.class);
        progress = mock(StudentProgressNoteRepository.class);
        availability = mock(TeacherAvailabilitySlotRepository.class);
        controller = new WorkspaceExtrasController(auth, users, teachers, consultations,
                lessons, reviews, progress, availability, mock(EntityManager.class));
    }

    @Test
    void rejectsUnauthenticatedAndWrongRoleAccessToTeacherProgress() {
        when(auth.authenticate(null)).thenReturn(Optional.empty());
        assertEquals(401, controller.teacherProgress(null).getStatusCode().value());

        User student = new User("Student", "student@example.com", "hash");
        when(auth.authenticate("Bearer student")).thenReturn(Optional.of(student));
        assertEquals(403, controller.teacherProgress("Bearer student").getStatusCode().value());
        verifyNoInteractions(progress);
    }

    @Test
    void rejectsStudentAccessToAdminUserList() {
        User student = new User("Student", "student@example.com", "hash");
        when(auth.authenticate("Bearer student")).thenReturn(Optional.of(student));
        assertEquals(403, controller.adminUsers("Bearer student").getStatusCode().value());
        verifyNoInteractions(users);
    }

    @Test
    void teacherCannotEditAnotherTeachersProgressNote() {
        User teacher = new User("Teacher", "teacher@example.com", "hash");
        teacher.setRole("TEACHER");
        when(auth.authenticate("Bearer teacher")).thenReturn(Optional.of(teacher));
        StudentProgressNote note = new StudentProgressNote();
        note.setTeacherUserId(999L);
        when(progress.findById(42L)).thenReturn(Optional.of(note));

        assertEquals(403, controller.editProgress("Bearer teacher", 42L,
                java.util.Map.of("subject", "Reading", "note", "Progress")).getStatusCode().value());
        verify(progress, never()).save(any());
    }

    @Test
    void teacherCannotDeleteAnotherTeachersAvailability() {
        User teacher = new User("Teacher", "teacher@example.com", "hash");
        teacher.setRole("TEACHER");
        when(auth.authenticate("Bearer teacher")).thenReturn(Optional.of(teacher));
        TeacherAvailabilitySlot slot = new TeacherAvailabilitySlot();
        slot.setTeacherUserId(999L);
        when(availability.findById(77L)).thenReturn(Optional.of(slot));

        assertEquals(403, controller.deleteAvailability("Bearer teacher", 77L).getStatusCode().value());
        verify(availability, never()).delete(any(TeacherAvailabilitySlot.class));
    }
}
