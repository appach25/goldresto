package com.goldresto.controller;

import com.goldresto.entity.Client;
import com.goldresto.service.ClientService;
import com.goldresto.service.WhatsAppTestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller pour envoyer des notifications aux clients
 */
@Controller
@RequestMapping("/admin/notifications")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OWNER')")
public class ClientNotificationController {
    
    private static final Logger logger = LoggerFactory.getLogger(ClientNotificationController.class);
    
    @Autowired
    private ClientService clientService;
    
    @Autowired
    private WhatsAppTestService whatsAppTestService;
    
    /**
     * Page d'envoi de notifications
     */
    @GetMapping("/send")
    public String sendNotificationPage(Model model) {
        List<Client> clients = clientService.getAllClients();
        model.addAttribute("clients", clients);
        return "admin/send-notification";
    }
    
    /**
     * Envoyer une notification WhatsApp à un client
     */
    @PostMapping("/send")
    public String sendNotification(
            @RequestParam Long clientId,
            @RequestParam String titre,
            @RequestParam String message,
            @RequestParam(required = false) boolean sendWhatsApp,
            RedirectAttributes redirectAttributes) {
        
        try {
            Client client = clientService.getClientById(clientId);
            
            if (sendWhatsApp) {
                boolean sent = whatsAppTestService.sendCustomMessage(client, titre, message);
                
                if (sent) {
                    redirectAttributes.addFlashAttribute("success", 
                        "Message WhatsApp testé avec succès à " + client.getNom());
                } else {
                    redirectAttributes.addFlashAttribute("error", 
                        "Erreur lors du test WhatsApp");
                }
            } else {
                redirectAttributes.addFlashAttribute("error", 
                    "Aucun canal de notification sélectionné");
            }
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de notification au client {}", clientId, e);
            redirectAttributes.addFlashAttribute("error", 
                "Erreur lors de l'envoi de la notification: " + e.getMessage());
        }
        
        return "redirect:/admin/notifications/send";
    }
    
    /**
     * API pour envoyer un message WhatsApp rapide
     */
    @PostMapping("/api/send-whatsapp")
    @ResponseBody
    public String sendWhatsAppApi(
            @RequestParam Long clientId,
            @RequestParam String message) {
        
        try {
            Client client = clientService.getClientById(clientId);
            
            boolean sent = whatsAppTestService.sendWhatsAppMessage(client, message);
            
            if (sent) {
                return "{\"success\": true, \"message\": \"Message WhatsApp testé avec succès (mode test)\"}";
            } else {
                return "{\"success\": false, \"message\": \"Échec du test WhatsApp\"}";
            }
            
        } catch (Exception e) {
            logger.error("Erreur lors du test WhatsApp API au client {}", clientId, e);
            return "{\"success\": false, \"message\": \"Erreur: " + e.getMessage() + "\"}";
        }
    }
}
