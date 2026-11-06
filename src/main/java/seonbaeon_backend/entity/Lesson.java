package seonbaeon_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lessons", indexes = {
    @Index(name = "idx_lessons_student", columnList = "student_user_id"),
    @Index(name = "idx_lessons_teacher", columnList = "teacher_user_id"),
    @Index(name = "idx_lessons_scheduled_at", columnList = "scheduled_at")
})
public class Lesson {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private Long consultationId;
    @Column(nullable = false)
    private Long teacherId;
    @Column(name = "teacher_user_id", nullable = false)
    private Long teacherUserId;
    @Column(name = "student_user_id", nullable = false)
    private Long studentUserId;
    @Column(nullable = false, length = 120)
    private String teacherName;
    @Column(nullable = false, length = 120)
    private String studentName;
    @Column(nullable = false, length = 254)
    private String studentEmail;
    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;
    @Column(nullable = false)
    private Integer durationMinutes = 30;
    @Column(nullable = false, length = 20)
    private String status = "SCHEDULED";
    @Column(length = 1000)
    private String meetingLink;
    @Column(length = 2000)
    private String notes;
    @Column(length = 1000)
    private String cancellationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Lesson() {}
    @PrePersist public void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (status == null) status = "SCHEDULED";
    }
    @PreUpdate public void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId(){return id;}
    public Long getConsultationId(){return consultationId;}
    public Long getTeacherId(){return teacherId;}
    public Long getTeacherUserId(){return teacherUserId;}
    public Long getStudentUserId(){return studentUserId;}
    public String getTeacherName(){return teacherName;}
    public String getStudentName(){return studentName;}
    public String getStudentEmail(){return studentEmail;}
    public LocalDateTime getScheduledAt(){return scheduledAt;}
    public Integer getDurationMinutes(){return durationMinutes;}
    public String getStatus(){return status;}
    public String getMeetingLink(){return meetingLink;}
    public String getNotes(){return notes;}
    public String getCancellationReason(){return cancellationReason;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public LocalDateTime getUpdatedAt(){return updatedAt;}

    public void setConsultationId(Long v){consultationId=v;}
    public void setTeacherId(Long v){teacherId=v;}
    public void setTeacherUserId(Long v){teacherUserId=v;}
    public void setStudentUserId(Long v){studentUserId=v;}
    public void setTeacherName(String v){teacherName=v;}
    public void setStudentName(String v){studentName=v;}
    public void setStudentEmail(String v){studentEmail=v;}
    public void setScheduledAt(LocalDateTime v){scheduledAt=v;}
    public void setDurationMinutes(Integer v){durationMinutes=v;}
    public void setStatus(String v){status=v;}
    public void setMeetingLink(String v){meetingLink=v;}
    public void setNotes(String v){notes=v;}
    public void setCancellationReason(String v){cancellationReason=v;}
}
