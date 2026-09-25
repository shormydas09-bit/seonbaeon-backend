package seonbaeon_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import seonbaeon_backend.entity.Review;
import seonbaeon_backend.entity.Teacher;
import seonbaeon_backend.repository.ReviewRepository;
import seonbaeon_backend.repository.TeacherRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teachers")
@CrossOrigin(origins = "*")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final TeacherRepository teacherRepository;

    public ReviewController(ReviewRepository reviewRepository, TeacherRepository teacherRepository) {
        this.reviewRepository = reviewRepository;
        this.teacherRepository = teacherRepository;
    }

    @GetMapping("/{teacherId}/reviews")
    public List<Review> getReviews(@PathVariable Long teacherId) {
        return reviewRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId);
    }

    @PostMapping("/{teacherId}/reviews")
    public ResponseEntity<?> addReview(
            @PathVariable Long teacherId,
            @RequestBody Map<String, Object> body
    ) {
        Teacher teacher = teacherRepository.findById(teacherId).orElse(null);

        if (teacher == null) {
            return ResponseEntity.notFound().build();
        }

        String studentName = String.valueOf(body.getOrDefault("studentName", "")).trim();
        String text = String.valueOf(body.getOrDefault("text", "")).trim();

        int rating;
        try {
            rating = Integer.parseInt(String.valueOf(body.getOrDefault("rating", "0")));
        } catch (NumberFormatException e) {
            rating = 0;
        }

        if (studentName.isBlank() || text.isBlank() || rating < 1 || rating > 5) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Student name, review text and a rating from 1 to 5 are required."));
        }

        Review review = new Review(teacher, studentName, rating, text);
        Review saved = reviewRepository.save(review);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
