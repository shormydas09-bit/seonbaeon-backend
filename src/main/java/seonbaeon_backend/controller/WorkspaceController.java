package seonbaeon_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import seonbaeon_backend.entity.*;
import seonbaeon_backend.repository.*;
import seonbaeon_backend.service.AuthService;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@RestController
@RequestMapping("/api/workspace")
@CrossOrigin(origins = "*")
public class WorkspaceController {
    private static final Set<String> CATEGORIES = Set.of(
        "Kids English", "Phonics", "Speaking", "Reading & Writing",
        "Grammar", "Storytelling", "School English"
    );
    private final AuthService auth;
    private final TeacherRepository teachers;
    private final TeacherVideoRepository videos;
    private final ConsultationRequestRepository consultations;

    public WorkspaceController(AuthService auth, TeacherRepository teachers,
            TeacherVideoRepository videos, ConsultationRequestRepository consultations) {
        this.auth = auth; this.teachers = teachers; this.videos = videos; this.consultations = consultations;
    }

    private User user(String authorization) {
        return auth.authenticate(authorization).orElse(null);
    }
    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Please log in again."));
    }
    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "You do not have permission for this action."));
    }

    @GetMapping("/student/consultations")
    public ResponseEntity<?> studentConsultations(@RequestHeader(value="Authorization", required=false) String authorization) {
        User u = user(authorization);
        if (u == null) return unauthorized();
        if (!"STUDENT".equals(u.getRole())) return forbidden();
        return ResponseEntity.ok(consultations.findByEmailIgnoreCaseOrderByCreatedAtDesc(u.getEmail()));
    }

    @GetMapping("/teacher/profile")
    public ResponseEntity<?> myTeacherProfile(@RequestHeader(value="Authorization", required=false) String authorization) {
        User u = user(authorization);
        if (u == null) return unauthorized();
        if (!"TEACHER".equals(u.getRole())) return forbidden();
        return teachers.findByOwnerUserId(u.getId()).<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.ok(Map.of("profile", "NOT_CREATED")));
    }

    @PutMapping("/teacher/profile")
    public ResponseEntity<?> saveTeacherProfile(@RequestHeader(value="Authorization", required=false) String authorization,
            @RequestBody Map<String,Object> body) {
        User u = user(authorization);
        if (u == null) return unauthorized();
        if (!"TEACHER".equals(u.getRole())) return forbidden();
        String category = str(body, "category");
        if (!CATEGORIES.contains(category)) return ResponseEntity.badRequest().body(Map.of("message", "Choose a valid English teaching category."));
        Teacher t = teachers.findByOwnerUserId(u.getId()).orElseGet(Teacher::new);
        t.setOwnerUserId(u.getId());
        t.setName(u.getName());
        t.setCategory(category);
        t.setLocation(orDefault(str(body,"location"), "Online"));
        t.setSpecialty(orDefault(str(body,"specialty"), category));
        t.setExperience(orDefault(str(body,"experience"), "Not specified"));
        t.setStudentCount(0);
        if (t.getRating() == null) t.setRating(0.0);
        if (t.getReviewCount() == null) t.setReviewCount(0);
        t.setTeachingStyle(orDefault(str(body,"teachingStyle"), "Supportive & Interactive"));
        t.setQualification(str(body,"qualification"));
        t.setBio(str(body,"bio"));
        t.setProfileImageUrl(str(body,"profileImageUrl"));
        t.setHourlyRate(number(body.get("hourlyRate")));
        t.setLessonDuration(intNumber(body.get("lessonDuration")));
        t.setLanguages(str(body,"languages"));
        t.setAgeRange(str(body,"ageRange"));
        t.setTeachingMode(str(body,"teachingMode"));
        t.setAvailability(str(body,"availability"));
        t.setAvailable(true);
        t.setEmoji("👩‍🏫");
        t.setVerified(false);
        return ResponseEntity.ok(teachers.save(t));
    }

    @PostMapping("/teacher/profile-image/sign")
    public ResponseEntity<?> signProfileImageUpload(@RequestHeader(value="Authorization", required=false) String authorization) {
        User u = user(authorization);
        if (u == null) return unauthorized();
        if (!"TEACHER".equals(u.getRole())) return forbidden();
        String cloudName = System.getenv("CLOUDINARY_CLOUD_NAME");
        String apiKey = System.getenv("CLOUDINARY_API_KEY");
        String apiSecret = System.getenv("CLOUDINARY_API_SECRET");
        if (cloudName == null || apiKey == null || apiSecret == null)
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", "Image upload is not configured yet."));
        long timestamp = System.currentTimeMillis() / 1000L;
        String publicId = "pick-my-teacher/teacher-" + u.getId() + "-profile";
        String params = "overwrite=true&public_id=" + publicId + "&timestamp=" + timestamp;
        try {
            String toSign = params + apiSecret;
            byte[] digest = MessageDigest.getInstance("SHA-1")
                    .digest(toSign.getBytes(StandardCharsets.UTF_8));
            StringBuilder signature = new StringBuilder();
            for (byte b : digest) signature.append(String.format("%02x", b));
            return ResponseEntity.ok(Map.of("cloudName", cloudName, "apiKey", apiKey,
                    "timestamp", timestamp, "publicId", publicId, "signature", signature.toString()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Could not prepare image upload."));
        }
    }

    @PostMapping("/teacher/videos")
    public ResponseEntity<?> addVideo(@RequestHeader(value="Authorization", required=false) String authorization,
            @RequestBody Map<String,String> body) {
        User u = user(authorization);
        if (u == null) return unauthorized();
        if (!"TEACHER".equals(u.getRole())) return forbidden();
        Teacher t = teachers.findByOwnerUserId(u.getId()).orElse(null);
        if (t == null) return ResponseEntity.badRequest().body(Map.of("message", "Save your teacher profile first."));
        List<TeacherVideo> current = videos.findByTeacherId(t.getId());
        if (current.size() >= 2) return ResponseEntity.badRequest().body(Map.of("message", "A maximum of two demo videos is allowed."));
        String title = body.getOrDefault("title", "").trim();
        String url = body.getOrDefault("videoUrl", "").trim();
        if (title.isBlank() || !(url.startsWith("https://") || url.startsWith("http://")))
            return ResponseEntity.badRequest().body(Map.of("message", "A video title and valid http(s) URL are required."));
        TeacherVideo v = new TeacherVideo(title, body.getOrDefault("description",""), url,
                body.getOrDefault("videoType","youtube"), body.getOrDefault("thumbnailUrl",""), t);
        return ResponseEntity.status(HttpStatus.CREATED).body(videos.save(v));
    }

    @GetMapping("/teacher/videos")
    public ResponseEntity<?> myVideos(@RequestHeader(value="Authorization", required=false) String authorization) {
        User u = user(authorization);
        if (u == null) return unauthorized();
        if (!"TEACHER".equals(u.getRole())) return forbidden();
        Teacher t = teachers.findByOwnerUserId(u.getId()).orElse(null);
        return ResponseEntity.ok(t == null ? List.of() : videos.findByTeacherId(t.getId()));
    }

    @GetMapping("/teacher/consultations")
    public ResponseEntity<?> teacherConsultations(@RequestHeader(value="Authorization", required=false) String authorization) {
        User u = user(authorization);
        if (u == null) return unauthorized();
        if (!"TEACHER".equals(u.getRole())) return forbidden();
        return ResponseEntity.ok(consultations.findByTeacherOwnerUserId(u.getId()));
    }

    @GetMapping("/admin/pending-teachers")
    public ResponseEntity<?> pendingTeachers(@RequestHeader(value="Authorization", required=false) String authorization) {
        User u = user(authorization);
        if (u == null) return unauthorized();
        if (!"ADMIN".equals(u.getRole())) return forbidden();
        return ResponseEntity.ok(teachers.findByVerifiedFalse().stream().filter(t -> t.getOwnerUserId() != null && t.isAvailable()).toList());
    }

    @PostMapping("/admin/teachers/{id}/approve")
    public ResponseEntity<?> approveTeacher(@RequestHeader(value="Authorization", required=false) String authorization,
            @PathVariable Long id) {
        User u = user(authorization);
        if (u == null) return unauthorized();
        if (!"ADMIN".equals(u.getRole())) return forbidden();
        Teacher t = teachers.findById(id).orElse(null);
        if (t == null || t.getOwnerUserId() == null) return ResponseEntity.notFound().build();
        t.setVerified(true);
        return ResponseEntity.ok(teachers.save(t));
    }

    @PostMapping("/admin/teachers/{id}/reject")
    public ResponseEntity<?> rejectTeacher(@RequestHeader(value="Authorization", required=false) String authorization,
            @PathVariable Long id) {
        User u = user(authorization);
        if (u == null) return unauthorized();
        if (!"ADMIN".equals(u.getRole())) return forbidden();
        Teacher t = teachers.findById(id).orElse(null);
        if (t == null || t.getOwnerUserId() == null || t.isVerified()) return ResponseEntity.notFound().build();
        // Keep the application and its videos in the database, but hide it from public listings and pending review.
        t.setAvailable(false);
        return ResponseEntity.ok(teachers.save(t));
    }

    @GetMapping("/admin/consultations")
    public ResponseEntity<?> adminConsultations(@RequestHeader(value="Authorization", required=false) String authorization) {
        User u = user(authorization);
        if (u == null) return unauthorized();
        if (!"ADMIN".equals(u.getRole())) return forbidden();
        return ResponseEntity.ok(consultations.findAll());
    }

    @PatchMapping("/admin/consultations/{id}/status")
    public ResponseEntity<?> updateConsultation(@RequestHeader(value="Authorization", required=false) String authorization,
            @PathVariable Long id, @RequestBody Map<String,String> body) {
        User u = user(authorization);
        if (u == null) return unauthorized();
        if (!"ADMIN".equals(u.getRole())) return forbidden();
        ConsultationRequest r = consultations.findById(id).orElse(null);
        if (r == null) return ResponseEntity.notFound().build();
        String status = body.getOrDefault("status", "").toUpperCase();
        if (!Set.of("PENDING","APPROVED","REJECTED","COMPLETED").contains(status))
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid status."));
        r.setStatus(status);
        return ResponseEntity.ok(consultations.save(r));
    }

    private static String str(Map<String,Object> m, String k) {
        Object v=m.get(k); return v==null ? null : String.valueOf(v).trim();
    }
    private static String orDefault(String s, String d) { return s==null || s.isBlank() ? d : s; }
    private static Double number(Object v) { try { return v==null || String.valueOf(v).isBlank() ? null : Double.valueOf(String.valueOf(v)); } catch(Exception e){ return null; } }
    private static Integer intNumber(Object v) { try { return v==null || String.valueOf(v).isBlank() ? null : Integer.valueOf(String.valueOf(v)); } catch(Exception e){ return null; } }
}
