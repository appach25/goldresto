package com.goldresto.service;

import com.goldresto.entity.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service de simulation WhatsApp pour tester sans Auth Token
 */
@Service
public class WhatsAppSimulationService {
    
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppSimulationService.class);
    
    @Value("${twilio.accountSid:}")
    private String accountSid;
    
    @Value("${twilio.authToken:}")
    private String authToken;
    
    /**
     * Simuler l'envoi WhatsApp (pour les tests)
     */
    public boolean simulateWhatsAppMessage(Client client, String message) {
        logger.info("=== SIMULATION WHATSAPP ===");
        logger.info("Client: {}", client.getNomComplet());
        logger.info("Numéro: {}", client.getWhatsAppNumber());
        logger.info("Message: {}", message);
        logger.info("Account SID: {}", accountSid);
        logger.info("Auth Token: {}", authToken != null && !authToken.isEmpty() ? "***CONFIGURÉ***" : "***NON CONFIGURÉ***");
        
        // Simuler un délai d'envoi
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        logger.info("=== FIN SIMULATION WHATSAPP ===");
        return true;
    }
    
    /**
     * Vérifier si la configuration est complète
     */
    public boolean isConfigurationComplete() {
        return accountSid != null && !accountSid.isEmpty() &&
               authToken != null && !authToken.isEmpty() &&
               !authToken.equals("AJOUTE_ICI_TON_AUTH_TOKEN");
    }
    
    /**
     * Obtenir le statut de la configuration
     */
    public String getConfigurationStatus() {
        if (isConfigurationComplete()) {
            return "Configuration complète - prêt à envoyer";
        } else if (accountSid != null && !accountSid.isEmpty()) {
            return "Account SID configuré - Auth Token manquant";
        } else {
            return "Configuration incomplète";
        }
    }
}
