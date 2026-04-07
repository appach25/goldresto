package com.goldresto.controller;

import com.goldresto.entity.Notification;
import com.goldresto.entity.User;
import com.goldresto.repository.UserRepository;
import com.goldresto.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/notifications")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OWNER', 'EMPLOYEE')")
public class NotificationController {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);
    
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    
    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }
    
    /**
     * Affiche la liste des notifications pour l'utilisateur connecté
     */
    @GetMapping
    public String listNotifications(Model model) {
        // Récupérer l'utilisateur authentifié
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé: " + username));
        
        List<Notification> notifications = notificationService.getNotifications(user);
        long nonLuesCount = notificationService.countNotificationsNonLues(user);
        
        model.addAttribute("notifications", notifications);
        model.addAttribute("nonLuesCount", nonLuesCount);
        
        return "notifications/list";
    }
    
    /**
     * Affiche uniquement les notifications non lues
     */
    @GetMapping("/non-lues")
    public String listNotificationsNonLues(Model model) {
        // Récupérer l'utilisateur authentifié
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé: " + username));
        
        List<Notification> notifications = notificationService.getNotificationsNonLues(user);
        
        model.addAttribute("notifications", notifications);
        model.addAttribute("nonLuesCount", notifications.size());
        model.addAttribute("titre", "Notifications non lues");
        
        return "notifications/list";
    }
    
    /**
     * API pour obtenir le nombre de notifications non lues (pour AJAX)
     */
    @GetMapping("/api/count")
    @ResponseBody
    public Map<String, Object> getNotificationsCount() {
        try {
            logger.info("=== API NOTIFICATIONS COUNT START ===");
            
            // Récupérer l'utilisateur authentifié
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            logger.info("Utilisateur authentifié: {}", username);
            
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé: " + username));
            
            logger.info("Utilisateur trouvé - ID: {}, Username: {}", user.getId(), user.getUsername());
            
            long count = notificationService.countNotificationsNonLues(user);
            List<Notification> recentNotifications = notificationService.getNotificationsNonLues(user)
                .stream()
                .limit(5)
                .toList();
            
            logger.info("Nombre de notifications non lues: {}", count);
            logger.info("Notifications récentes récupérées: {}", recentNotifications.size());
            
            // Log des notifications pour debug
            if (!recentNotifications.isEmpty()) {
                recentNotifications.forEach(n -> 
                    logger.info("  - Notification ID: {}, Titre: {}, Status: {}", 
                               n.getId(), n.getTitre(), n.getStatus())
                );
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("count", count);
            response.put("notifications", recentNotifications);
            response.put("debugInfo", "User: " + username + ", Count: " + count);
            response.put("totalNotifications", recentNotifications.size());
            
            logger.info("=== API NOTIFICATIONS COUNT END ===");
            logger.info("Réponse retournée: count={}, notifications={}", count, recentNotifications.size());
            
            return response;
        } catch (Exception e) {
            logger.error("Erreur dans l'API /notifications/api/count", e);
            Map<String, Object> response = new HashMap<>();
            response.put("count", 0);
            response.put("notifications", List.of());
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    /**
     * Endpoint de debug pour vérifier les notifications
     */
    @GetMapping("/debug/count")
    @ResponseBody
    public Map<String, Object> debugNotificationCount() {
        logger.info("=== DEBUG NOTIFICATIONS ===");
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé: " + username));
            
            List<Notification> allNotifications = notificationService.getNotifications(user);
            long nonLuesCount = notificationService.countNotificationsNonLues(user);
            
            Map<String, Object> debug = new HashMap<>();
            debug.put("username", username);
            debug.put("userId", user.getId());
            debug.put("totalNotifications", allNotifications.size());
            debug.put("nonLuesCount", nonLuesCount);
            debug.put("notifications", allNotifications.stream()
                .map(n -> Map.of(
                    "id", n.getId(),
                    "titre", n.getTitre(),
                    "status", n.getStatus(),
                    "createdAt", n.getCreatedAt()
                )).toList());
            
            logger.info("DEBUG - Total: {}, Non lues: {}, Utilisateur: {}", 
                       allNotifications.size(), nonLuesCount, username);
            
            return debug;
        } catch (Exception e) {
            logger.error("Erreur debug", e);
            Map<String, Object> debug = new HashMap<>();
            debug.put("error", e.getMessage());
            return debug;
        }
    }
    
    /**
     * Déclenche manuellement la vérification des échéances
     */
    @PostMapping("/verifier-echeances")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OWNER')")
    public String verifierEcheancesManuellement(RedirectAttributes redirectAttributes) {
        try {
            // Récupérer l'utilisateur authentifié
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            logger.info("Déclenchement manuel de la vérification des échéances par l'utilisateur: {}", username);
            notificationService.verifierEcheanceReservations();
            redirectAttributes.addFlashAttribute("success", "Vérification des échéances terminée. Notifications créées si nécessaire.");
            logger.info("Vérification des échéances terminée avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors de la vérification des échéances", e);
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la vérification: " + e.getMessage());
        }
        
        return "redirect:/notifications";
    }
    
    /**
     * Marque une notification comme lue
     */
    @PostMapping("/{id}/marquer-lue")
    public String marquerCommeLue(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            notificationService.marquerCommeLue(id);
            redirectAttributes.addFlashAttribute("success", "Notification marquée comme lue");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/notifications";
    }
    
    /**
     * Marque toutes les notifications comme lues
     */
    @PostMapping("/marquer-toutes-lues")
    public String marquerToutesCommeLues(RedirectAttributes redirectAttributes) {
        // Récupérer l'utilisateur authentifié
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé: " + username));
        
        notificationService.marquerToutesCommeLues(user);
        redirectAttributes.addFlashAttribute("success", "Toutes les notifications ont été marquées comme lues");
        
        return "redirect:/notifications";
    }
    
    /**
     * Redirige vers la réservation associée à la notification
     */
    @GetMapping("/{id}/redirect")
    public String redirectToReservation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            notificationService.marquerCommeLue(id);
            // TODO: Rediriger vers la page de détails de la réservation
            // Pour l'instant, on redirige vers la liste des réservations
            return "redirect:/reservations";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/notifications";
        }
    }
}
