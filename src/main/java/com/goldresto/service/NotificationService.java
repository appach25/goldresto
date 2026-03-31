package com.goldresto.service;

import com.goldresto.entity.Notification;
import com.goldresto.entity.Produit;
import com.goldresto.entity.Reservation;
import com.goldresto.entity.User;
import com.goldresto.repository.NotificationRepository;
import com.goldresto.repository.ProduitRepository;
import com.goldresto.repository.ReservationRepository;
import com.goldresto.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    private final NotificationRepository notificationRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ProduitRepository produitRepository;
    
    public NotificationService(NotificationRepository notificationRepository, 
                             ReservationRepository reservationRepository,
                             UserRepository userRepository,
                             ProduitRepository produitRepository) {
        this.notificationRepository = notificationRepository;
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.produitRepository = produitRepository;
    }
    
    /**
     * Vérifie les réservations arrivant à échéance et crée des notifications
     * Exécuté tous les jours à 9h du matin
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void verifierEcheanceReservations() {
        logger.info("Vérification des échéances de réservations...");
        
        LocalDate aujourdHui = LocalDate.now();
        LocalDate demain = aujourdHui.plusDays(1);
        LocalDate dansTroisJours = aujourdHui.plusDays(3);
        
        logger.info("Aujourd'hui: {}, Demain: {}, Dans 3 jours: {}", aujourdHui, demain, dansTroisJours);
        
        // Réservations pour demain (alerte 1 jour avant)
        List<Reservation> reservationsDemain = reservationRepository.findByDateReservation(demain);
        logger.info("Réservations trouvées pour demain ({}): {}", demain, reservationsDemain.size());
        
        for (Reservation reservation : reservationsDemain) {
            logger.info("Réservation demain - ID: {}, Statut: {}, Produit: {}", 
                       reservation.getId(), reservation.getStatus(), reservation.getProduit().getNomProduit());
            
            if (reservation.getStatus() == Reservation.ReservationStatus.EN_ATTENTE || 
                reservation.getStatus() == Reservation.ReservationStatus.CONFIRMEE) {
                
                creerNotificationEcheance(reservation, 1);
            } else {
                logger.info("Réservation {} ignorée - statut: {}", reservation.getId(), reservation.getStatus());
            }
        }
        
        // Réservations dans 3 jours (alerte 3 jours avant)
        List<Reservation> reservationsDansTroisJours = reservationRepository.findByDateReservation(dansTroisJours);
        logger.info("Réservations trouvées dans 3 jours ({}): {}", dansTroisJours, reservationsDansTroisJours.size());
        
        for (Reservation reservation : reservationsDansTroisJours) {
            logger.info("Réservation 3 jours - ID: {}, Statut: {}, Produit: {}", 
                       reservation.getId(), reservation.getStatus(), reservation.getProduit().getNomProduit());
            
            if (reservation.getStatus() == Reservation.ReservationStatus.EN_ATTENTE || 
                reservation.getStatus() == Reservation.ReservationStatus.CONFIRMEE) {
                
                creerNotificationEcheance(reservation, 3);
            } else {
                logger.info("Réservation {} ignorée - statut: {}", reservation.getId(), reservation.getStatus());
            }
        }
        
        logger.info("Vérification terminée. {} réservations pour demain, {} pour dans 3 jours.", 
                   reservationsDemain.size(), reservationsDansTroisJours.size());
    }
    
    /**
     * Crée une réservation de test pour demain et la notification associée
     */
    public void creerNotificationTest(User user) {
        logger.info("Création réservation de test pour demain...");
        
        if (user == null) {
            logger.error("Utilisateur null - impossible de créer la réservation de test");
            throw new IllegalArgumentException("Utilisateur non authentifié");
        }
        
        try {
            // Trouver un produit existant
            List<Produit> produits = produitRepository.findAll();
            if (produits.isEmpty()) {
                logger.error("Aucun produit trouvé pour créer une réservation de test");
                throw new IllegalStateException("Aucun produit disponible");
            }
            
            Produit produit = produits.get(0);
            LocalDate demain = LocalDate.now().plusDays(1);
            
            // Créer une réservation de test
            Reservation reservation = new Reservation();
            reservation.setProduit(produit);
            reservation.setUser(user);
            reservation.setQuantite(1);
            reservation.setDateReservation(demain);
            reservation.setDateLimite(demain.minusDays(1));
            reservation.setNotes("Réservation de test pour notification");
            reservation.setStatus(Reservation.ReservationStatus.EN_ATTENTE);
            
            reservation = reservationRepository.save(reservation);
            logger.info("Réservation de test créée: ID {}, Produit {}, Date {}, Utilisateur {}", 
                       reservation.getId(), produit.getNomProduit(), demain, user.getUsername());
            
            // Créer directement la notification
            creerNotificationEcheance(reservation, 1);
            
        } catch (Exception e) {
            logger.error("Erreur lors de la création de la réservation de test", e);
            throw new RuntimeException("Impossible de créer la réservation de test: " + e.getMessage());
        }
    }
    
    /**
     * Crée une notification d'échéance pour une réservation
     */
    public void creerNotificationEcheance(Reservation reservation, int joursAvant) {
        logger.info("Création notification échéance pour réservation {} - {} jours avant", 
                   reservation.getId(), joursAvant);
        
        String titre = "Réservation arrive à échéance";
        String message = String.format(
            "La réservation de %d x %s pour le %s arrive à échéance dans %d jour(s). " +
            "Date limite de livraison : %s",
            reservation.getQuantite(),
            reservation.getProduit().getNomProduit(),
            reservation.getDateReservation(),
            joursAvant,
            reservation.getDateLimite()
        );
        
        // Notifier tous les utilisateurs avec rôles ADMIN, MANAGER, OWNER
        List<User> utilisateurs = userRepository.findByRolesNameIn(
            List.of("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_OWNER")
        );
        
        logger.info("Utilisateurs trouvés pour notification: {}", utilisateurs.size());
        
        for (User user : utilisateurs) {
            logger.info("Création notification pour utilisateur: {}", user.getUsername());
            Notification notification = new Notification(
                reservation, 
                user, 
                titre, 
                message, 
                Notification.NotificationType.RESERVATION_ECHEANCE
            );
            
            notificationRepository.save(notification);
            logger.info("Notification créée pour l'utilisateur {} - Réservation ID: {}", 
                       user.getUsername(), reservation.getId());
        }
    }
    
    /**
     * Crée une notification pour le changement de statut d'une réservation
     */
    public void creerNotificationChangementStatut(Reservation reservation, 
                                               Reservation.ReservationStatus ancienStatut,
                                               Reservation.ReservationStatus nouveauStatut) {
        String titre = "Changement de statut de réservation";
        String message = String.format(
            "La réservation de %d x %s pour le %s est passée de %s à %s",
            reservation.getQuantite(),
            reservation.getProduit().getNomProduit(),
            reservation.getDateReservation(),
            ancienStatut.getLibelle(),
            nouveauStatut.getLibelle()
        );
        
        Notification.NotificationType type = switch (nouveauStatut) {
            case CONFIRMEE -> Notification.NotificationType.RESERVATION_CONFIRMEE;
            case ANNULEE -> Notification.NotificationType.RESERVATION_ANNULEE;
            case TERMINEE -> Notification.NotificationType.RESERVATION_TERMINEE;
            default -> Notification.NotificationType.RESERVATION_ECHEANCE;
        };
        
        // Notifier tous les utilisateurs avec rôles ADMIN, MANAGER, OWNER
        List<User> utilisateurs = userRepository.findByRolesNameIn(
            List.of("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_OWNER")
        );
        
        for (User user : utilisateurs) {
            Notification notification = new Notification(
                reservation, 
                user, 
                titre, 
                message, 
                type
            );
            
            notificationRepository.save(notification);
        }
    }
    
    /**
     * Récupère les notifications non lues pour un utilisateur
     */
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsNonLues(User user) {
        return notificationRepository.findNonLuByUser(user);
    }
    
    /**
     * Récupère toutes les notifications pour un utilisateur
     */
    @Transactional(readOnly = true)
    public List<Notification> getNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    /**
     * Compte les notifications non lues pour un utilisateur
     */
    @Transactional(readOnly = true)
    public long countNotificationsNonLues(User user) {
        return notificationRepository.countNonLuByUser(user);
    }
    
    /**
     * Marque une notification comme lue
     */
    public void marquerCommeLue(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("Notification non trouvée: " + notificationId));
        
        notification.markAsRead();
        notificationRepository.save(notification);
    }
    
    /**
     * Marque toutes les notifications d'un utilisateur comme lues
     */
    public void marquerToutesCommeLues(User user) {
        List<Notification> notificationsNonLues = getNotificationsNonLues(user);
        
        for (Notification notification : notificationsNonLues) {
            notification.markAsRead();
            notificationRepository.save(notification);
        }
    }
    
    /**
     * Supprime les anciennes notifications lues (plus de 30 jours)
     */
    @Scheduled(cron = "0 0 2 * * ?") // Tous les jours à 2h du matin
    public void nettoyerAnciennesNotifications() {
        LocalDateTime ilYA30Jours = LocalDateTime.now().minusDays(30);
        notificationRepository.deleteByReadAtBefore(ilYA30Jours);
        logger.info("Nettoyage des anciennes notifications terminé.");
    }
}
