package seonbaeon_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import seonbaeon_backend.entity.*;
import seonbaeon_backend.repository.*;
import seonbaeon_backend.service.AuthService;
import seonbaeon_backend.service.NotificationService;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@RestController
@RequestMapping("/api/lessons")
@CrossOrigin(origins = "*")
public class LessonController {
    private final AuthService auth;
    private final LessonRepository lessons;
    private final ConsultationRequestRepository consultations;
    private final TeacherRepository teachers;
    private final UserRepository users;
    private final NotificationService notifications;
    private final DashboardHiddenItemRepository hiddenItems;

    public LessonController(AuthService auth, LessonRepository lessons,
            ConsultationRequestRepository consultations, TeacherRepository teachers,
            UserRepository users, NotificationService notifications, DashboardHiddenItemRepository hiddenItems) {
        this.auth=auth; this.lessons=lessons; this.consultations=consultations;
        this.teachers=teachers; this.users=users; this.notifications=notifications;
    }

    private User current(String authorization) { return auth.authenticate(authorization).orElse(null); }
    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","Please log in again."));
    }
    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message","You do not have permission for this lesson."));
    }

    @GetMapping
    public ResponseEntity<?> myLessons(@RequestHeader(value="Authorization", required=false) String authorization) {
        User user=current(authorization);
        if(user==null) return unauthorized();
        if("STUDENT".equals(user.getRole()))
            Set<Long> hidden=DashboardCleanupController.hiddenIds(hiddenItems,user.getId(),"LESSON");
            return ResponseEntity.ok(lessons.findByStudentUserIdOrderByScheduledAtDesc(user.getId()).stream().filter(l->!hidden.contains(l.getId())).map(this::data).toList());
        if("TEACHER".equals(user.getRole()))
            Set<Long> hidden=DashboardCleanupController.hiddenIds(hiddenItems,user.getId(),"LESSON");
            return ResponseEntity.ok(lessons.findByTeacherUserIdOrderByScheduledAtDesc(user.getId()).stream().filter(l->!hidden.contains(l.getId())).map(this::data).toList());
        if("ADMIN".equals(user.getRole()))
            Set<Long> hidden=DashboardCleanupController.hiddenIds(hiddenItems,user.getId(),"LESSON");
            return ResponseEntity.ok(lessons.findAll().stream().filter(l->!hidden.contains(l.getId())).map(this::data).toList());
        return forbidden();
    }

    @PostMapping("/from-consultation/{consultationId}")
    @Transactional
    public ResponseEntity<?> schedule(@RequestHeader(value="Authorization", required=false) String authorization,
            @PathVariable Long consultationId, @RequestBody Map<String,Object> body) {
        User user=current(authorization);
        if(user==null) return unauthorized();
        if(!"TEACHER".equals(user.getRole())) return forbidden();
        ConsultationRequest request=consultations.findById(consultationId).orElse(null);
        if(request==null || request.getTeacher()==null ||
                !Objects.equals(request.getTeacher().getOwnerUserId(),user.getId())) return ResponseEntity.notFound().build();
        if(!"APPROVED".equalsIgnoreCase(request.getStatus()))
            return ResponseEntity.badRequest().body(Map.of("message","Only approved consultations can be scheduled."));
        if(lessons.existsByConsultationId(consultationId))
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message","A lesson is already scheduled for this consultation."));
        User student=users.findByEmailIgnoreCase(request.getEmail()).orElse(null);
        if(student==null || !"STUDENT".equals(student.getRole()))
            return ResponseEntity.badRequest().body(Map.of("message","The student must create an account with the consultation email before scheduling."));
        LocalDateTime scheduledAt=parseDate(body.get("scheduledAt"));
        if(scheduledAt==null || !scheduledAt.isAfter(LocalDateTime.now()))
            return ResponseEntity.badRequest().body(Map.of("message","Choose a valid future lesson date and time."));
        Integer duration=parseDuration(body.get("durationMinutes"));
        if(duration==null || duration<15 || duration>180)
            return ResponseEntity.badRequest().body(Map.of("message","Lesson duration must be between 15 and 180 minutes."));
        Lesson lesson=new Lesson();
        lesson.setConsultationId(request.getId());
        lesson.setTeacherId(request.getTeacher().getId());
        lesson.setTeacherUserId(user.getId());
        lesson.setStudentUserId(student.getId());
        lesson.setTeacherName(request.getTeacher().getName());
        lesson.setStudentName(student.getName());
        lesson.setStudentEmail(student.getEmail());
        lesson.setScheduledAt(scheduledAt);
        lesson.setDurationMinutes(duration);
        lesson.setMeetingLink(clean(body.get("meetingLink"),1000));
        lesson.setNotes(clean(body.get("notes"),2000));
        Lesson saved=lessons.save(lesson);
        notifications.notifyUser(student.getId(),"Lesson scheduled",
                "Your lesson with "+saved.getTeacherName()+" is scheduled for "+scheduledAt+".","LESSON");
        notifications.notifyUser(user.getId(),"Lesson scheduled",
                "Your lesson with "+saved.getStudentName()+" has been scheduled.","LESSON");
        return ResponseEntity.status(HttpStatus.CREATED).body(data(saved));
    }

    @PatchMapping("/{id}/reschedule")
    @Transactional
    public ResponseEntity<?> reschedule(@RequestHeader(value="Authorization", required=false) String authorization,
            @PathVariable Long id, @RequestBody Map<String,Object> body) {
        User user=current(authorization);
        if(user==null) return unauthorized();
        Lesson lesson=lessons.findById(id).orElse(null);
        if(lesson==null) return ResponseEntity.notFound().build();
        if(!owns(user,lesson)) return forbidden();
        if(!"SCHEDULED".equals(lesson.getStatus()))
            return ResponseEntity.badRequest().body(Map.of("message","Only scheduled lessons can be rescheduled."));
        LocalDateTime next=parseDate(body.get("scheduledAt"));
        if(next==null || !next.isAfter(LocalDateTime.now()))
            return ResponseEntity.badRequest().body(Map.of("message","Choose a valid future lesson date and time."));
        lesson.setScheduledAt(next);
        if("TEACHER".equals(user.getRole())) {
            String link=clean(body.get("meetingLink"),1000);
            if(link!=null) lesson.setMeetingLink(link);
        }
        Lesson saved=lessons.save(lesson);
        notifyOtherParty(user,saved,"Lesson rescheduled","The lesson has been rescheduled to "+next+".");
        return ResponseEntity.ok(data(saved));
    }

    @PatchMapping("/{id}/status")
    @Transactional
    public ResponseEntity<?> updateStatus(@RequestHeader(value="Authorization", required=false) String authorization,
            @PathVariable Long id, @RequestBody Map<String,Object> body) {
        User user=current(authorization);
        if(user==null) return unauthorized();
        Lesson lesson=lessons.findById(id).orElse(null);
        if(lesson==null) return ResponseEntity.notFound().build();
        if(!owns(user,lesson)) return forbidden();
        String next=String.valueOf(body.getOrDefault("status","")).trim().toUpperCase(Locale.ROOT);
        if(!Set.of("COMPLETED","CANCELLED").contains(next))
            return ResponseEntity.badRequest().body(Map.of("message","Choose COMPLETED or CANCELLED."));
        if(!"SCHEDULED".equals(lesson.getStatus()))
            return ResponseEntity.badRequest().body(Map.of("message","This lesson is no longer scheduled."));
        if("COMPLETED".equals(next) && !"TEACHER".equals(user.getRole()) && !"ADMIN".equals(user.getRole()))
            return forbidden();
        lesson.setStatus(next);
        lesson.setCancellationReason("CANCELLED".equals(next)?clean(body.get("reason"),1000):null);
        if ("COMPLETED".equals(next)) {
            consultations.findById(lesson.getConsultationId()).ifPresent(request -> {
                request.setStatus("COMPLETED");
                consultations.save(request);
            });
        }
        Lesson saved=lessons.save(lesson);
        String title="COMPLETED".equals(next)?"Lesson completed":"Lesson cancelled";
        notifyOtherParty(user,saved,title,"Your lesson has been marked "+next.toLowerCase(Locale.ROOT)+".");
        if("COMPLETED".equals(next))
            notifications.notifyUser(saved.getStudentUserId(),"You can now review your teacher",
                    "Your lesson with "+saved.getTeacherName()+" is complete. Share your feedback on the teacher profile.","LESSON");
        return ResponseEntity.ok(data(saved));
    }

    private boolean owns(User user, Lesson lesson) {
        return ("STUDENT".equals(user.getRole()) && Objects.equals(user.getId(),lesson.getStudentUserId()))
            || ("TEACHER".equals(user.getRole()) && Objects.equals(user.getId(),lesson.getTeacherUserId()))
            || "ADMIN".equals(user.getRole());
    }
    private void notifyOtherParty(User actor, Lesson lesson, String title, String message) {
        Long recipient="STUDENT".equals(actor.getRole())?lesson.getTeacherUserId():lesson.getStudentUserId();
        notifications.notifyUser(recipient,title,message,"LESSON");
    }
    private LocalDateTime parseDate(Object value) {
        if(value==null) return null;
        try{return LocalDateTime.parse(String.valueOf(value));}
        catch(DateTimeParseException ex){return null;}
    }
    private Integer parseDuration(Object value) {
        if(value==null) return 30;
        try{return Integer.valueOf(String.valueOf(value));}
        catch(NumberFormatException ex){return null;}
    }
    private String clean(Object value,int max) {
        if(value==null) return null;
        String s=String.valueOf(value).trim();
        if(s.isEmpty()) return null;
        return s.length()>max?s.substring(0,max):s;
    }
    private Map<String,Object> data(Lesson l) {
        Map<String,Object> out=new LinkedHashMap<>();
        out.put("id",l.getId()); out.put("consultationId",l.getConsultationId());
        out.put("teacherId",l.getTeacherId()); out.put("teacherName",l.getTeacherName());
        out.put("studentName",l.getStudentName()); out.put("studentEmail",l.getStudentEmail());
        out.put("scheduledAt",l.getScheduledAt()); out.put("durationMinutes",l.getDurationMinutes());
        out.put("status",l.getStatus()); out.put("meetingLink",l.getMeetingLink());
        out.put("notes",l.getNotes()); out.put("cancellationReason",l.getCancellationReason());
        out.put("createdAt",l.getCreatedAt()); out.put("updatedAt",l.getUpdatedAt());
        return out;
    }
}
