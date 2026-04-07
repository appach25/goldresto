package com.goldresto.controller;

import com.goldresto.entity.Client;
import com.goldresto.entity.Produit;
import com.goldresto.entity.Reservation;
import com.goldresto.entity.User;
import com.goldresto.repository.ClientRepository;
import com.goldresto.repository.ProduitRepository;
import com.goldresto.repository.ReservationRepository;
import com.goldresto.repository.UserRepository;
import com.goldresto.service.ClientService;
import com.goldresto.service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/reservations")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'OWNER')")
public class ReservationController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReservationController.class);
    
    @Autowired
    private ReservationService reservationService;
    
    @Autowired
    private ReservationRepository reservationRepository;
    
    @Autowired
    private ProduitRepository produitRepository;
    
    @Autowired
    private ClientService clientService;
    
    @Autowired
    private ClientRepository clientRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping
    public String listReservations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) String view,
            Model model) {
        
        List<Reservation> reservations;
        LocalDate selectedDate;
        
        if (clientId != null) {
            // Recherche par client
            Client client = clientRepository.findById(clientId).orElse(null);
            if (client != null) {
                reservations = reservationRepository.findByClient(client);
                model.addAttribute("searchType", "client");
                model.addAttribute("searchClient", client.getNomComplet());
            } else {
                reservations = Collections.emptyList();
            }
            selectedDate = LocalDate.now();
        } else if (search != null && search.equals("client")) {
            // Redirection vers la recherche par client
            reservations = reservationService.getReservationsByDate(LocalDate.now());
            selectedDate = LocalDate.now();
        } else if ("all".equals(view)) {
            // Afficher toutes les réservations
            reservations = reservationService.getAllReservations();
            selectedDate = LocalDate.now();
            model.addAttribute("view", "all");
        } else {
            // Recherche normale par date
            selectedDate = date != null ? date : LocalDate.now();
            reservations = reservationService.getReservationsByDate(selectedDate);
        }
        
        logger.info("Affichage de {} réservations pour la date {}", reservations.size(), selectedDate);
        
        model.addAttribute("reservations", reservations);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("allClients", clientService.getAllClients());
        
        return "reservations/list";
    }
    
    @GetMapping("/debug")
    @ResponseBody
    public Map<String, Object> debugReservations() {
        Map<String, Object> debug = new HashMap<>();
        
        try {
            List<Reservation> allReservations = reservationRepository.findAll();
            List<Client> allClients = clientRepository.findAll();
            List<Produit> allProduits = produitRepository.findAll();
            
            debug.put("totalReservations", allReservations.size());
            debug.put("totalClients", allClients.size());
            debug.put("totalProduits", allProduits.size());
            
            // Détails des réservations
            List<Map<String, Object>> reservationDetails = new ArrayList<>();
            for (Reservation r : allReservations) {
                Map<String, Object> details = new HashMap<>();
                details.put("id", r.getId());
                details.put("produit", r.getProduit() != null ? r.getProduit().getNomProduit() : "NULL");
                details.put("client", r.getClient() != null ? r.getClient().getNomComplet() : "NULL");
                details.put("quantite", r.getQuantite());
                details.put("date", r.getDateReservation());
                details.put("status", r.getStatus());
                reservationDetails.add(details);
            }
            debug.put("reservations", reservationDetails);
            
            // Détails des clients
            List<Map<String, Object>> clientDetails = new ArrayList<>();
            for (Client c : allClients) {
                Map<String, Object> details = new HashMap<>();
                details.put("id", c.getId());
                details.put("nom", c.getNomComplet());
                details.put("telephone", c.getTelephone());
                clientDetails.add(details);
            }
            debug.put("clients", clientDetails);
            
        } catch (Exception e) {
            debug.put("error", e.getMessage());
            e.printStackTrace();
        }
        
        return debug;
    }
    
    @GetMapping("/calendar")
    public String calendarView(Model model) {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        
        List<Reservation> monthReservations = reservationRepository.findByDateRange(startOfMonth, endOfMonth);
        model.addAttribute("reservations", monthReservations);
        model.addAttribute("currentDate", today);
        
        return "reservations/calendar";
    }
    
    @GetMapping("/new")
    public String createReservationForm(Model model) {
        model.addAttribute("reservation", new Reservation());
        model.addAttribute("produits", produitRepository.findAll());
        model.addAttribute("today", LocalDate.now());
        return "reservations/form-improved";
    }
    
    @PostMapping("/test-submit")
    @ResponseBody
    public Map<String, Object> testSubmit(@RequestParam Map<String, String> allParams) {
        Map<String, Object> response = new HashMap<>();
        
        logger.info("=== TEST SUBMISSION ===");
        logger.info("All parameters received: {}", allParams);
        
        response.put("receivedParams", allParams);
        response.put("paramCount", allParams.size());
        
        // Check for specific parameters
        response.put("hasProduitId", allParams.containsKey("produitId"));
        response.put("hasClientId", allParams.containsKey("clientId"));
        response.put("hasQuantite", allParams.containsKey("quantite"));
        response.put("hasDate", allParams.containsKey("dateReservation"));
        
        logger.info("=== END TEST SUBMISSION ===");
        return response;
    }
    
    @PostMapping("/**")
    @ResponseBody
    public Map<String, Object> catchAllPost(HttpServletRequest request, @RequestParam Map<String, String> allParams) {
        Map<String, Object> response = new HashMap<>();
        
        logger.info("=== CATCH ALL POST TO RESERVATIONS ===");
        logger.info("Request URI: {}", request.getRequestURI());
        logger.info("Method: {}", request.getMethod());
        logger.info("Content Type: {}", request.getContentType());
        logger.info("All parameters: {}", allParams);
        
        response.put("uri", request.getRequestURI());
        response.put("method", request.getMethod());
        response.put("contentType", request.getContentType());
        response.put("parameters", allParams);
        response.put("paramCount", allParams.size());
        response.put("timestamp", System.currentTimeMillis());
        
        // Check for specific parameters
        response.put("hasProduitId", allParams.containsKey("produitId"));
        response.put("hasClientId", allParams.containsKey("clientId"));
        response.put("hasQuantite", allParams.containsKey("quantite"));
        response.put("hasDate", allParams.containsKey("dateReservation"));
        
        logger.info("=== END CATCH ALL POST ===");
        return response;
    }
    
    @PostMapping
    public String saveReservation(@ModelAttribute Reservation reservation,
                                 @RequestParam Long produitId,
                                 @RequestParam(required = false) Long clientId,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        
        logger.info("=== DÉBUT CRÉATION RÉSERVATION ===");
        logger.info("Reçu: Produit ID={}, Client ID={}, Quantité={}, Date={}, Notes={}", 
                   produitId, clientId, reservation.getQuantite(), 
                   reservation.getDateReservation(), reservation.getNotes());
        logger.info("Reservation object: {}", reservation);
        
        try {
            if (clientId == null) {
                logger.error("❌ Client ID est null !");
                redirectAttributes.addFlashAttribute("error", "Veuillez sélectionner un client");
                return "redirect:/reservations/new";
            }
            
            logger.info("✅ Client ID reçu: {}", clientId);
            
            Produit produit = produitRepository.findById(produitId)
                    .orElseThrow(() -> {
                        logger.error("❌ Produit non trouvé: {}", produitId);
                        return new IllegalArgumentException("Produit non trouvé");
                    });
            Client client = clientRepository.findById(clientId)
                    .orElseThrow(() -> {
                        logger.error("❌ Client non trouvé: {}", clientId);
                        return new IllegalArgumentException("Client non trouvé");
                    });
            
            logger.info("✅ Produit trouvé: {} (stock: {})", produit.getNomProduit(), produit.getStock());
            logger.info("✅ Client trouvé: {}", client.getNomComplet());
            
            // Récupérer l'utilisateur connecté
            String username = principal.getName();
            User user = userRepository.findByUsername(username)
                    .orElse(null);
            
            logger.info("✅ Utilisateur: {}", username);
            
            Reservation savedReservation = reservationService.createReservation(
                produitId, 
                reservation.getQuantite(), 
                reservation.getDateReservation(), 
                user, 
                reservation.getNotes(),
                client
            );
            
            logger.info("✅ Réservation créée avec succès - ID: {}", savedReservation.getId());
            logger.info("   - Client: {}", savedReservation.getClient() != null ? savedReservation.getClient().getNomComplet() : "NULL");
            logger.info("   - Produit: {}", savedReservation.getProduit().getNomProduit());
            logger.info("   - Quantité: {}", savedReservation.getQuantite());
            logger.info("   - Date: {}", savedReservation.getDateReservation());
            logger.info("   - Status: {}", savedReservation.getStatus());
            logger.info("=== FIN CRÉATION RÉSERVATION - SUCCÈS ===");
            
            redirectAttributes.addFlashAttribute("success", "Réservation créée avec succès - ID: " + savedReservation.getId());
            return "redirect:/reservations";
            
        } catch (Exception e) {
            logger.error("❌ ERREUR CRÉATION RÉSERVATION", e);
            logger.error("Message d'erreur: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la création: " + e.getMessage());
            return "redirect:/reservations/new";
        }
    }
    
    @PostMapping("/{id}/confirm")
    public String confirmReservation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            reservationService.confirmReservation(id);
            redirectAttributes.addFlashAttribute("success", "Réservation confirmée");
        } catch (Exception e) {
            logger.error("Error confirming reservation", e);
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/reservations";
    }
    
    @PostMapping("/{id}/cancel")
    public String cancelReservation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            reservationService.cancelReservation(id);
            redirectAttributes.addFlashAttribute("success", "Réservation annulée");
        } catch (Exception e) {
            logger.error("Error cancelling reservation", e);
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/reservations";
    }
    
    @PostMapping("/{id}/complete")
    public String completeReservation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            reservationService.completeReservation(id);
            redirectAttributes.addFlashAttribute("success", "Réservation terminée");
        } catch (Exception e) {
            logger.error("Error completing reservation", e);
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/reservations";
    }
    
    @GetMapping("/check-availability")
    @ResponseBody
    public String checkAvailability(@RequestParam Long produitId, 
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                   @RequestParam Integer quantity) {
        try {
            Integer available = reservationService.getAvailableStock(produitId, date);
            return available >= quantity ? "AVAILABLE" : "INSUFFICIENT:" + available;
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }
}
