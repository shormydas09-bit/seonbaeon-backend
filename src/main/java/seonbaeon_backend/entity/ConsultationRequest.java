package seonbaeon_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "consultation_requests")
public class ConsultationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(nullable = false)
    private String studentName;

    @Column(nullable = false)
    private String email;

    private String phone;

    @Column(nullable = false)
    private LocalDate preferredDate;

    @Column(nullable = false)
    private LocalTime preferredTime;

    @Column(length = 2000)
    private String message;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public ConsultationRequest() {
    }

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = "PENDING";
        }
    }

    public Long getId() { return id; }
    public Teacher getTeacher() { return teacher; }
    public String getStudentName() { return studentName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public LocalDate getPreferredDate() { return preferredDate; }
    public LocalTime getPreferredTime() { return preferredTime; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setTeacher(Teacher teacher) { this.teacher = teacher; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setPreferredDate(LocalDate preferredDate) { this.preferredDate = preferredDate; }
    public void setPreferredTime(LocalTime preferredTime) { this.preferredTime = preferredTime; }
    public void setMessage(String message) { this.message = message; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
