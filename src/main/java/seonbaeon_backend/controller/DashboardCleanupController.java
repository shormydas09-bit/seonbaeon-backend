package seonbaeon_backend.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import seonbaeon_backend.entity.*;
import seonbaeon_backend.repository.*;
import seonbaeon_backend.service.AuthService;
import java.util.*;

@RestController
@RequestMapping("/api/workspace")
@CrossOrigin(origins="*")
public class DashboardCleanupController {
    private final AuthService auth;
    private final DashboardHiddenItemRepository hidden;
    private final ConsultationRequestRepository consultations;
    private final LessonRepository lessons;
    public DashboardCleanupController(AuthService auth,DashboardHiddenItemRepository hidden,
        ConsultationRequestRepository consultations,LessonRepository lessons){
        this.auth=auth;this.hidden=hidden;this.consultations=consultations;this.lessons=lessons;
    }
    @PostMapping("/hide/{type}/{id}")
    public ResponseEntity<?> hide(@RequestHeader(value="Authorization",required=false) String h,
        @PathVariable String type,@PathVariable Long id){
        User u=auth.authenticate(h).orElse(null);
        if(u==null)return ResponseEntity.status(401).body(Map.of("message","Please log in again."));
        String kind=type.toUpperCase(Locale.ROOT);
        boolean allowed=false;
        if("CONSULTATION".equals(kind)){
            ConsultationRequest c=consultations.findById(id).orElse(null);
            if(c!=null){
                if("STUDENT".equals(u.getRole())) allowed=c.getEmail()!=null&&c.getEmail().equalsIgnoreCase(u.getEmail());
                if("TEACHER".equals(u.getRole())) allowed=c.getTeacher()!=null&&Objects.equals(c.getTeacher().getOwnerUserId(),u.getId());
                if("ADMIN".equals(u.getRole())) allowed=true;
            }
        }else if("LESSON".equals(kind)){
            Lesson l=lessons.findById(id).orElse(null);
            if(l!=null) allowed=("STUDENT".equals(u.getRole())&&Objects.equals(l.getStudentUserId(),u.getId()))
                ||("TEACHER".equals(u.getRole())&&Objects.equals(l.getTeacherUserId(),u.getId()))
                ||"ADMIN".equals(u.getRole());
        }else return ResponseEntity.badRequest().body(Map.of("message","Unsupported dashboard item."));
        if(!allowed)return ResponseEntity.status(403).body(Map.of("message","You cannot hide this item."));
        if(!hidden.existsByUserIdAndItemTypeAndItemId(u.getId(),kind,id))
            hidden.save(new DashboardHiddenItem(u.getId(),kind,id));
        return ResponseEntity.ok(Map.of("message","Item removed from your dashboard."));
    }
    public static Set<Long> hiddenIds(DashboardHiddenItemRepository repo,Long userId,String type){
        Set<Long> ids=new HashSet<>();
        repo.findByUserIdAndItemType(userId,type).forEach(x->ids.add(x.getItemId()));
        return ids;
    }
}