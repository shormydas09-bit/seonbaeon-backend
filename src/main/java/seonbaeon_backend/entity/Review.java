package seonbaeon_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(nullable = false)
    private String studentName;

    @Column(name = "reviewer_user_id")
    private Long reviewerUserId;

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable=false)
    private boolean reported = false;

    @Column(nullable=false, columnDefinition="boolean default false")
    private boolean hidden = false;

    @Column(length=1000)
    private String reportReason;

    public Review() {
    }

    public Review(Teacher teacher, String studentName, Integer rating, String text) {
        this.teacher = teacher;
        this.studentName = studentName;
        this.rating = rating;
        this.text = text;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public Teacher getTeacher() { return teacher; }
    public String getStudentName() { return studentName; }
    public Long getReviewerUserId() { return reviewerUserId; }
    public Integer getRating() { return rating; }
    public String getText() { return text; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isReported() { return reported; }
    public boolean isHidden() { return hidden; }
    public String getReportReason() { return reportReason; }

    public void setTeacher(Teacher teacher) { this.teacher = teacher; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public void setReviewerUserId(Long reviewerUserId) { this.reviewerUserId = reviewerUserId; }
    public void setRating(Integer rating) { this.rating = rating; }
    public void setText(String text) { this.text = text; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setReported(boolean reported) { this.reported = reported; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
    public void setReportReason(String reportReason) { this.reportReason = reportReason; }
}
