package seonbaeon_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "consultation_requests")
public class ConsultationRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @JsonIgnore @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "teacher_id", nullable = false) private Teacher teacher;
    @Column(nullable = false) private String studentName;
    @Column(nullable = false) private String email;
    private String phone;
    @Column(nullable = false) private LocalDate preferredDate;
    @Column(nullable = false) private LocalTime preferredTime;
    @Column(length = 2000) private String message;
    @Column(nullable = false) private String status = "PENDING";
    @Column(length=1000) private String rejectionReason;
    private String teacherDecision;
    private LocalDateTime teacherDecisionAt;
    private String adminPreviousStatus;
    @Column(length=1000) private String adminNote;
    private LocalDateTime adminDecisionAt;
    @Column(length=5000) private String decisionHistory;
    private LocalDateTime createdAt;

    public ConsultationRequest() {}
    @PrePersist public void onCreate(){if(createdAt==null)createdAt=LocalDateTime.now();if(status==null||status.isBlank())status="PENDING";}
    public Long getId(){return id;} public Teacher getTeacher(){return teacher;}
    public String getStudentName(){return studentName;} public String getEmail(){return email;}
    public String getPhone(){return phone;} public LocalDate getPreferredDate(){return preferredDate;}
    public LocalTime getPreferredTime(){return preferredTime;} public String getMessage(){return message;}
    public String getStatus(){return status;} public LocalDateTime getCreatedAt(){return createdAt;}
    public String getRejectionReason(){return rejectionReason;} public String getTeacherDecision(){return teacherDecision;}
    public LocalDateTime getTeacherDecisionAt(){return teacherDecisionAt;} public String getAdminPreviousStatus(){return adminPreviousStatus;}
    public String getAdminNote(){return adminNote;} public LocalDateTime getAdminDecisionAt(){return adminDecisionAt;}
    public void setTeacher(Teacher v){teacher=v;} public void setStudentName(String v){studentName=v;}
    public void setEmail(String v){email=v;} public void setPhone(String v){phone=v;}
    public void setPreferredDate(LocalDate v){preferredDate=v;} public void setPreferredTime(LocalTime v){preferredTime=v;}
    public void setMessage(String v){message=v;} public void setStatus(String v){status=v;}
    public void setCreatedAt(LocalDateTime v){createdAt=v;} public void setRejectionReason(String v){rejectionReason=v;}
    public void setTeacherDecision(String v){teacherDecision=v;} public void setTeacherDecisionAt(LocalDateTime v){teacherDecisionAt=v;}
    public void setAdminPreviousStatus(String v){adminPreviousStatus=v;} public void setAdminNote(String v){adminNote=v;}
    public void setAdminDecisionAt(LocalDateTime v){adminDecisionAt=v;} public void setDecisionHistory(String v){decisionHistory=v;}
}