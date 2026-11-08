package seonbaeon_backend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import seonbaeon_backend.entity.AuthSession;
import seonbaeon_backend.entity.Teacher;
import seonbaeon_backend.entity.User;
import seonbaeon_backend.repository.TeacherRepository;
import seonbaeon_backend.repository.TeacherVideoRepository;
import seonbaeon_backend.service.AuthService;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeacherControllerAuthTest {
    private AuthService auth;
    private TeacherRepository teachers;
    private TeacherVideoRepository videos;
    private TeacherController controller;

    @BeforeEach
    void setUp() {
        auth = mock(AuthService.class);
        teachers = mock(TeacherRepository.class);
        videos = mock(TeacherVideoRepository.class);
        controller = new TeacherController(teachers, videos, auth);
        User member = new User("QA Member", "qa-member@example.test", "hash");
        member.setRole("STUDENT");
        when(auth.authenticate("Bearer valid-session")).thenReturn(Optional.of(member));
        User teacherViewer = new User("QA Teacher", "qa-teacher@example.test", "hash");
        teacherViewer.setRole("TEACHER");
        User adminViewer = new User("QA Admin", "qa-admin@example.test", "hash");
        adminViewer.setRole("ADMIN");
        User unknownViewer = new User("QA Other", "qa-other@example.test", "hash");
        unknownViewer.setRole("GUEST");
        when(auth.authenticate("Bearer teacher-session")).thenReturn(Optional.of(teacherViewer));
        when(auth.authenticate("Bearer admin-session")).thenReturn(Optional.of(adminViewer));
        when(auth.authenticate("Bearer guest-session")).thenReturn(Optional.of(unknownViewer));
        when(auth.authenticate(null)).thenReturn(Optional.empty());
        Teacher teacher = new Teacher();
        teacher.setName("QA English Teacher");
        teacher.setCategory("Speaking");
        teacher.setAvailable(true);
        teacher.setVerified(true);
        when(teachers.findAll()).thenReturn(List.of(teacher));
        when(teachers.findById(1L)).thenReturn(Optional.of(teacher));
    }

    @Test
    void directoryRejectsRequestsWithoutAnAuthenticatedSession() {
        assertEquals(401, controller.getTeachers(null).getStatusCode().value());
        verifyNoInteractions(teachers);
    }

    @Test
    void directoryAllowsAuthenticatedMembers() {
        var response = controller.getTeachers("Bearer valid-session");
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void individualProfileAndVideosRejectAnonymousRequests() {
        assertEquals(401, controller.getTeacher(1L, null).getStatusCode().value());
        assertEquals(401, controller.getTeacherVideos(1L, null).getStatusCode().value());
    }

    @Test
    void individualProfileAllowsAuthenticatedMembers() {
        var response = controller.getTeacher(1L, "Bearer valid-session");
        assertEquals(200, response.getStatusCode().value());
        assertEquals("QA English Teacher", response.getBody().getName());
    }
}
