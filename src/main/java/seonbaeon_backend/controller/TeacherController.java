package seonbaeon_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import seonbaeon_backend.entity.Teacher;
import seonbaeon_backend.entity.TeacherVideo;
import seonbaeon_backend.repository.TeacherRepository;
import seonbaeon_backend.repository.TeacherVideoRepository;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/teachers")
@CrossOrigin(origins = "*")
public class TeacherController {

    private static final Set<String> ENGLISH_CATEGORIES = Set.of(
            "Kids English",
            "Speaking",
            "Reading & Writing",
            "Grammar",
            "Storytelling",
            "School English"
    );

    private final TeacherRepository teacherRepository;
    private final TeacherVideoRepository teacherVideoRepository;

    public TeacherController(
            TeacherRepository teacherRepository,
            TeacherVideoRepository teacherVideoRepository
    ) {
        this.teacherRepository = teacherRepository;
        this.teacherVideoRepository = teacherVideoRepository;
    }

    @GetMapping
    public List<Teacher> getTeachers() {
        return teacherRepository.findAll().stream()
                .filter(teacher -> ENGLISH_CATEGORIES.contains(teacher.getCategory()))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Teacher> getTeacher(@PathVariable Long id) {
        return teacherRepository.findById(id)
                .filter(teacher -> ENGLISH_CATEGORIES.contains(teacher.getCategory()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{teacherId}/videos")
    public ResponseEntity<List<TeacherVideo>> getTeacherVideos(
            @PathVariable Long teacherId
    ) {
        return teacherRepository.findById(teacherId)
                .filter(teacher -> ENGLISH_CATEGORIES.contains(teacher.getCategory()))
                .map(teacher -> ResponseEntity.ok(
                        teacherVideoRepository.findByTeacherId(teacherId)
                ))
                .orElse(ResponseEntity.notFound().build());
    }
} 