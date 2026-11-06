package seonbaeon_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import seonbaeon_backend.entity.DashboardHiddenItem;
import java.util.List;

public interface DashboardHiddenItemRepository extends JpaRepository<DashboardHiddenItem,Long> {
    boolean existsByUserIdAndItemTypeAndItemId(Long userId,String itemType,Long itemId);
    List<DashboardHiddenItem> findByUserIdAndItemType(Long userId,String itemType);
}