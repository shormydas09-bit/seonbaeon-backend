package seonbaeon_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import seonbaeon_backend.entity.*;
import seonbaeon_backend.repository.*;
import seonbaeon_backend.service.AuthService;
import seonbaeon_backend.service.NotificationService;
import org.springframework.transaction.annotation.Transactional;
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
    private final UserRepository users;
    private final NotificationService notificationService;
    private final DashboardHiddenItemRepository hiddenItems;
    private final ReviewRepository reviews;

    public WorkspaceController(AuthService auth, TeacherRepository teachers,
            TeacherVideoRepository videos, ConsultationRequestRepository consultations, UserRepository users,
            NotificationService notificationService, DashboardHiddenItemRepository hiddenItems, ReviewRepository reviews) {
        this.auth = auth; this.teachers = teachers; this.videos = videos; this.consultations = consultations;
        this.users = users; this.notificationService = notificationService; this.hiddenItems = hiddenItems; this.reviews = reviews;
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

    @GetMapping("/student/profile")
    public ResponseEntity<?> studentProfile(@RequestHeader(value="Authorization", required=false) String authorization) {
        User u = user(authorization);
        if (u == null) return unauthorized();
        if (!"STUDENT".equals(u.getRole())) return forbidden();
        return ResponseEntity.ok(studentProfileData(u));
    }

    @PutMapping("/student/profile")
    public ResponseEntity<?> updateStudentProfile(@RequestHeader(value="Authorization", required=false) String authorization,
            @RequestBody Map<String,Object> body) {
        User u = user(authorization);
        if (u == null) return unauthorized();
        if (!"STUDENT".equals(u.getRole())) return forbidden();
        String name = str(body, "name");
        if (name == null || name.isBlank() || name.length() > 100)
            return ResponseEntity.badRequest().body(Map.of("message", "Enter a valid name (maximum 100 characters)."));
        u.setName(name);
        u.setProfileImageUrl(str(body,"profileImageUrl"));
        u.setAge(intNumber(body.get("age")));
        u.setAgeGroup(str(body,"ageGroup"));
        u.setEnglishLevel(str(body,"englishLevel"));
        u.setLearningGoals(str(body,"learningGoals"));
        u.setPreferredCategory(str(body,"preferredCategory"));
        u.setInterests(str(body,"interests"));
        u.setPreferredSchedule(str(body,"preferredSchedule"));
        u.setIntroduction(str(body,"introduction"));
        u.setGuardianName(str(body,"guardianName"));
        u.setGuardianPhone(str(body,"guardianPhone"));
        return ResponseEntity.ok(studentProfileData(users.save(u)));
    }

    private Map<String,Object> studentProfileData(User u) {
        Map<String,Object> data = new LinkedHashMap<>();
        data.put("id",u.getId()); data.put("name",u.getName()); data.put("email",u.getEmail());
        data.put("role",u.getRole()); data.put("profileImageUrl",u.getProfileImageUrl());
        data.put("age",u.getAge()); data.put("ageGroup",u.getAgeGroup()); data.put("englishLevel",u.getEnglishLevel());
        data.put("learningGoals",u.getLearningGoals()); data.put("preferredCategory",u.getPreferredCategory());
        data.put("interests",u.getInterests()); data.put("preferredSchedule",u.getPreferredSchedule());
        data.put("introduction",u.getIntroduction()); data.put("guardianName",u.getGuardianName());
        data.put("guardianPhone",u.getGuardianPhone());
        return data;
    }

    @GetMapping("/student/consultations")
    public ResponseEntity<?> studentConsultations(@RequestHeader(value="Authorization", required=false) String authorization) {
        User u = user(authorization);
        if (u == null) return unauthorized();
        if (!"STUDENT".equals(u.getRole())) return forbidden();
        Set<Long> hidden = DashboardCleanupController.hiddenIds(hiddenItems, u.getId(), "CONSULTATION");
        return ResponseEntity.ok(consultations.findByEmailIgnoreCaseOrderByCreatedAtDesc(u.getEmail()).stream().filter(c -> !hidden.contains(c.getId())).map(this::consultationData).toList());
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
        String[] requiredFields = {"specialty", "experience", "qualification", "bio", "languages", "ageRange", "teachingMode", "availability"};
        for (String field : requiredFields) {
            String value = str(body, field);
            if (value == null || value.isBlank()) {
                String label = switch (field) {
                    case "specialty" -> "Teaching specialty";
                    case "experience" -> "Teaching experience";
                    case "qualification" -> "Qualifications";
                    case "bio" -> "Teaching introduction";
                    case "languages" -> "Languages";
                    case "ageRange" -> "Student age range";
                    case "teachingMode" -> "Teaching mode";
                    default -> "Weekly availability";
                };
                return ResponseEntity.badRequest().body(Map.of("message", label + " is required."));
            }
        }
        Double hourlyRate = number(body.get("hourlyRate"));
        Integer lessonDuration = intNumber(body.get("lessonDuration"));
        if (hourlyRate == null || hourlyRate <= 0)
            return ResponseEntity.badRequest().body(Map.of("message", "Enter a valid hourly rate greater than zero."));
        if (lessonDuration == null || lessonDuration < 15 || lessonDuration > 240)
            return ResponseEntity.badRequest().body(Map.of("message", "Lesson duration must be between 15 and 240 minutes."));
        Teacher t = teachers.findByOwnerUserId(u.getId()).orElseGet(Teacher::new);
        // Preserve admin approval when an already-approved teacher edits profile details.
        // New or previously unapproved applications remain unverified until admin review.
        boolean wasVerified = t.isVerified();
        t.setOwnerUserId(u.getId());
        t.setName(u.getName());
        t.setCategory(category);
        t.setLocation(orDefault(str(body,"location"), "Online"));
        t.setSpecialty(orDefault(str(body,"specialty"), category));
        t.setExperience(orDefault(str(body,"experience"), "Not specified"));
        if (t.getStudentCount() == null) t.setStudentCount(0);
        if (t.getRating() == null) t.setRating(0.0);
        if (t.getReviewCount() == null) t.setReviewCount(0);
        t.setTeachingStyle(orDefault(str(body,"teachingStyle"), "Supportive & Interactive"));
        t.setQualification(str(body,"qualification"));
        t.setQualificationInstitution(str(body,"qualificationInstitution"));
        t.setQualificationDocumentUrl(str(body,"qualificationDocumentUrl"));
        t.setBio(str(body,"bio"));
        t.setProfileImageUrl(str(body,"profileImageUrl"));
        t.setHourlyRate(hourlyRate);
        t.setLessonDuration(lessonDuration);
        t.setLanguages(str(body,"languages"));
        t.setAgeRange(str(body,"ageRange"));
        t.setTeachingMode(str(body,"teachingMode"));
        t.setAvailability(str(body,"availability"));
        // Restore rejected profiles for review, but never revoke an existing approval
        // merely because an approved teacher updates ordinary profile information.
        t.setAvailable(true);
        t.setVerified(wasVerified);
        // A rejected application edited and resubmitted by its owner returns to the review queue.
        t.setRejectionReason(null);
        t.setRejectedAt(null);
        t.setEmoji("👩‍🏫");
        return ResponseEntity.ok(teachers.save(t));
    }

    @PostMapping("/teacher/qualification-document/sign")
    public ResponseEntity<?> signQualificationDocumentUpload(@RequestHeader(value="Authorization", required=false) String authorization) {
        User u = user(authorization);
        if (u == null) return unauthorized();
        if (!"TEACHER".equals(u.getRole())) return forbidden();
        String cloudName = System.getenv("CLOUDINARY_CLOUD_NAME");
        String apiKey = System.getenv("CLOUDINARY_API_KEY");
        String apiSecret = System.getenv("CLOUDINARY_API_SECRET");
        if (cloudName == null || apiKey == null || apiSecret == null)
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", "Document upload is not configured yet."));
        long timestamp = System.currentTimeMillis() / 1000L;
        String publicId = "pick-my-teacher/teacher-" + u.getId() + "-qualification";
        String params = "overwrite=true&public_id=" + publicId + "&timestamp=" + timestamp;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest((params + apiSecret).getBytes(StandardCharsets.UTF_8));
            StringBuilder signature = new StringBuilder();
            for (byte value : digest) signature.append(String.format("%02x", value));
            return ResponseEntity.ok(Map.of("cloudName", cloudName, "apiKey", apiKey, "timestamp", timestamp,
                    "publicId", publicId, "signature", signature.toString()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Could not prepare document upload."));
        }
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

    @PostMapping("/teacher/demo-video/sign")
    public ResponseEntity<?> signDemoVideoUpload(@RequestHeader(value="Authorization", required=false) String authorization) {
        User u = user(authorization);
        if (u == null) return unauthorized();
        if (!"TEACHER".equals(u.getRole())) return forbidden();
        Teacher t = teachers.findByOwnerUserId(u.getId()).orElse(null);
        if (t == null) return ResponseEntity.badRequest().body(Map.of("message", "Save your teacher profile first."));
        if (videos.findByTeacherId(t.getId()).size() >= 2)
            return ResponseEntity.badRequest().body(Map.of("message", "A maximum of two demo videos is allowed."));
        String cloudName = System.getenv("CLOUDINARY_CLOUD_NAME");
        String apiKey = System.getenv("CLOUDINARY_API_KEY");
        String apiSecret = System.getenv("CLOUDINARY_API_SECRET");
        if (cloudName == null || apiKey == null || apiSecret == null)
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", "Video upload is not configured yet."));
        long timestamp = System.currentTimeMillis() / 1000L;
        String publicId = "pick-my-teacher/demo-videos/teacher-" + u.getId() + "-" + UUID.randomUUID();
        String params = "public_id=" + publicId + "&timestamp=" + timestamp;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest((params + apiSecret).getBytes(StandardCharsets.UTF_8));
            StringBuilder signature = new StringBuilder();
            for (byte value : digest) signature.append(String.format("%02x", value));
            return ResponseEntity.ok(Map.of("cloudName", cloudName, "apiKey", apiKey, "timestamp", timestamp,
                    "publicId", publicId, "signature", signature.toString()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Could not prepare video upload."));
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

    @DeleteMapping("/teacher/videos/{id}")
    public ResponseEntity<?> deleteTeacherVideo(
            @RequestHeader(value="Authorization", required=false) String authorization,
            @PathVariable Long id) {
        User u = user(authorization);
        if (u == null) return unauthorized();
        if (!"TEACHER".equals(u.getRole())) return forbidden();
        Teacher t = teachers.findByOwnerUserId(u.getId()).orElse(null);
        if (t == null) return ResponseEntity.notFound().build();
        TeacherVideo video = videos.findById(id).orElse(null);
        if (video == null || video.getTeacher() == null
                || !Objects.equals(video.getTeacher().getId(), t.getId())) {
            return ResponseEntity.notFound().build();
        }
        // Clear the legacy profile-level demo URL as well. Otherwise the frontend
        // restores this old URL when the teacher's saved video list becomes empty.
        if (Objects.equals(t.getDemoVideoUrl(), video.getVideoUrl())) {
            t.setDemoVideoUrl(null);
            t.setDemoVideoType("youtube");
            teachers.save(t);
        }
        videos.delete(video);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/teacher/consultations")
    public ResponseEntity<?> teacherConsultations(@RequestHeader(value="Authorization", required=false) String authorization) {
        User u = user(authorization);
        if (u == null) return unauthorized();
        if (!"TEACHER".equals(u.getRole())) return forbidden();
        Set<Long> hidden = DashboardCleanupController.hiddenIds(hiddenItems, u.getId(), "CONSULTATION");
        return ResponseEntity.ok(consultations.findByTeacherOwnerUserId(u.getId()).stream().filter(c -> !hidden.contains(c.getId())).map(this::consultationData).toList());
    }

    @PatchMapping("/teacher/consultations/{id}/status")
    public ResponseEntity<?> updateTeacherConsultation(@RequestHeader(value="Authorization", required=false) String authorization,
            @PathVariable Long id, @RequestBody Map<String,String> body) {
        User u = user(authorization);
        if (u == null) return unauthorized();
        if (!"TEACHER".equals(u.getRole())) return forbidden();
        ConsultationRequest r = consultations.findById(id).orElse(null);
        if (r == null || r.getTeacher() == null || !Objects.equals(r.getTeacher().getOwnerUserId(), u.getId()))
            return ResponseEntity.notFound().build();
        String status = body.getOrDefault("status", "").toUpperCase();
        if (!Set.of("APPROVED", "REJECTED").contains(status))
            return ResponseEntity.badRequest().body(Map.of("message", "Choose APPROVED or REJECTED."));
        if (!"PENDING".equals(r.getStatus()))
            return ResponseEntity.badRequest().body(Map.of("message", "This request has already been reviewed."));
        String reason = body.getOrDefault("rejectionReason", "").trim();
        if ("REJECTED".equals(status) && (reason == null || reason.isBlank()))
            return ResponseEntity.badRequest().body(Map.of("message", "Please provide a reason for rejecting this request."));
        r.setStatus(status);
        r.setTeacherDecision(status);
        r.setTeacherDecisionAt(java.time.LocalDateTime.now());
        r.setRejectionReason("REJECTED".equals(status) ? reason : null);
        String teacherHistory = "Teacher: " + status + " at " + java.time.LocalDateTime.now()
                + ("REJECTED".equals(status) ? " | Reason: " + reason : "");
        r.setDecisionHistory((r.getDecisionHistory() == null ? "" : r.getDecisionHistory() + "\n") + teacherHistory);
        consultations.save(r);
        users.findByEmailIgnoreCase(r.getEmail()).ifPresent(student -> notificationService.notifyUser(
                student.getId(), "Consultation " + status.toLowerCase(),
                "Your teacher " + ("APPROVED".equals(status) ? "approved" : "rejected") + " your consultation request."
                        + ("REJECTED".equals(status) ? " Reason: " + reason : ""), "CONSULTATION_STATUS"));
        return ResponseEntity.ok(consultationData(r));
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
        t.setRejectionReason(null);
        t.setRejectedAt(null);
        Teacher saved = teachers.save(t);
        notificationService.notifyUser(t.getOwnerUserId(), "Teacher profile approved",
                "Your English teacher profile is now approved and visible to students.", "TEACHER_STATUS");
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/admin/teachers/{id}/reject")
    public ResponseEntity<?> rejectTeacher(@RequestHeader(value="Authorization", required=false) String authorization,
            @PathVariable Long id, @RequestBody(required=false) Map<String,String> body) {
        User u = user(authorization);
        if (u == null) return unauthorized();
        if (!"ADMIN".equals(u.getRole())) return forbidden();
        Teacher t = teachers.findById(id).orElse(null);
        if (t == null || t.getOwnerUserId() == null) return ResponseEntity.notFound().build();
        if (t.isVerified()) return ResponseEntity.badRequest().body(Map.of("message", "This teacher is already approved."));
        String reason = body == null ? null : body.get("reason");
        if (reason == null || reason.isBlank() || reason.trim().length() > 1000)
            return ResponseEntity.badRequest().body(Map.of("message", "A rejection reason is required (maximum 1000 characters)."));
        t.setAvailable(false);
        t.setRejectionReason(reason.trim());
        t.setRejectedAt(java.time.LocalDateTime.now());
        teachers.save(t);
        notificationService.notifyUser(t.getOwnerUserId(), "Teacher application update",
                "Your teacher application was not approved. Reason: " + reason.trim(), "TEACHER_STATUS");
        return ResponseEntity.ok(Map.of("message", "Teacher application rejected."));
    }

    @PostMapping("/admin/teachers/{id}/restore")
    public ResponseEntity<?> restoreTeacherProfile(@RequestHeader(value="Authorization", required=false) String authorization,
            @PathVariable Long id) {
        User u = user(authorization);
        if (u == null) return unauthorized();
        if (!"ADMIN".equals(u.getRole())) return forbidden();
        Teacher t = teachers.findById(id).orElse(null);
        if (t == null) return ResponseEntity.notFound().build();
        t.setAvailable(true);
        t.setVerified(true);
        t.setRejectionReason(null);
        t.setRejectedAt(null);
        Teacher saved = teachers.save(t);
        if (t.getOwnerUserId() != null) {
            notificationService.notifyUser(t.getOwnerUserId(), "Teacher profile restored",
                    "An administrator restored your public teacher profile. It is now visible to students.", "TEACHER_STATUS");
        }
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/admin/teachers")
    public ResponseEntity<?> allTeacherProfiles(@RequestHeader(value="Authorization", required=false) String authorization) {
        User u = user(authorization);
        if (u == null) return unauthorized();
        if (!"ADMIN".equals(u.getRole())) return forbidden();
        return ResponseEntity.ok(teachers.findAll().stream().map(t -> {
            Map<String,Object> item = new LinkedHashMap<>();
            item.put("id", t.getId()); item.put("name", t.getName()); item.put("category", t.getCategory());
            item.put("location", t.getLocation()); item.put("experience", t.getExperience());
            item.put("verified", t.isVerified()); item.put("available", t.isAvailable());
            item.put("rejectionReason", t.getRejectionReason()); item.put("rejectedAt", t.getRejectedAt());
            item.put("ownerEmail", users.findById(t.getOwnerUserId() == null ? -1L : t.getOwnerUserId()).map(User::getEmail).orElse(""));
            return item;
        }).toList());
    }

    @PatchMapping("/admin/teachers/{id}/hide")
    public ResponseEntity<?> hideTeacherProfile(@RequestHeader(value="Authorization", required=false) String authorization,
            @PathVariable Long id) {
        User u = user(authorization);
        if (u == null) return unauthorized();
        if (!"ADMIN".equals(u.getRole())) return forbidden();
        Teacher t = teachers.findById(id).orElse(null);
        if (t == null) return ResponseEntity.notFound().build();
        t.setAvailable(false);
        teachers.save(t);
        return ResponseEntity.ok(Map.of("message", "Teacher profile hidden."));
    }

    @Transactional
    @DeleteMapping("/admin/teachers/{id}")
    public ResponseEntity<?> deleteTeacherProfilePermanently(@RequestHeader(value="Authorization", required=false) String authorization,
            @PathVariable Long id) {
        User u = user(authorization);
        if (u == null) return unauthorized();
        if (!"ADMIN".equals(u.getRole())) return forbidden();
        Teacher t = teachers.findById(id).orElse(null);
        if (t == null) return ResponseEntity.notFound().build();
        Long ownerId = t.getOwnerUserId();
        consultations.deleteByTeacherId(id);
        reviews.deleteByTeacherId(id);
        teachers.delete(t); // Teacher videos are removed through the entity's cascade; user account and lesson snapshots remain.
        if (ownerId != null) {
            notificationService.notifyUser(ownerId, "Teacher profile deleted",
                    "An administrator permanently deleted your teacher profile. Your login account and historical lesson records remain available.", "TEACHER_STATUS");
        }
        return ResponseEntity.ok(Map.of("message", "Teacher profile permanently deleted."));
    }

    @GetMapping("/admin/consultations")
    public ResponseEntity<?> adminConsultations(@RequestHeader(value="Authorization", required=false) String authorization) {
        User u = user(authorization);
        if (u == null) return unauthorized();
        if (!"ADMIN".equals(u.getRole())) return forbidden();
        Set<Long> hidden = DashboardCleanupController.hiddenIds(hiddenItems, u.getId(), "CONSULTATION");
        return ResponseEntity.ok(consultations.findAll().stream().filter(c -> !hidden.contains(c.getId())).map(this::consultationData).toList());
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
        if (!Objects.equals(r.getStatus(), status)) r.setAdminPreviousStatus(r.getStatus());
        String previousStatus = r.getStatus();
        r.setStatus(status);
        r.setAdminNote(body.getOrDefault("adminNote", "").trim());
        r.setAdminDecisionAt(java.time.LocalDateTime.now());
        String adminHistory = "Admin: " + previousStatus + " -> " + status + " at " + r.getAdminDecisionAt()
                + (r.getAdminNote() == null || r.getAdminNote().isBlank() ? "" : " | Note: " + r.getAdminNote());
        r.setDecisionHistory((r.getDecisionHistory() == null ? "" : r.getDecisionHistory() + "\n") + adminHistory);
        consultations.save(r);
        users.findByEmailIgnoreCase(r.getEmail()).ifPresent(student -> notificationService.notifyUser(
                student.getId(), "Consultation updated", "Admin changed your consultation status from "
                        + previousStatus + " to " + status + ".", "CONSULTATION_STATUS"));
        if (r.getTeacher() != null && r.getTeacher().getOwnerUserId() != null) {
            notificationService.notifyUser(r.getTeacher().getOwnerUserId(), "Consultation updated",
                    "Admin changed a consultation status from " + previousStatus + " to " + status + ".", "CONSULTATION_STATUS");
        }
        return ResponseEntity.ok(consultationData(r));
    }

    private Map<String,Object> consultationData(ConsultationRequest r) {
        Map<String,Object> data = new LinkedHashMap<>();
        data.put("id",r.getId()); data.put("studentName",r.getStudentName()); data.put("email",r.getEmail());
        if (r.getTeacher() != null) { data.put("teacherId", r.getTeacher().getId()); data.put("teacherName", r.getTeacher().getName()); data.put("teacherCategory", r.getTeacher().getCategory()); data.put("teacherLocation", r.getTeacher().getLocation()); }
        data.put("phone",r.getPhone()); data.put("preferredDate",r.getPreferredDate());
        data.put("preferredTime",r.getPreferredTime()); data.put("message",r.getMessage());
        data.put("status",r.getStatus()); data.put("createdAt",r.getCreatedAt());
        data.put("rejectionReason",r.getRejectionReason()); data.put("teacherDecision",r.getTeacherDecision());
        data.put("teacherDecisionAt",r.getTeacherDecisionAt()); data.put("adminPreviousStatus",r.getAdminPreviousStatus());
        data.put("adminNote",r.getAdminNote()); data.put("adminDecisionAt",r.getAdminDecisionAt()); data.put("decisionHistory",r.getDecisionHistory());
        users.findByEmailIgnoreCase(r.getEmail()).ifPresent(student -> { data.put("studentUserId", student.getId()); data.put("studentProfile", studentProfileData(student)); });
        return data;
    }

    private static String str(Map<String,Object> m, String k) {
        Object v=m.get(k); return v==null ? null : String.valueOf(v).trim();
    }
    private static String orDefault(String s, String d) { return s==null || s.isBlank() ? d : s; }
    private static Double number(Object v) { try { return v==null || String.valueOf(v).isBlank() ? null : Double.valueOf(String.valueOf(v)); } catch(Exception e){ return null; } }
    private static Integer intNumber(Object v) { try { return v==null || String.valueOf(v).isBlank() ? null : Integer.valueOf(String.valueOf(v)); } catch(Exception e){ return null; } }
}
