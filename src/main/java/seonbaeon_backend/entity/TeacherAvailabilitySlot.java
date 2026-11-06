package seonbaeon_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="teacher_availability_slots")
public class TeacherAvailabilitySlot {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long teacherUserId;
    @Column(nullable=false) private LocalDateTime startsAt;
    @Column(nullable=false) private LocalDateTime endsAt;
    @Column(nullable=false) private boolean available=true;
    @Column(length=500) private String note;
    public TeacherAvailabilitySlot() {}
    public Long getId(){return id;} public Long getTeacherUserId(){return teacherUserId;}
    public LocalDateTime getStartsAt(){return startsAt;} public LocalDateTime getEndsAt(){return endsAt;}
    public boolean isAvailable(){return available;} public String getNote(){return note;}
    public void setTeacherUserId(Long v){teacherUserId=v;} public void setStartsAt(LocalDateTime v){startsAt=v;}
    public void setEndsAt(LocalDateTime v){endsAt=v;} public void setAvailable(boolean v){available=v;}
    public void setNote(String v){note=v;}
}