package seonbaeon_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "teacher_videos")
public class TeacherVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, length = 1000)
    private String videoUrl;

    @Column(nullable = false)
    private String videoType = "youtube";

    @Column(length = 1000)
    private String thumbnailUrl;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    public TeacherVideo() {
    }

    public TeacherVideo(
            String title,
            String description,
            String videoUrl,
            String videoType,
            String thumbnailUrl,
            Teacher teacher
    ) {
        this.title = title;
        this.description = description;
        this.videoUrl = videoUrl;
        this.videoType = videoType;
        this.thumbnailUrl = thumbnailUrl;
        this.teacher = teacher;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getVideoType() {
        return videoType;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public void setVideoType(String videoType) {
        this.videoType = videoType;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }
}