package com.goldresto.controller;

import com.goldresto.entity.User;
import com.goldresto.repository.UserRepository;
import com.goldresto.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * ControllerAdvice pour injecter le compteur de notifications dans toutes les pages
 */
@ControllerAdvice
public class NotificationControllerAdvice {
    
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    
    public NotificationControllerAdvice(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }
    
    /**
     * Ajoute le compteur de notifications non lues à tous les modèles
     */
    @ModelAttribute
    public void addNotificationCount(Model model) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                String username = auth.getName();
                
                User user = userRepository.findByUsername(username)
                    .orElse(null);
                
                if (user != null) {
                    long notificationCount = notificationService.countNotificationsNonLues(user);
                    model.addAttribute("notificationCount", notificationCount);
                }
            }
        } catch (Exception e) {
            // En cas d'erreur, ne pas bloquer le chargement de la page
            model.addAttribute("notificationCount", 0);
        }
    }
}
