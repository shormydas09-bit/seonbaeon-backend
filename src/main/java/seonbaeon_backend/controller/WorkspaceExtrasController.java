package seonbaeon_backend.controller;

import org.springframework.http.*;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
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
    private final EntityManager entityManager;

    public WorkspaceExtrasController(AuthService auth,UserRepository users,TeacherRepository teachers,
        ConsultationRequestRepository consultations,LessonRepository lessons,ReviewRepository reviews,
        StudentProgressNoteRepository progress,TeacherAvailabilitySlotRepository availability,EntityManager entityManager) {
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

    private List<Map<String,Object>> connectedStudentsFor(Long teacherUserId) {
        Map<Long,Map<String,Object>> linkedStudents=new LinkedHashMap<>();
        for(ConsultationRequest c:consultations.findByTeacherOwnerUserId(teacherUserId)) {
            String status=c.getStatus()==null?"":c.getStatus().toUpperCase(Locale.ROOT);
            if(!Set.of("APPROVED","COMPLETED").contains(status)) continue;
            users.findByEmailIgnoreCase(c.getEmail()).filter(student -> "STUDENT".equals(student.getRole()))
                .ifPresent(student -> {
                    Map<String,Object> item=linkedStudents.computeIfAbsent(student.getId(),id -> {
                        Map<String,Object> value=new LinkedHashMap<>();
                        value.put("id",student.getId());value.put("name",student.getName());value.put("email",student.getEmail());
                        value.put("hasActiveConnection",false);value.put("hasCompletedConnection",false);
                        value.put("lastActivity",c.getCreatedAt());
                        return value;
                    });
                    if("APPROVED".equals(status)) item.put("hasActiveConnection",true);
                    if("COMPLETED".equals(status)) item.put("hasCompletedConnection",true);
                    if(c.getCreatedAt()!=null && (item.get("lastActivity")==null ||
                        c.getCreatedAt().isAfter((LocalDateTime)item.get("lastActivity")))) item.put("lastActivity",c.getCreatedAt());
                });
        }
        for(Map<String,Object> item:linkedStudents.values()) {
            boolean active=Boolean.TRUE.equals(item.get("hasActiveConnection"));
            item.put("status",active?"ACTIVE":"COMPLETED");
            item.put("statusLabel",active?"Active student":"Completed student");
            item.remove("hasActiveConnection");item.remove("hasCompletedConnection");
        }
        return new ArrayList<>(linkedStudents.values());
    }

    @GetMapping("/teacher/students")
    public ResponseEntity<?> teacherStudents(@RequestHeader(value="Authorization",required=false) String h) {
        User u=current(h); if(u==null)return noAuth(); if(!"TEACHER".equals(u.getRole()))return forbidden();
        return ResponseEntity.ok(connectedStudentsFor(u.getId()));
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
        return ResponseEntity.ok(users.findAll().stream().map(x->{Map<String,Object> item=new LinkedHashMap<>();item.put("id",x.getId());item.put("name",x.getName());item.put("email",x.getEmail());item.put("role",x.getRole());item.put("active",x.isActive());return item;}).toList());
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

    @DeleteMapping("/admin/users/{id}")
    @Transactional
    public ResponseEntity<?> deleteUserAccount(@RequestHeader(value="Authorization",required=false) String h,@PathVariable Long id) {
        User admin=current(h);if(admin==null)return noAuth();if(!"ADMIN".equals(admin.getRole()))return forbidden();
        User target=users.findById(id).orElse(null);if(target==null)return ResponseEntity.notFound().build();
        if("ADMIN".equalsIgnoreCase(target.getRole())||Objects.equals(target.getId(),admin.getId()))
            return ResponseEntity.badRequest().body(Map.of("message","Admin accounts cannot be deleted from User Management."));
        Long targetId=target.getId();
        // Remove dependent records first so account deletion cannot leave broken references.
        entityManager.createQuery("delete from Lesson l where l.studentUserId=:id or l.teacherUserId=:id").setParameter("id",targetId).executeUpdate();
        entityManager.createQuery("delete from StudentProgressNote n where n.studentUserId=:id or n.teacherUserId=:id").setParameter("id",targetId).executeUpdate();
        entityManager.createQuery("delete from TeacherAvailabilitySlot a where a.teacherUserId=:id").setParameter("id",targetId).executeUpdate();
        entityManager.createQuery("delete from ConsultationRequest c where c.email=:email or c.teacher.ownerUserId=:id").setParameter("email",target.getEmail()).setParameter("id",targetId).executeUpdate();
        entityManager.createQuery("delete from Review r where r.reviewerUserId=:id").setParameter("id",targetId).executeUpdate();
        entityManager.createQuery("delete from AuthSession s where s.user.id=:id").setParameter("id",targetId).executeUpdate();
        entityManager.createQuery("delete from PasswordResetToken p where p.user.id=:id").setParameter("id",targetId).executeUpdate();
        entityManager.createQuery("delete from DashboardHiddenItem d where d.userId=:id").setParameter("id",targetId).executeUpdate();
        entityManager.createQuery("delete from UserNotification n where n.recipientUserId=:id").setParameter("id",targetId).executeUpdate();
        teachers.findByOwnerUserId(targetId).ifPresent(teachers::delete);
        users.delete(target);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/admin/users/{id}/status")
    public ResponseEntity<?> updateUserStatus(@RequestHeader(value="Authorization",required=false) String h,@PathVariable Long id,@RequestBody Map<String,Object> body) {
        User admin=current(h);if(admin==null)return noAuth();if(!"ADMIN".equals(admin.getRole()))return forbidden();
        User target=users.findById(id).orElse(null);if(target==null)return ResponseEntity.notFound().build();
        if(Objects.equals(target.getId(),admin.getId()))return ResponseEntity.badRequest().body(Map.of("message","You cannot suspend your own admin account."));
        Object raw=body.get("active");if(!(raw instanceof Boolean active))return ResponseEntity.badRequest().body(Map.of("message","A valid active status is required."));
        target.setActive(active);users.save(target);
        return ResponseEntity.ok(Map.of("id",target.getId(),"name",target.getName(),"email",target.getEmail(),"role",target.getRole(),"active",target.isActive()));
    }

    @GetMapping("/admin/reviews")
    public ResponseEntity<?> adminReviews(@RequestHeader(value="Authorization",required=false) String h) {
        User u=current(h);if(u==null)return noAuth();if(!"ADMIN".equals(u.getRole()))return forbidden();
        return ResponseEntity.ok(reviews.findAllWithTeacher().stream().map(r->Map.of("id",r.getId(),"teacherId",r.getTeacher().getId(),"teacherName",r.getTeacher().getName(),"studentName",r.getStudentName(),"rating",r.getRating(),"text",r.getText(),"createdAt",r.getCreatedAt(),"reported",r.isReported(),"reportReason",r.getReportReason()==null?"":r.getReportReason(),"hidden",r.isHidden())).toList());
    }
    @PatchMapping("/admin/reviews/{id}/visibility")
    public ResponseEntity<?> updateReviewVisibility(@RequestHeader(value="Authorization",required=false) String h,@PathVariable Long id,@RequestBody Map<String,Object> body) {
        User u=current(h);if(u==null)return noAuth();if(!"ADMIN".equals(u.getRole()))return forbidden();
        Review review=reviews.findById(id).orElse(null);if(review==null)return ResponseEntity.notFound().build();
        Object raw=body.get("hidden");if(!(raw instanceof Boolean hidden))return ResponseEntity.badRequest().body(Map.of("message","A valid hidden status is required."));
        review.setHidden(hidden);reviews.save(review);
        return ResponseEntity.ok(Map.of("id",review.getId(),"hidden",review.isHidden()));
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
        Teacher t=teachers.findByOwnerUserId(u.getId()).orElse(null);
        var own=consultations.findByTeacherOwnerUserId(u.getId());var teacherLessons=lessons.findByTeacherUserIdOrderByScheduledAtDesc(u.getId());
        List<Map<String,Object>> connected=connectedStudentsFor(u.getId());
        long activeStudents=connected.stream().filter(x->"ACTIVE".equals(x.get("status"))).count();
        long completedStudents=connected.stream().filter(x->"COMPLETED".equals(x.get("status"))).count();
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("consultations",own.size());result.put("pendingConsultations",own.stream().filter(x->"PENDING".equals(x.getStatus())).count());
        result.put("lessons",teacherLessons.size());result.put("completedLessons",teacherLessons.stream().filter(x->"COMPLETED".equals(x.getStatus())).count());
        result.put("reviews",t==null?0:reviews.countByTeacherId(t.getId()));result.put("averageRating",t==null?0:t.getRating());
        result.put("students",connected.size());result.put("totalConnectedStudents",connected.size());
        result.put("activeStudents",activeStudents);result.put("completedStudents",completedStudents);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/admin/activity")
    public ResponseEntity<?> adminActivity(@RequestHeader(value="Authorization",required=false) String h) {
        User u=current(h);if(u==null)return noAuth();if(!"ADMIN".equals(u.getRole()))return forbidden();
        List<Map<String,Object>> events=new ArrayList<>();
        for(var item:consultations.findAll()) if(item.getCreatedAt()!=null) events.add(Map.of("type","CONSULTATION","title","New consultation request","detail",item.getStudentName()+" requested "+item.getTeacher().getName(),"status",item.getStatus(),"occurredAt",item.getCreatedAt()));
        for(var item:lessons.findAll()) if(item.getCreatedAt()!=null) events.add(Map.of("type","LESSON","title","Lesson created","detail",item.getStudentName()+" · "+item.getTeacherName(),"status",item.getStatus(),"occurredAt",item.getCreatedAt()));
        for(var item:reviews.findAllWithTeacher()) if(item.getCreatedAt()!=null) events.add(Map.of("type","REVIEW","title","Student review submitted","detail",item.getStudentName()+" reviewed "+item.getTeacher().getName(),"status",item.isReported()?"REPORTED":(item.isHidden()?"HIDDEN":"VISIBLE"),"occurredAt",item.getCreatedAt()));
        events.sort((a,b)->((LocalDateTime)b.get("occurredAt")).compareTo((LocalDateTime)a.get("occurredAt")));
        return ResponseEntity.ok(events.stream().limit(50).toList());
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
