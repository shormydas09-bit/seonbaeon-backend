package seonbaeon_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Entity
@Table(name = "teachers")
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String specialty;

    @Column(nullable = false)
    private String experience;

    @Column(nullable = false)
    private Integer studentCount;

    @Column(nullable = false)
    private Double rating;

    @Column(nullable = false)
    private Integer reviewCount;

    @Column(nullable = false)
    private String teachingStyle;

    @Column(length = 2000)
    private String qualification;

    @Column(length = 1000)
    private String qualificationDocumentUrl;

    @Column(length = 500)
    private String qualificationInstitution;

    @Column(length = 4000)
    private String bio;

    @Column(length = 1000)
    private String demoVideoUrl;

    @Column(nullable = false)
    private String demoVideoType = "youtube";

    @Column(nullable = false)
    private boolean available = true;

    @Column(nullable = false)
    private String emoji;

    @Column(length = 1000)
    private String profileImageUrl;

    private Double hourlyRate;
    private Integer lessonDuration;

    @Column(length = 1000)
    private String languages;

    @Column(length = 500)
    private String ageRange;

    @Column(length = 500)
    private String teachingMode;

    @Column(length = 1000)
    private String availability;

    private boolean verified = false;

    @Column(length = 1000)
    private String rejectionReason;

    private LocalDateTime rejectedAt;

    @Column(name = "owner_user_id", unique = true)
    private Long ownerUserId;

    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeacherVideo> videos = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "teacher")
    private List<Review> reviews = new ArrayList<>();

    public Teacher() {
    }

    public Teacher(
        String name,
        String category,
        String location,
        String specialty,
        String experience,
        Integer studentCount,
        Double rating,
        Integer reviewCount,
        String teachingStyle,
        String qualification,
        String bio,
        String demoVideoUrl,
        String demoVideoType,
        boolean available,
        String emoji
    ) {
        this.name = name;
        this.category = category;
        this.location = location;
        this.specialty = specialty;
        this.experience = experience;
        this.studentCount = studentCount;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.teachingStyle = teachingStyle;
        this.qualification = qualification;
        this.bio = bio;
        this.demoVideoUrl = demoVideoUrl;
        this.demoVideoType = demoVideoType;
        this.available = available;
        this.emoji = emoji;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getLocation() { return location; }
    public String getSpecialty() { return specialty; }
    public String getExperience() { return experience; }
    public Integer getStudentCount() { return studentCount; }
    public Double getRating() { return rating; }
    public Integer getReviewCount() { return reviewCount; }
    public String getTeachingStyle() { return teachingStyle; }
    public String getQualification() { return qualification; }
    public String getQualificationDocumentUrl() { return qualificationDocumentUrl; }
    public String getQualificationInstitution() { return qualificationInstitution; }
    public String getBio() { return bio; }
    public String getDemoVideoUrl() { return demoVideoUrl; }
    public String getDemoVideoType() { return demoVideoType; }
    public boolean isAvailable() { return available; }
    public String getEmoji() { return emoji; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public Double getHourlyRate() { return hourlyRate; }
    public Integer getLessonDuration() { return lessonDuration; }
    public String getLanguages() { return languages; }
    public String getAgeRange() { return ageRange; }
    public String getTeachingMode() { return teachingMode; }
    public String getAvailability() { return availability; }
    public boolean isVerified() { return verified; }
    public String getRejectionReason() { return rejectionReason; }
    public LocalDateTime getRejectedAt() { return rejectedAt; }

    @JsonIgnore
    public Long getOwnerUserId() { return ownerUserId; }

    public void setName(String name) { this.name = name; }
    public void setCategory(String category) { this.category = category; }
    public void setLocation(String location) { this.location = location; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }
    public void setExperience(String experience) { this.experience = experience; }
    public void setStudentCount(Integer studentCount) { this.studentCount = studentCount; }
    public void setRating(Double rating) { this.rating = rating; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }
    public void setTeachingStyle(String teachingStyle) { this.teachingStyle = teachingStyle; }
    public void setQualification(String qualification) { this.qualification = qualification; }
    public void setQualificationDocumentUrl(String url) { this.qualificationDocumentUrl = url; }
    public void setQualificationInstitution(String institution) { this.qualificationInstitution = institution; }
    public void setBio(String bio) { this.bio = bio; }
    public void setDemoVideoUrl(String demoVideoUrl) { this.demoVideoUrl = demoVideoUrl; }
    public void setDemoVideoType(String demoVideoType) { this.demoVideoType = demoVideoType; }
    public void setAvailable(boolean available) { this.available = available; }
    public void setEmoji(String emoji) { this.emoji = emoji; }

    @JsonIgnore
    public List<TeacherVideo> getVideos() {
        return videos;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void setHourlyRate(Double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public void setLessonDuration(Integer lessonDuration) {
        this.lessonDuration = lessonDuration;
    }

    public void setLanguages(String languages) {
        this.languages = languages;
    }

    public void setAgeRange(String ageRange) {
        this.ageRange = ageRange;
    }

    public void setTeachingMode(String teachingMode) {
        this.teachingMode = teachingMode;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }
}
