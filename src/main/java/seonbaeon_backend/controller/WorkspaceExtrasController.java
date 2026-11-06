package seonbaeon_backend.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import seonbaeon_backend.entity.*;
import seonbaeon_backend.repository.*;
import seonbaeon_backend.service.AuthService;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/workspace")
@CrossOrigin(origins="*")
public class WorkspaceExtrasController {
    private final AuthService auth;
    private final UserRepository users;
    private final TeacherRepository teachers;
    private final ConsultationRequestRepository consultations;
    private final LessonRepository lessons;
    private final ReviewRepository reviews;
    private final StudentProgressNoteRepository progress;
    private final TeacherAvailabilitySlotRepository availability;

    public WorkspaceExtrasController(AuthService auth,UserRepository users,TeacherRepository teachers,
        ConsultationRequestRepository consultations,LessonRepository lessons,ReviewRepository reviews,
        StudentProgressNoteRepository progress,TeacherAvailabilitySlotRepository availability) {
        this.auth=auth;this.users=users;this.teachers=teachers;this.consultations=consultations;
        this.lessons=lessons;this.reviews=reviews;this.progress=progress;this.availability=availability;
    }
    private User current(String h){return auth.authenticate(h).orElse(null);}
    private ResponseEntity<?> noAuth(){return ResponseEntity.status(401).body(Map.of("message","Please log in again."));}
    private ResponseEntity<?> forbidden(){return ResponseEntity.status(403).body(Map.of("message","You do not have permission for this action."));}
    private LocalDateTime date(Object v){try{return LocalDateTime.parse(String.valueOf(v));}catch(Exception e){return null;}}
    private boolean linked(Long teacherId,Long studentId) {
        User student=users.findById(studentId).orElse(null);
        if(student==null||!"STUDENT".equals(student.getRole())) return false;
        return consultations.findByTeacherOwnerUserId(teacherId).stream()
            .anyMatch(c->c.getEmail().equalsIgnoreCase(student.getEmail()) &&
                Set.of("APPROVED","COMPLETED").contains(c.getStatus()));
    }

    @GetMapping("/teacher/students")
    public ResponseEntity<?> teacherStudents(@RequestHeader(value="Authorization",required=false) String h) {
        User u=current(h); if(u==null)return noAuth(); if(!"TEACHER".equals(u.getRole()))return forbidden();
        Map<Long,Map<String,Object>> linkedStudents=new LinkedHashMap<>();
        for(ConsultationRequest c:consultations.findByTeacherOwnerUserId(u.getId())) {
            if(!Set.of("APPROVED","COMPLETED").contains(c.getStatus())) continue;
            users.findByEmailIgnoreCase(c.getEmail()).filter(student -> "STUDENT".equals(student.getRole()))
                .ifPresent(student -> linkedStudents.put(student.getId(),Map.of("id",student.getId(),"name",student.getName(),"email",student.getEmail())));
        }
        return ResponseEntity.ok(new ArrayList<>(linkedStudents.values()));
    }

    // Teacher notes are private to the teacher and the linked student.
    @GetMapping("/teacher/progress")
    public ResponseEntity<?> teacherProgress(@RequestHeader(value="Authorization",required=false) String h) {
        User u=current(h); if(u==null)return noAuth(); if(!"TEACHER".equals(u.getRole()))return forbidden();
        return ResponseEntity.ok(progress.findByTeacherUserIdOrderByUpdatedAtDesc(u.getId()));
    }
    @PostMapping("/teacher/progress")
    public ResponseEntity<?> saveProgress(@RequestHeader(value="Authorization",required=false) String h,@RequestBody Map<String,Object> body) {
        User u=current(h); if(u==null)return noAuth(); if(!"TEACHER".equals(u.getRole()))return forbidden();
        Long studentId; try{studentId=Long.valueOf(String.valueOf(body.get("studentUserId")));}catch(Exception e){return ResponseEntity.badRequest().body(Map.of("message","Choose a valid student."));}
        if(!linked(u.getId(),studentId))return forbidden();
        String subject=String.valueOf(body.getOrDefault("subject","")).trim();
        String note=String.valueOf(body.getOrDefault("note","")).trim();
        if(subject.isBlank()||subject.length()>120||note.isBlank()||note.length()>3000)
            return ResponseEntity.badRequest().body(Map.of("message","A subject and progress note (up to 3000 characters) are required."));
        StudentProgressNote item=new StudentProgressNote(); item.setTeacherUserId(u.getId());item.setStudentUserId(studentId);item.setSubject(subject);item.setNote(note);
        return ResponseEntity.status(201).body(progress.save(item));
    }
    @PutMapping("/teacher/progress/{id}")
    public ResponseEntity<?> editProgress(@RequestHeader(value="Authorization",required=false) String h,@PathVariable Long id,@RequestBody Map<String,Object> body) {
        User u=current(h);if(u==null)return noAuth();if(!"TEACHER".equals(u.getRole()))return forbidden();
        StudentProgressNote item=progress.findById(id).orElse(null);if(item==null)return ResponseEntity.notFound().build();
        if(!Objects.equals(item.getTeacherUserId(),u.getId()))return forbidden();
        String subject=String.valueOf(body.getOrDefault("subject","")).trim(),note=String.valueOf(body.getOrDefault("note","")).trim();
        if(subject.isBlank()||subject.length()>120||note.isBlank()||note.length()>3000)return ResponseEntity.badRequest().body(Map.of("message","A subject and progress note are required."));
        item.setSubject(subject);item.setNote(note);return ResponseEntity.ok(progress.save(item));
    }
    @DeleteMapping("/teacher/progress/{id}")
    public ResponseEntity<?> deleteProgress(@RequestHeader(value="Authorization",required=false) String h,@PathVariable Long id) {
        User u=current(h);if(u==null)return noAuth();if(!"TEACHER".equals(u.getRole()))return forbidden();
        StudentProgressNote item=progress.findById(id).orElse(null);if(item==null)return ResponseEntity.notFound().build();
        if(!Objects.equals(item.getTeacherUserId(),u.getId()))return forbidden();progress.delete(item);return ResponseEntity.noContent().build();
    }
    @GetMapping("/student/progress")
    public ResponseEntity<?> studentProgress(@RequestHeader(value="Authorization",required=false) String h) {
        User u=current(h);if(u==null)return noAuth();if(!"STUDENT".equals(u.getRole()))return forbidden();
        return ResponseEntity.ok(progress.findByStudentUserIdOrderByUpdatedAtDesc(u.getId()));
    }

    @GetMapping("/teacher/availability")
    public ResponseEntity<?> getAvailability(@RequestHeader(value="Authorization",required=false) String h) {
        User u=current(h);if(u==null)return noAuth();if(!"TEACHER".equals(u.getRole()))return forbidden();
        return ResponseEntity.ok(availability.findByTeacherUserIdOrderByStartsAtAsc(u.getId()));
    }
    @PostMapping("/teacher/availability")
    public ResponseEntity<?> addAvailability(@RequestHeader(value="Authorization",required=false) String h,@RequestBody Map<String,Object> body) {
        User u=current(h);if(u==null)return noAuth();if(!"TEACHER".equals(u.getRole()))return forbidden();
        LocalDateTime start=date(body.get("startsAt")),end=date(body.get("endsAt"));
        if(start==null||end==null||!start.isAfter(LocalDateTime.now())||!end.isAfter(start))return ResponseEntity.badRequest().body(Map.of("message","Choose a future start and an end time after it."));
        if(availability.findByTeacherUserIdOrderByStartsAtAsc(u.getId()).stream().anyMatch(s->start.isBefore(s.getEndsAt())&&end.isAfter(s.getStartsAt())))
            return ResponseEntity.status(409).body(Map.of("message","This time overlaps an existing availability slot."));
        TeacherAvailabilitySlot slot=new TeacherAvailabilitySlot();slot.setTeacherUserId(u.getId());slot.setStartsAt(start);slot.setEndsAt(end);slot.setAvailable(true);
        slot.setNote(String.valueOf(body.getOrDefault("note","")).trim());return ResponseEntity.status(201).body(availability.save(slot));
    }
    @DeleteMapping("/teacher/availability/{id}")
    public ResponseEntity<?> deleteAvailability(@RequestHeader(value="Authorization",required=false) String h,@PathVariable Long id) {
        User u=current(h);if(u==null)return noAuth();if(!"TEACHER".equals(u.getRole()))return forbidden();
        TeacherAvailabilitySlot slot=availability.findById(id).orElse(null);if(slot==null)return ResponseEntity.notFound().build();
        if(!Objects.equals(slot.getTeacherUserId(),u.getId()))return forbidden();availability.delete(slot);return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/users")
    public ResponseEntity<?> adminUsers(@RequestHeader(value="Authorization",required=false) String h) {
        User u=current(h);if(u==null)return noAuth();if(!"ADMIN".equals(u.getRole()))return forbidden();
        return ResponseEntity.ok(users.findAll().stream().map(x->Map.of("id",x.getId(),"name",x.getName(),"email",x.getEmail(),"role",x.getRole())).toList());
    }
    @PatchMapping("/admin/users/{id}/role")
    public ResponseEntity<?> updateUserRole(@RequestHeader(value="Authorization",required=false) String h,@PathVariable Long id,@RequestBody Map<String,Object> body) {
        User admin=current(h);if(admin==null)return noAuth();if(!"ADMIN".equals(admin.getRole()))return forbidden();
        User target=users.findById(id).orElse(null);if(target==null)return ResponseEntity.notFound().build();
        String role=String.valueOf(body.getOrDefault("role","")).toUpperCase(Locale.ROOT);
        if(!Set.of("STUDENT","TEACHER","ADMIN").contains(role)||Objects.equals(target.getId(),admin.getId()))
            return ResponseEntity.badRequest().body(Map.of("message","Choose a valid role. You cannot change your own role."));
        target.setRole(role);users.save(target);return ResponseEntity.ok(Map.of("id",target.getId(),"name",target.getName(),"email",target.getEmail(),"role",target.getRole()));
    }

    @GetMapping("/admin/reviews")
    public ResponseEntity<?> adminReviews(@RequestHeader(value="Authorization",required=false) String h) {
        User u=current(h);if(u==null)return noAuth();if(!"ADMIN".equals(u.getRole()))return forbidden();
        return ResponseEntity.ok(reviews.findAllWithTeacher().stream().map(r->Map.of("id",r.getId(),"teacherId",r.getTeacher().getId(),"teacherName",r.getTeacher().getName(),"studentName",r.getStudentName(),"rating",r.getRating(),"text",r.getText(),"createdAt",r.getCreatedAt(),"reported",r.isReported(),"reportReason",r.getReportReason()==null?"":r.getReportReason())).toList());
    }
    @DeleteMapping("/admin/reviews/{id}")
    public ResponseEntity<?> deleteReview(@RequestHeader(value="Authorization",required=false) String h,@PathVariable Long id) {
        User u=current(h);if(u==null)return noAuth();if(!"ADMIN".equals(u.getRole()))return forbidden();
        Review review=reviews.findById(id).orElse(null);if(review==null)return ResponseEntity.notFound().build();
        Long teacherId=review.getTeacher().getId();reviews.delete(review);
        long count=reviews.countByTeacherId(teacherId);Double avg=reviews.findAverageRatingByTeacherId(teacherId);
        Teacher t=teachers.findById(teacherId).orElse(null);if(t!=null){t.setReviewCount((int)count);t.setRating(avg==null?0.0:Math.round(avg*100.0)/100.0);teachers.save(t);}
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/teacher/analytics")
    public ResponseEntity<?> teacherAnalytics(@RequestHeader(value="Authorization",required=false) String h) {
        User u=current(h);if(u==null)return noAuth();if(!"TEACHER".equals(u.getRole()))return forbidden();
        Teacher t=teachers.findByOwnerUserId(u.getId()).orElse(null);if(t==null)return ResponseEntity.ok(Map.of("consultations",0,"lessons",0,"completedLessons",0,"reviews",0,"averageRating",0));
        var own=consultations.findByTeacherOwnerUserId(u.getId());var teacherLessons=lessons.findByTeacherUserIdOrderByScheduledAtDesc(u.getId());
        return ResponseEntity.ok(Map.of("consultations",own.size(),"pendingConsultations",own.stream().filter(x->"PENDING".equals(x.getStatus())).count(),
            "lessons",teacherLessons.size(),"completedLessons",teacherLessons.stream().filter(x->"COMPLETED".equals(x.getStatus())).count(),
            "reviews",reviews.countByTeacherId(t.getId()),"averageRating",t.getRating(),"students",own.stream().map(x->x.getEmail().toLowerCase(Locale.ROOT)).distinct().count()));
    }

    @GetMapping("/admin/overview")
    public ResponseEntity<?> adminOverview(@RequestHeader(value="Authorization",required=false) String h) {
        User u=current(h);if(u==null)return noAuth();if(!"ADMIN".equals(u.getRole()))return forbidden();
        var allUsers=users.findAll();var allConsultations=consultations.findAll();var allLessons=lessons.findAll();
        return ResponseEntity.ok(Map.of("users",allUsers.size(),"students",allUsers.stream().filter(x->"STUDENT".equals(x.getRole())).count(),
            "teachers",allUsers.stream().filter(x->"TEACHER".equals(x.getRole())).count(),"teacherProfiles",teachers.count(),
            "consultations",allConsultations.size(),"pendingConsultations",allConsultations.stream().filter(x->"PENDING".equals(x.getStatus())).count(),
            "lessons",allLessons.size(),"completedLessons",allLessons.stream().filter(x->"COMPLETED".equals(x.getStatus())).count(),
            "reviews",reviews.count()));
    }
}
