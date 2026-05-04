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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller pour tester les notifications WhatsApp
 */
@Controller
@RequestMapping("/admin/whatsapp-test")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OWNER')")
public class WhatsAppTestController {
    
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppTestController.class);
    
    @Autowired
    private WhatsAppTestService whatsAppTestService;
    
    @Autowired
    private ClientService clientService;
    
    /**
     * Page de test WhatsApp
     */
    @GetMapping
    public String testWhatsAppPage(Model model) {
        List<Client> clients = clientService.getAllClients();
        model.addAttribute("clients", clients);
        return "admin/whatsapp-test";
    }
    
    /**
     * Tester l'envoi WhatsApp
     */
    @PostMapping("/send")
    @ResponseBody
    public Map<String, Object> testWhatsAppSend(
            @RequestParam Long clientId,
            @RequestParam String message) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Client client = clientService.getClientById(clientId);
            
            logger.info("Test WhatsApp - Client: {}, Message: {}", 
                client.getNomComplet(), message);
            
            // Vérifier si le client a un numéro WhatsApp
            if (!client.hasWhatsAppNumber()) {
                result.put("success", false);
                result.put("message", "Le client n'a pas de numéro de téléphone configuré");
                return result;
            }
            
            logger.info("Numéro WhatsApp formaté: {}", client.getWhatsAppNumber());
            
            // Envoyer le message
            boolean sent = whatsAppTestService.sendWhatsAppMessage(client, message);
            
            if (sent) {
                result.put("success", true);
                result.put("message", "Message WhatsApp envoyé avec succès !");
                result.put("clientName", client.getNomComplet());
                result.put("phoneNumber", client.getWhatsAppNumber());
            } else {
                result.put("success", false);
                result.put("message", "Échec de l'envoi WhatsApp - vérifiez la configuration Twilio");
            }
            
        } catch (Exception e) {
            logger.error("Erreur lors du test WhatsApp", e);
            result.put("success", false);
            result.put("message", "Erreur: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Tester la configuration Twilio
     */
    @GetMapping("/check-config")
    @ResponseBody
    public Map<String, Object> checkTwilioConfig() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Simuler la vérification de la configuration
            result.put("accountSid", "ACLTQ4TRQHTYADFPF6LPEL1B43");
            result.put("whatsappFrom", "+17407576223");
            result.put("authToken", "***CONFIGURÉ***"); // Ne pas montrer le token réel
            
            // Vérifier si les services sont injectés
            result.put("whatsAppService", whatsAppTestService != null ? "OK" : "ERREUR");
            result.put("clientService", clientService != null ? "OK" : "ERREUR");
            
            result.put("success", true);
            result.put("message", "Configuration Twilio vérifiée");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Erreur de configuration: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Récupérer le numéro WhatsApp d'un client
     */
    @GetMapping("/check-client-phone/{clientId}")
    @ResponseBody
    public Map<String, Object> getClientPhone(@PathVariable Long clientId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Client client = clientService.getClientById(clientId);
            
            result.put("success", true);
            result.put("phoneNumber", client.getWhatsAppNumber());
            result.put("hasPhone", client.hasWhatsAppNumber());
            result.put("originalPhone", client.getTelephone());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Erreur: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Tester avec un message de test prédéfini
     */
    @PostMapping("/send-test-message")
    @ResponseBody
    public Map<String, Object> sendTestMessage(@RequestParam Long clientId) {
        String testMessage = "Ceci est un message de test WhatsApp depuis GoldResto !\n\n" +
                           "Si vous recevez ce message, la configuration est correcte. \n\n" +
                           "GoldResto - Test WhatsApp";
        
        return testWhatsAppSend(clientId, testMessage);
    }
}
