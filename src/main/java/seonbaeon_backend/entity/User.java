package seonbaeon_backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false, unique = true) private String email;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @Column(nullable = false) private String password;
    @Column(nullable = false) private String role = "STUDENT";
    @Column(nullable = false) private boolean active = true;
    @Column(length=1000) private String profileImageUrl;
    private Integer age;
    private String ageGroup;
    private String englishLevel;
    @Column(length=1000) private String learningGoals;
    private String preferredCategory;
    @Column(length=1000) private String interests;
    private String preferredSchedule;
    @Column(length=2000) private String introduction;
    private String guardianName;
    private String guardianPhone;

    public User() {}
    public User(String name, String email, String password) { this.name=name; this.email=email; this.password=password; this.role="STUDENT"; }
    public Long getId(){return id;} public String getName(){return name;} public void setName(String v){name=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getPassword(){return password;} public void setPassword(String v){password=v;}
    public String getRole(){return role;} public void setRole(String v){role=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
    public String getProfileImageUrl(){return profileImageUrl;} public void setProfileImageUrl(String v){profileImageUrl=v;}
    public Integer getAge(){return age;} public void setAge(Integer v){age=v;}
    public String getAgeGroup(){return ageGroup;} public void setAgeGroup(String v){ageGroup=v;}
    public String getEnglishLevel(){return englishLevel;} public void setEnglishLevel(String v){englishLevel=v;}
    public String getLearningGoals(){return learningGoals;} public void setLearningGoals(String v){learningGoals=v;}
    public String getPreferredCategory(){return preferredCategory;} public void setPreferredCategory(String v){preferredCategory=v;}
    public String getInterests(){return interests;} public void setInterests(String v){interests=v;}
    public String getPreferredSchedule(){return preferredSchedule;} public void setPreferredSchedule(String v){preferredSchedule=v;}
    public String getIntroduction(){return introduction;} public void setIntroduction(String v){introduction=v;}
    public String getGuardianName(){return guardianName;} public void setGuardianName(String v){guardianName=v;}
    public String getGuardianPhone(){return guardianPhone;} public void setGuardianPhone(String v){guardianPhone=v;}
}