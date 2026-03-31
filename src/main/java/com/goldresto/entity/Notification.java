package com.goldresto.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(nullable = false)
    private String titre;
    
    @Column(nullable = false, length = 1000)
    private String message;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime readAt;
    
    public enum NotificationType {
        RESERVATION_ECHEANCE("Échéance de réservation"),
        RESERVATION_CONFIRMEE("Réservation confirmée"),
        RESERVATION_ANNULEE("Réservation annulée"),
        RESERVATION_TERMINEE("Réservation terminée"),
        STOCK_FAIBLE("Stock faible");
        
        private final String libelle;
        
        NotificationType(String libelle) {
            this.libelle = libelle;
        }
        
        public String getLibelle() {
            return libelle;
        }
    }
    
    public enum NotificationStatus {
        NON_LU("Non lu"),
        LU("Lu");
        
        private final String libelle;
        
        NotificationStatus(String libelle) {
            this.libelle = libelle;
        }
        
        public String getLibelle() {
            return libelle;
        }
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        status = NotificationStatus.NON_LU;
    }
    
    // Constructeurs
    public Notification() {}
    
    public Notification(Reservation reservation, User user, String titre, String message, NotificationType type) {
        this.reservation = reservation;
        this.user = user;
        this.titre = titre;
        this.message = message;
        this.type = type;
    }
    
    // Getters et Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Reservation getReservation() {
        return reservation;
    }
    
    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public String getTitre() {
        return titre;
    }
    
    public void setTitre(String titre) {
        this.titre = titre;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public NotificationType getType() {
        return type;
    }
    
    public void setType(NotificationType type) {
        this.type = type;
    }
    
    public NotificationStatus getStatus() {
        return status;
    }
    
    public void setStatus(NotificationStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getReadAt() {
        return readAt;
    }
    
    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }
    
    public void markAsRead() {
        this.status = NotificationStatus.LU;
        this.readAt = LocalDateTime.now();
    }
}
