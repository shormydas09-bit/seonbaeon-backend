package seonbaeon_backend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import seonbaeon_backend.entity.Teacher;
import seonbaeon_backend.entity.TeacherVideo;
import seonbaeon_backend.entity.User;
import seonbaeon_backend.repository.*;
import seonbaeon_backend.service.AuthService;
import seonbaeon_backend.service.NotificationService;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WorkspaceControllerTest {
    private AuthService auth;
    private TeacherRepository teachers;
    private TeacherVideoRepository videos;
    private ConsultationRequestRepository consultations;
    private UserRepository users;
    private NotificationService notifications;
    private DashboardHiddenItemRepository hidden;
    private ReviewRepository reviews;
    private WorkspaceController controller;
    private User teacherUser;
    private Teacher teacher;

    @BeforeEach
    void setUp() {
        auth = mock(AuthService.class);
        teachers = mock(TeacherRepository.class);
        videos = mock(TeacherVideoRepository.class);
        consultations = mock(ConsultationRequestRepository.class);
        users = mock(UserRepository.class);
        notifications = mock(NotificationService.class);
        hidden = mock(DashboardHiddenItemRepository.class);
        reviews = mock(ReviewRepository.class);
        controller = new WorkspaceController(auth, teachers, videos, consultations, users,
                notifications, hidden, reviews);
        teacherUser = new User("Teacher", "teacher@example.com", "hash");
        teacherUser.setRole("TEACHER");
        org.springframework.test.util.ReflectionTestUtils.setField(teacherUser, "id", 12L);
        teacher = new Teacher();
        org.springframework.test.util.ReflectionTestUtils.setField(teacher, "id", 22L);
        teacher.setOwnerUserId(12L);
        when(auth.authenticate("Bearer teacher-token")).thenReturn(Optional.of(teacherUser));
        when(teachers.findByOwnerUserId(12L)).thenReturn(Optional.of(teacher));
        when(videos.findByTeacherId(22L)).thenReturn(List.of());
    }

    @Test
    void teacherProfileRejectsUnknownAndNonEnglishCategoriesBeforeSaving() {
        var invalid = controller.saveTeacherProfile("Bearer teacher-token",
                java.util.Map.of("category", "Mathematics"));
        assertEquals(HttpStatus.BAD_REQUEST, invalid.getStatusCode());
        verify(teachers, never()).save(any());

        var nonEnglish = controller.saveTeacherProfile("Bearer teacher-token",
                java.util.Map.of("category", "Science"));
        assertEquals(HttpStatus.BAD_REQUEST, nonEnglish.getStatusCode());
        verify(teachers, never()).save(any());
    }

    @Test
    void videoEndpointRejectsJavascriptAndDataUrls() {
        var javascript = controller.addVideo("Bearer teacher-token",
                java.util.Map.of("title", "Demo", "videoUrl", "javascript:alert(1)"));
        var data = controller.addVideo("Bearer teacher-token",
                java.util.Map.of("title", "Demo", "videoUrl", "data:text/html,<script>alert(1)</script>"));
        assertEquals(HttpStatus.BAD_REQUEST, javascript.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, data.getStatusCode());
        verify(videos, never()).save(any());
    }

    @Test
    void videoEndpointEnforcesMaximumOfTwoItems() {
        when(videos.findByTeacherId(22L)).thenReturn(List.of(
                new TeacherVideo("one", "", "https://example.com/one", "external", "", teacher),
                new TeacherVideo("two", "", "https://example.com/two", "external", "", teacher)));
        var response = controller.addVideo("Bearer teacher-token",
                java.util.Map.of("title", "third", "videoUrl", "https://example.com/three"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(videos, never()).save(any());
    }

    @Test
    void qualificationUploadSigningRequiresTeacherRole() {
        User student = new User("Student", "student@example.com", "hash");
        student.setRole("STUDENT");
        when(auth.authenticate("Bearer student-token")).thenReturn(Optional.of(student));
        var response = controller.signQualificationDocumentUpload("Bearer student-token");
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void teacherDemoUploadSigningRequiresSavedProfileAndRespectsVideoLimit() {
        when(teachers.findByOwnerUserId(12L)).thenReturn(Optional.empty());
        var noProfile = controller.signDemoVideoUpload("Bearer teacher-token");
        assertEquals(HttpStatus.BAD_REQUEST, noProfile.getStatusCode());

        when(teachers.findByOwnerUserId(12L)).thenReturn(Optional.of(teacher));
        when(videos.findByTeacherId(22L)).thenReturn(List.of(
                new TeacherVideo("one", "", "https://example.com/one", "external", "", teacher),
                new TeacherVideo("two", "", "https://example.com/two", "external", "", teacher)));
        var full = controller.signDemoVideoUpload("Bearer teacher-token");
        assertEquals(HttpStatus.BAD_REQUEST, full.getStatusCode());
    }
    @Test
    void teacherProfileRejectsUnsafeUploadedDocumentAndImageUrls() {
        java.util.Map<String,Object> body = new java.util.HashMap<>();
        body.put("category", "Speaking");
        body.put("specialty", "Speaking");
        body.put("experience", "3 years");
        body.put("qualification", "TESOL");
        body.put("bio", "English teacher");
        body.put("languages", "English");
        body.put("ageRange", "Kids");
        body.put("teachingMode", "Online");
        body.put("availability", "Weekends");
        body.put("hourlyRate", 20);
        body.put("lessonDuration", 30);
        body.put("qualificationDocumentUrl", "javascript:alert(1)");
        assertEquals(HttpStatus.BAD_REQUEST,
                controller.saveTeacherProfile("Bearer teacher-token", body).getStatusCode());
        body.put("qualificationDocumentUrl", "https://cdn.example.test/certificate.pdf");
        body.put("profileImageUrl", "data:image/svg+xml,<svg onload=alert(1)>");
        assertEquals(HttpStatus.BAD_REQUEST,
                controller.saveTeacherProfile("Bearer teacher-token", body).getStatusCode());
        verify(teachers, never()).save(any());
    }
}
