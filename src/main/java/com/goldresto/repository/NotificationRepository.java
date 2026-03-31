package com.goldresto.repository;

import com.goldresto.entity.Notification;
import com.goldresto.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    
    List<Notification> findByUserAndStatusOrderByCreatedAtDesc(User user, Notification.NotificationStatus status);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user = :user AND n.status = 'NON_LU'")
    long countNonLuByUser(@Param("user") User user);
    
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.status = 'NON_LU' ORDER BY n.createdAt DESC")
    List<Notification> findNonLuByUser(@Param("user") User user);
    
    @Query("SELECT n FROM Notification n WHERE n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<Notification> findRecent(@Param("since") LocalDateTime since);
    
    void deleteByReadAtBefore(LocalDateTime date);
}
