package com.goldresto.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller pour la page de test hors ligne
 */
@Controller
@RequestMapping("/admin")
public class OfflineTestController {
    
    /**
     * Page de test WhatsApp hors ligne
     */
    @GetMapping("/whatsapp-offline-test")
    public String offlineTestPage(Model model) {
        return "admin/whatsapp-offline-test";
    }
}
