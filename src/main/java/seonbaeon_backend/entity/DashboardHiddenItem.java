package seonbaeon_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="dashboard_hidden_items", uniqueConstraints=@UniqueConstraint(
    name="uk_dashboard_hidden_user_item", columnNames={"user_id","item_type","item_id"}))
public class DashboardHiddenItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(name="item_type",nullable=false,length=30) private String itemType;
    @Column(name="item_id",nullable=false) private Long itemId;
    @Column(nullable=false) private LocalDateTime hiddenAt;
    public DashboardHiddenItem() {}
    public DashboardHiddenItem(Long userId,String itemType,Long itemId){this.userId=userId;this.itemType=itemType;this.itemId=itemId;this.hiddenAt=LocalDateTime.now();}
    public Long getId(){return id;} public Long getUserId(){return userId;} public String getItemType(){return itemType;} public Long getItemId(){return itemId;} public LocalDateTime getHiddenAt(){return hiddenAt;}
}