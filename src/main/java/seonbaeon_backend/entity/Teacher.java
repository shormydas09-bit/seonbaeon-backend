package seonbaeon_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

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
    public String getBio() { return bio; }
    public String getDemoVideoUrl() { return demoVideoUrl; }
    public String getDemoVideoType() { return demoVideoType; }
    public boolean isAvailable() { return available; }
    public String getEmoji() { return emoji; }

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
    public void setBio(String bio) { this.bio = bio; }
    public void setDemoVideoUrl(String demoVideoUrl) { this.demoVideoUrl = demoVideoUrl; }
    public void setDemoVideoType(String demoVideoType) { this.demoVideoType = demoVideoType; }
    public void setAvailable(boolean available) { this.available = available; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
}
