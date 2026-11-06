package seonbaeon_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="student_progress_notes")
public class StudentProgressNote {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long teacherUserId;
    @Column(nullable=false) private Long studentUserId;
    @Column(nullable=false,length=120) private String subject;
    @Column(nullable=false,length=3000) private String note;
    @Column(nullable=false) private LocalDateTime createdAt;
    @Column(nullable=false) private LocalDateTime updatedAt;
    public StudentProgressNote() {}
    @PrePersist public void create(){createdAt=LocalDateTime.now();updatedAt=createdAt;}
    @PreUpdate public void update(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;} public Long getTeacherUserId(){return teacherUserId;}
    public Long getStudentUserId(){return studentUserId;} public String getSubject(){return subject;}
    public String getNote(){return note;} public LocalDateTime getCreatedAt(){return createdAt;}
    public LocalDateTime getUpdatedAt(){return updatedAt;}
    public void setTeacherUserId(Long v){teacherUserId=v;} public void setStudentUserId(Long v){studentUserId=v;}
    public void setSubject(String v){subject=v;} public void setNote(String v){note=v;}
}