package com.goldresto.service;

import com.goldresto.entity.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service WhatsApp de test (sans Twilio) pour démonstration
 */
@Service
public class WhatsAppTestService {
    
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppTestService.class);
    
    @Value("${twilio.accountSid:}")
    private String accountSid;
    
    @Value("${twilio.authToken:}")
    private String authToken;
    
    @Value("${twilio.whatsappFrom:+17407576223}")
    private String whatsappFrom;
    
    /**
     * Simuler l'envoi WhatsApp
     */
    public boolean sendWhatsAppMessage(Client client, String message) {
        logger.info("=== TEST WHATSAPP ===");
        logger.info("Client: {}", client.getNomComplet());
        logger.info("Numéro WhatsApp: {}", client.getWhatsAppNumber());
        logger.info("Message: {}", message);
        logger.info("Account SID: {}", accountSid);
        logger.info("WhatsApp From: {}", whatsappFrom);
        logger.info("Auth Token configuré: {}", isAuthTokenConfigured());
        
        // Simuler un délai d'envoi
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        logger.info("=== FIN TEST WHATSAPP ===");
        return true;
    }
    
    /**
     * Envoyer une notification de réservation confirmée
     */
    public boolean sendReservationConfirmation(Client client, Long reservationId) {
        String message = String.format(
            "Bonjour %s !\n\n" +
            "Votre réservation #%d chez GoldResto est confirmée !\n" +
            "Merci de votre confiance et à très bientôt !\n\n" +
            "GoldResto",
            client.getPrenom(),
            reservationId
        );
        
        return sendWhatsAppMessage(client, message);
    }
    
    /**
     * Envoyer une notification d'échéance
     */
    public boolean sendReservationEcheance(Client client, Long reservationId) {
        String message = String.format(
            "Bonjour %s !\n\n" +
            "Votre réservation #%d arrive à échéance.\n" +
            "Pensez à la renouveler ou à passer nous voir !\n\n" +
            "GoldResto",
            client.getPrenom(),
            reservationId
        );
        
        return sendWhatsAppMessage(client, message);
    }
    
    /**
     * Envoyer une promotion
     */
    public boolean sendPromotion(Client client, String promotion) {
        String message = String.format(
            "Bonjour %s !\n\n" +
            "Offre spéciale chez GoldResto :\n" +
            "%s\n\n" +
            "Profitez-en vite !\n" +
            "GoldResto",
            client.getPrenom(),
            promotion
        );
        
        return sendWhatsAppMessage(client, message);
    }
    
    /**
     * Envoyer un message personnalisé
     */
    public boolean sendCustomMessage(Client client, String titre, String message) {
        String fullMessage = String.format(
            "Bonjour %s !\n\n" +
            "%s\n\n" +
            "%s\n\n" +
            "GoldResto",
            client.getPrenom(),
            titre,
            message
        );
        
        return sendWhatsAppMessage(client, fullMessage);
    }
    
    /**
     * Vérifier si l'Auth Token est configuré
     */
    private boolean isAuthTokenConfigured() {
        return authToken != null && 
               !authToken.isEmpty() && 
               !authToken.equals("AJOUTE_ICI_TON_AUTH_TOKEN") &&
               !authToken.equals("your_auth_token_here");
    }
    
    /**
     * Obtenir le statut de la configuration
     */
    public String getConfigurationStatus() {
        if (isAuthTokenConfigured()) {
            return "Configuration complète - prêt pour Twilio";
        } else {
            return "Auth Token manquant - mode test activé";
        }
    }
}
