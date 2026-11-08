package seonbaeon_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import seonbaeon_backend.entity.Teacher;
import seonbaeon_backend.entity.TeacherVideo;
import seonbaeon_backend.repository.TeacherRepository;
import seonbaeon_backend.repository.TeacherVideoRepository;
import seonbaeon_backend.service.AuthService;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/teachers")
@CrossOrigin(origins = {"https://seonbaeon-frontend.onrender.com", "http://localhost:5173", "http://127.0.0.1:5173"})
public class TeacherController {

    private static final Set<String> ENGLISH_CATEGORIES = Set.of(
            "Kids English",
            "Phonics",
            "Speaking",
            "Reading & Writing",
            "Grammar",
            "Storytelling",
            "School English"
    );

    private static final Set<String> ALLOWED_VIEWER_ROLES = Set.of("STUDENT", "TEACHER", "ADMIN");

    private boolean canViewTeachers(String authorization) {
        return authService.authenticate(authorization)
                .map(user -> ALLOWED_VIEWER_ROLES.contains(user.getRole()))
                .orElse(false);
    }

    private final TeacherRepository teacherRepository;
    private final TeacherVideoRepository teacherVideoRepository;
    private final AuthService authService;

    public TeacherController(
            TeacherRepository teacherRepository,
            TeacherVideoRepository teacherVideoRepository,
            AuthService authService
    ) {
        this.teacherRepository = teacherRepository;
        this.teacherVideoRepository = teacherVideoRepository;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<Teacher>> getTeachers(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!canViewTeachers(authorization)) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(teacherRepository.findAll().stream()
                .filter(Teacher::isAvailable)
                .filter(teacher -> ENGLISH_CATEGORIES.contains(teacher.getCategory()))
                .filter(teacher -> teacher.getOwnerUserId() == null || teacher.isVerified())
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Teacher> getTeacher(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!canViewTeachers(authorization)) return ResponseEntity.status(401).build();
        return teacherRepository.findById(id)
                .filter(Teacher::isAvailable)
                .filter(teacher -> ENGLISH_CATEGORIES.contains(teacher.getCategory()))
                .filter(teacher -> teacher.getOwnerUserId() == null || teacher.isVerified())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{teacherId}/videos")
    public ResponseEntity<List<TeacherVideo>> getTeacherVideos(
            @PathVariable Long teacherId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (!canViewTeachers(authorization)) return ResponseEntity.status(401).build();
        return teacherRepository.findById(teacherId)
                .filter(Teacher::isAvailable)
                .filter(teacher -> ENGLISH_CATEGORIES.contains(teacher.getCategory()))
                .filter(teacher -> teacher.getOwnerUserId() == null || teacher.isVerified())
                .map(teacher -> ResponseEntity.ok(
                        teacherVideoRepository.findByTeacherId(teacherId)
                ))
                .orElse(ResponseEntity.notFound().build());
    }
} 