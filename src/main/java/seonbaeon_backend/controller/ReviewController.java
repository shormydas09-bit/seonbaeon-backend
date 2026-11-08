package seonbaeon_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import seonbaeon_backend.entity.Review;
import seonbaeon_backend.entity.Teacher;
import seonbaeon_backend.entity.User;
import seonbaeon_backend.repository.ConsultationRequestRepository;
import seonbaeon_backend.repository.ReviewRepository;
import seonbaeon_backend.repository.TeacherRepository;
import seonbaeon_backend.service.AuthService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teachers")
@CrossOrigin(origins = {"https://seonbaeon-frontend.onrender.com", "http://localhost:5173", "http://127.0.0.1:5173"})
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final TeacherRepository teacherRepository;
    private final ConsultationRequestRepository consultationRepository;
    private final AuthService authService;

    public ReviewController(
            ReviewRepository reviewRepository,
            TeacherRepository teacherRepository,
            ConsultationRequestRepository consultationRepository,
            AuthService authService
    ) {
        this.reviewRepository = reviewRepository;
        this.teacherRepository = teacherRepository;
        this.consultationRepository = consultationRepository;
        this.authService = authService;
    }

    @GetMapping("/{teacherId}/reviews")
    public List<Review> getReviews(@PathVariable Long teacherId) {
        return reviewRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId).stream().filter(review -> !review.isHidden()).toList();
    }

    @GetMapping("/{teacherId}/reviews/eligibility")
    public ResponseEntity<?> getReviewEligibility(
            @PathVariable Long teacherId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        User student = authService.authenticate(authorization).orElse(null);
        if (student == null || !"STUDENT".equalsIgnoreCase(student.getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please log in with a Student account to review this teacher."));
        }

        if (!teacherRepository.existsById(teacherId)) {
            return ResponseEntity.notFound().build();
        }

        boolean eligible = consultationRepository
                .existsByTeacherIdAndEmailIgnoreCaseAndStatusIn(
                        teacherId, student.getEmail(), List.of("COMPLETED"));

        boolean alreadyReviewed = reviewRepository
                .existsByTeacherIdAndReviewerUserId(teacherId, student.getId());

        return ResponseEntity.ok(Map.of(
                "eligible", eligible,
                "alreadyReviewed", alreadyReviewed
        ));
    }


    @PostMapping("/{teacherId}/reviews/{reviewId}/report")
    public ResponseEntity<?> reportReview(@PathVariable Long teacherId, @PathVariable Long reviewId,
            @RequestHeader(value="Authorization", required=false) String authorization,
            @RequestBody Map<String,Object> body) {
        User student = authService.authenticate(authorization).orElse(null);
        if (student == null || !"STUDENT".equalsIgnoreCase(student.getRole()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","Please log in as a student to report a review."));
        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null || review.getTeacher() == null || !review.getTeacher().getId().equals(teacherId))
            return ResponseEntity.notFound().build();
        if (review.getReviewerUserId() != null && review.getReviewerUserId().equals(student.getId()))
            return ResponseEntity.badRequest().body(Map.of("message","You cannot report your own review."));
        String reason = String.valueOf(body.getOrDefault("reason","")).trim();
        if (reason.isBlank() || reason.length() > 1000)
            return ResponseEntity.badRequest().body(Map.of("message","Please provide a report reason (up to 1000 characters)."));
        if (review.isReported())
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message","This review has already been reported."));
        review.setReported(true);
        review.setReportReason(reason);
        reviewRepository.save(review);
        return ResponseEntity.ok(Map.of("message","Review reported for administrator moderation."));
    }

    @PostMapping("/{teacherId}/reviews")
    public ResponseEntity<?> addReview(
            @PathVariable Long teacherId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body
    ) {
        User student = authService.authenticate(authorization).orElse(null);
        if (student == null || !"STUDENT".equalsIgnoreCase(student.getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please log in with a Student account to submit a review."));
        }

        Teacher teacher = teacherRepository.findById(teacherId).orElse(null);
        if (teacher == null) {
            return ResponseEntity.notFound().build();
        }

        boolean eligible = consultationRepository
                .existsByTeacherIdAndEmailIgnoreCaseAndStatusIn(
                        teacherId, student.getEmail(), List.of("COMPLETED"));
        if (!eligible) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You can review this teacher after completing a lesson with them."));
        }

        if (reviewRepository.existsByTeacherIdAndReviewerUserId(teacherId, student.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "You have already reviewed this teacher."));
        }

        String text = String.valueOf(body.getOrDefault("text", "")).trim();
        int rating;
        try {
            rating = Integer.parseInt(String.valueOf(body.getOrDefault("rating", "0")));
        } catch (NumberFormatException e) {
            rating = 0;
        }

        if (text.isBlank() || text.length() > 2000 || rating < 1 || rating > 5) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Review text (up to 2000 characters) and a rating from 1 to 5 are required."));
        }

        Review review = new Review(teacher, student.getName(), rating, text);
        review.setReviewerUserId(student.getId());
        Review saved = reviewRepository.save(review);

        long reviewCount = reviewRepository.countByTeacherId(teacherId);
        Double averageRating = reviewRepository.findAverageRatingByTeacherId(teacherId);
        teacher.setReviewCount((int) reviewCount);
        teacher.setRating(averageRating == null ? 0.0 : Math.round(averageRating * 100.0) / 100.0);
        teacherRepository.save(teacher);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
