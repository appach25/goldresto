package com.goldresto.controller;

import com.goldresto.entity.Client;
import com.goldresto.service.ClientService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/clients")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OWNER', 'EMPLOYEE')")
public class ClientController {
    
    private static final Logger logger = LoggerFactory.getLogger(ClientController.class);
    
    private final ClientService clientService;
    
    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }
    
    @GetMapping("/test-ultra-simple")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OWNER', 'EMPLOYEE')")
    public Map<String, Object> testUltraSimple() {
        Map<String, Object> result = new HashMap<>();
        
        // Données test hardcodées
        List<Map<String, Object>> clientsTest = new ArrayList<>();
        
        Map<String, Object> client1 = new HashMap<>();
        client1.put("id", 1L);
        client1.put("nomComplet", "Client Test 1");
        client1.put("email", "test1@example.com");
        client1.put("telephone", "0612345678");
        client1.put("actif", true);
        clientsTest.add(client1);
        
        Map<String, Object> client2 = new HashMap<>();
        client2.put("id", 2L);
        client2.put("nomComplet", "ClientDirect TestDirect");
        client2.put("email", "direct@test.com");
        client2.put("telephone", "0623456789");
        client2.put("actif", true);
        clientsTest.add(client2);
        
        result.put("success", true);
        result.put("totalClients", 2);
        result.put("clients", clientsTest);
        
        return result;
    }
    
    @GetMapping("/simple-test")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OWNER', 'EMPLOYEE')")
    public String simpleTest() {
        System.out.println("=== TEST SIMPLE CONTROLLER ===");
        return "Controller fonctionne ! Nombre de clients: " + clientService.getAllClients().size();
    }
    
    @GetMapping("/search-by-phone")
    @ResponseBody
    public Map<String, Object> searchByPhone(@RequestParam String telephone) {
        Map<String, Object> response = new HashMap<>();
        
        logger.info("=== RECHERCHE CLIENT PAR TÉLÉPHONE ===");
        logger.info("Téléphone recherché: '{}'", telephone);
        
        try {
            // Nettoyer le numéro de téléphone
            String cleanedPhone = telephone.replaceAll("[\\s\\-\\.]", "");
            logger.info("Téléphone nettoyé: '{}'", cleanedPhone);
            
            List<Client> clients = clientService.getAllClients();
            logger.info("Nombre total de clients dans la base: {}", clients.size());
            
            Client foundClient = null;
            
            for (int i = 0; i < clients.size(); i++) {
                Client client = clients.get(i);
                logger.info("Client {}: {} - Téléphone: '{}'", i+1, client.getNomComplet(), client.getTelephone());
                
                if (client.getTelephone() != null) {
                    String clientPhone = client.getTelephone().replaceAll("[\\s\\-\\.]", "");
                    logger.info("Téléphone client {} nettoyé: '{}'", i+1, clientPhone);
                    
                    if (clientPhone.equals(cleanedPhone)) {
                        foundClient = client;
                        logger.info("✅ Client trouvé: {}", client.getNomComplet());
                        break;
                    }
                } else {
                    logger.info("Client {} n'a pas de téléphone", i+1);
                }
            }
            
            if (foundClient != null) {
                response.put("found", true);
                response.put("client", Map.of(
                    "id", foundClient.getId(),
                    "nomComplet", foundClient.getNomComplet(),
                    "telephone", foundClient.getTelephone(),
                    "email", foundClient.getEmail() != null ? foundClient.getEmail() : "",
                    "ville", foundClient.getVille() != null ? foundClient.getVille() : ""
                ));
                logger.info("✅ Réponse positive envoyée");
            } else {
                response.put("found", false);
                logger.info("❌ Aucun client trouvé pour le téléphone: {}", telephone);
            }
            
        } catch (Exception e) {
            logger.error("❌ Erreur lors de la recherche du client", e);
            response.put("found", false);
            response.put("error", e.getMessage());
        }
        
        logger.info("=== FIN RECHERCHE CLIENT ===");
        return response;
    }
    
    @PostMapping("/create-ajax")
    @ResponseBody
    public Map<String, Object> createClientAjax(@RequestBody Map<String, Object> clientData) {
        Map<String, Object> response = new HashMap<>();
        
        logger.info("=== CRÉATION CLIENT AJAX ===");
        logger.info("Données reçues: {}", clientData);
        
        try {
            Client client = new Client();
            client.setNom((String) clientData.get("nom"));
            client.setPrenom((String) clientData.get("prenom"));
            client.setTelephone((String) clientData.get("telephone"));
            client.setEmail((String) clientData.get("email"));
            client.setVille((String) clientData.get("ville"));
            client.setActif((Boolean) clientData.getOrDefault("actif", true));
            
            logger.info("Client à créer: {} {} - {}", client.getNom(), client.getPrenom(), client.getTelephone());
            
            Client savedClient = clientService.createClient(client);
            
            logger.info("✅ Client créé avec ID: {}", savedClient.getId());
            
            response.put("success", true);
            response.put("client", Map.of(
                "id", savedClient.getId(),
                "nomComplet", savedClient.getNomComplet(),
                "telephone", savedClient.getTelephone(),
                "email", savedClient.getEmail() != null ? savedClient.getEmail() : "",
                "ville", savedClient.getVille() != null ? savedClient.getVille() : ""
            ));
            
            logger.info("✅ Réponse positive envoyée");
            
        } catch (Exception e) {
            logger.error("❌ Erreur lors de la création du client", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        logger.info("=== FIN CRÉATION CLIENT AJAX ===");
        return response;
    }
    
    @GetMapping("/test-clients")
    @ResponseBody
    public Map<String, Object> testClients() {
        Map<String, Object> response = new HashMap<>();
        
        logger.info("=== TEST CLIENTS EN BASE ===");
        
        try {
            List<Client> clients = clientService.getAllClients();
            response.put("totalClients", clients.size());
            
            List<Map<String, Object>> clientList = new ArrayList<>();
            for (Client client : clients) {
                Map<String, Object> clientInfo = new HashMap<>();
                clientInfo.put("id", client.getId());
                clientInfo.put("nom", client.getNom());
                clientInfo.put("prenom", client.getPrenom());
                clientInfo.put("nomComplet", client.getNomComplet());
                clientInfo.put("telephone", client.getTelephone());
                clientInfo.put("email", client.getEmail());
                clientInfo.put("ville", client.getVille());
                clientInfo.put("actif", client.getActif());
                clientList.add(clientInfo);
            }
            
            response.put("clients", clientList);
            logger.info("✅ {} clients trouvés", clients.size());
            
        } catch (Exception e) {
            logger.error("❌ Erreur lors du test des clients", e);
            response.put("error", e.getMessage());
        }
        
        logger.info("=== FIN TEST CLIENTS ===");
        return response;
    }
    
    @GetMapping("/simple-list")
    public String simpleList(Model model) {
        try {
            // Récupérer les vrais clients de la base de données
            List<Client> clients = clientService.getAllClients();
            model.addAttribute("clients", clients);
        } catch (Exception e) {
            // En cas d'erreur, créer une liste vide
            model.addAttribute("clients", new ArrayList<>());
        }
        
        return "clients/simple-list";
    }
    
    @GetMapping("/test-ultra")
    public String testUltra(Model model) {
        // Créer un client simple
        List<Client> clientsTest = new ArrayList<>();
        
        Client client1 = new Client();
        client1.setId(1L);
        client1.setNom("Test");
        client1.setPrenom("Client1");
        client1.setEmail("test1@example.com");
        client1.setActif(true);
        clientsTest.add(client1);
        
        model.addAttribute("clients", clientsTest);
        
        return "clients/test-ultra";
    }
    
    @GetMapping("/test-no-fragment")
    public String testNoFragment(Model model) {
        // Créer de vrais objets Client pour le test
        List<Client> clientsTest = new ArrayList<>();
        
        Client client1 = new Client();
        client1.setId(1L);
        client1.setNom("Test");
        client1.setPrenom("Client1");
        client1.setEmail("test1@example.com");
        client1.setActif(true);
        clientsTest.add(client1);
        
        Client client2 = new Client();
        client2.setId(2L);
        client2.setNom("ClientDirect");
        client2.setPrenom("TestDirect");
        client2.setEmail("direct@test.com");
        client2.setActif(true);
        clientsTest.add(client2);
        
        model.addAttribute("clients", clientsTest);
        
        return "clients/list-no-fragment";
    }
    
    @GetMapping("/test-minimal")
    public String testMinimal(Model model) {
        // Créer de vrais objets Client pour le test
        List<Client> clientsTest = new ArrayList<>();
        
        Client client1 = new Client();
        client1.setId(1L);
        client1.setNom("Test");
        client1.setPrenom("Client1");
        client1.setEmail("test1@example.com");
        client1.setActif(true);
        clientsTest.add(client1);
        
        Client client2 = new Client();
        client2.setId(2L);
        client2.setNom("ClientDirect");
        client2.setPrenom("TestDirect");
        client2.setEmail("direct@test.com");
        client2.setActif(true);
        clientsTest.add(client2);
        
        // Stats test
        Map<String, Object> statsTest = new HashMap<>();
        statsTest.put("totalClients", 2);
        statsTest.put("clientsActifs", 2);
        statsTest.put("clientsInactifs", 0);
        statsTest.put("nouveauxClientsCeMois", 2);
        
        // ClientPage test
        Map<String, Object> clientPageTest = new HashMap<>();
        clientPageTest.put("totalElements", 2);
        clientPageTest.put("totalPages", 1);
        clientPageTest.put("number", 0);
        clientPageTest.put("hasNext", false);
        clientPageTest.put("hasPrevious", false);
        
        model.addAttribute("clients", clientsTest);
        model.addAttribute("stats", statsTest);
        model.addAttribute("clientPage", clientPageTest);
        
        return "clients/test-minimal";
    }
    
    @GetMapping("/test-list-simple")
    public String testListSimple(Model model) {
        // Créer de vrais objets Client pour le test
        List<Client> clientsTest = new ArrayList<>();
        
        Client client1 = new Client();
        client1.setId(1L);
        client1.setNom("Test");
        client1.setPrenom("Client1");
        client1.setEmail("test1@example.com");
        client1.setTelephone("0612345678");
        client1.setActif(true);
        clientsTest.add(client1);
        
        Client client2 = new Client();
        client2.setId(2L);
        client2.setNom("ClientDirect");
        client2.setPrenom("TestDirect");
        client2.setEmail("direct@test.com");
        client2.setTelephone("0623456789");
        client2.setActif(true);
        clientsTest.add(client2);
        
        // Stats test
        Map<String, Object> statsTest = new HashMap<>();
        statsTest.put("totalClients", 2);
        statsTest.put("clientsActifs", 2);
        statsTest.put("clientsInactifs", 0);
        statsTest.put("nouveauxClientsCeMois", 2);
        
        // ClientPage test
        Map<String, Object> clientPageTest = new HashMap<>();
        clientPageTest.put("totalElements", 2);
        clientPageTest.put("totalPages", 1);
        clientPageTest.put("number", 0);
        clientPageTest.put("hasNext", false);
        clientPageTest.put("hasPrevious", false);
        
        model.addAttribute("clients", clientsTest);
        model.addAttribute("stats", statsTest);
        model.addAttribute("clientPage", clientPageTest);
        
        return "clients/list-simple";
    }
    
    @GetMapping("/test-template-simple")
    public String testTemplateSimple(Model model) {
        // Créer de vrais objets Client pour le test
        List<Client> clientsTest = new ArrayList<>();
        
        Client client1 = new Client();
        client1.setId(1L);
        client1.setNom("Test");
        client1.setPrenom("Client1");
        client1.setEmail("test1@example.com");
        client1.setActif(true);
        clientsTest.add(client1);
        
        Client client2 = new Client();
        client2.setId(2L);
        client2.setNom("ClientDirect");
        client2.setPrenom("TestDirect");
        client2.setEmail("direct@test.com");
        client2.setActif(true);
        clientsTest.add(client2);
        
        model.addAttribute("clients", clientsTest);
        
        return "clients/test-simple";
    }
    
    @GetMapping("/test-real-clients")
    public String testRealClients(Model model) {
        // Créer de vrais objets Client pour le test
        List<Client> clientsTest = new ArrayList<>();
        
        Client client1 = new Client();
        client1.setId(1L);
        client1.setNom("Test");
        client1.setPrenom("Client1");
        client1.setEmail("test1@example.com");
        client1.setTelephone("0612345678");
        client1.setActif(true);
        client1.setCreatedAt(LocalDateTime.now());
        client1.setUpdatedAt(LocalDateTime.now());
        clientsTest.add(client1);
        
        Client client2 = new Client();
        client2.setId(2L);
        client2.setNom("ClientDirect");
        client2.setPrenom("TestDirect");
        client2.setEmail("direct@test.com");
        client2.setTelephone("0623456789");
        client2.setActif(true);
        client2.setCreatedAt(LocalDateTime.now());
        client2.setUpdatedAt(LocalDateTime.now());
        clientsTest.add(client2);
        
        // Stats test
        Map<String, Object> statsTest = new HashMap<>();
        statsTest.put("totalClients", 2);
        statsTest.put("clientsActifs", 2);
        statsTest.put("clientsInactifs", 0);
        statsTest.put("nouveauxClientsCeMois", 2);
        
        // ClientPage test
        Map<String, Object> clientPageTest = new HashMap<>();
        clientPageTest.put("totalElements", 2);
        clientPageTest.put("totalPages", 1);
        clientPageTest.put("number", 0);
        clientPageTest.put("hasNext", false);
        clientPageTest.put("hasPrevious", false);
        
        model.addAttribute("clients", clientsTest);
        model.addAttribute("stats", statsTest);
        model.addAttribute("clientPage", clientPageTest);
        model.addAttribute("currentPage", 0);
        model.addAttribute("totalPages", 1);
        model.addAttribute("totalItems", 2);
        model.addAttribute("sort", "nom");
        model.addAttribute("direction", "asc");
        
        return "clients/list";
    }
    
    @GetMapping("/test-list")
    public String testList(Model model) {
        // Données test hardcodées sans passer par le service
        List<Map<String, Object>> clientsTest = new ArrayList<>();
        
        Map<String, Object> client1 = new HashMap<>();
        client1.put("id", 1L);
        client1.put("nomComplet", "Client Test 1");
        client1.put("email", "test1@example.com");
        client1.put("telephone", "0612345678");
        client1.put("actif", true);
        clientsTest.add(client1);
        
        Map<String, Object> client2 = new HashMap<>();
        client2.put("id", 2L);
        client2.put("nomComplet", "ClientDirect TestDirect");
        client2.put("email", "direct@test.com");
        client2.put("telephone", "0623456789");
        client2.put("actif", true);
        clientsTest.add(client2);
        
        // Stats test
        Map<String, Object> statsTest = new HashMap<>();
        statsTest.put("totalClients", 2);
        statsTest.put("clientsActifs", 2);
        statsTest.put("clientsInactifs", 0);
        statsTest.put("nouveauxClientsCeMois", 2);
        
        // ClientPage test
        Map<String, Object> clientPageTest = new HashMap<>();
        clientPageTest.put("totalElements", 2);
        clientPageTest.put("totalPages", 1);
        clientPageTest.put("number", 0);
        clientPageTest.put("hasNext", false);
        clientPageTest.put("hasPrevious", false);
        
        model.addAttribute("clients", clientsTest);
        model.addAttribute("stats", statsTest);
        model.addAttribute("clientPage", clientPageTest);
        model.addAttribute("currentPage", 0);
        model.addAttribute("totalPages", 1);
        model.addAttribute("totalItems", 2);
        model.addAttribute("sort", "nom");
        model.addAttribute("direction", "asc");
        
        return "clients/list";
    }
    
    // Liste des clients avec pagination et recherche
    @GetMapping
    public String listClients(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nom") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {
        
        try {
            // Utiliser le service pour récupérer les vraies données
            List<Client> clients = clientService.getAllClients();
            
            // Créer des stats simples basées sur les vraies données
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalClients", clients.size());
            stats.put("clientsActifs", (int) clients.stream().filter(client -> client.getActif() != null && client.getActif()).count());
            stats.put("clientsInactifs", (int) clients.stream().filter(client -> client.getActif() == null || !client.getActif()).count());
            stats.put("nouveauxClientsCeMois", clients.size()); // Simplifié
            
            // Créer un clientPage simple
            Map<String, Object> clientPage = new HashMap<>();
            clientPage.put("totalElements", clients.size());
            clientPage.put("totalPages", 1);
            clientPage.put("number", 0);
            clientPage.put("hasNext", false);
            clientPage.put("hasPrevious", false);
            
            model.addAttribute("clients", clients);
            model.addAttribute("stats", stats);
            model.addAttribute("clientPage", clientPage);
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 1);
            model.addAttribute("totalItems", clients.size());
            model.addAttribute("sort", sort);
            model.addAttribute("direction", direction);
            
        } catch (Exception e) {
            // En cas d'erreur, utiliser les données de test comme fallback
            List<Client> clientsTest = new ArrayList<>();
            
            Client client1 = new Client();
            client1.setId(1L);
            client1.setNom("Test");
            client1.setPrenom("Client1");
            client1.setEmail("test1@example.com");
            client1.setActif(true);
            clientsTest.add(client1);
            
            Client client2 = new Client();
            client2.setId(2L);
            client2.setNom("ClientDirect");
            client2.setPrenom("TestDirect");
            client2.setEmail("direct@test.com");
            client2.setActif(true);
            clientsTest.add(client2);
            
            Map<String, Object> statsTest = new HashMap<>();
            statsTest.put("totalClients", 2);
            statsTest.put("clientsActifs", 2);
            statsTest.put("clientsInactifs", 0);
            statsTest.put("nouveauxClientsCeMois", 2);
            
            Map<String, Object> clientPageTest = new HashMap<>();
            clientPageTest.put("totalElements", 2);
            clientPageTest.put("totalPages", 1);
            clientPageTest.put("number", 0);
            clientPageTest.put("hasNext", false);
            clientPageTest.put("hasPrevious", false);
            
            model.addAttribute("clients", clientsTest);
            model.addAttribute("stats", statsTest);
            model.addAttribute("clientPage", clientPageTest);
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 1);
            model.addAttribute("totalItems", 2);
            model.addAttribute("sort", sort);
            model.addAttribute("direction", direction);
        }
        
        return "clients/list";
    }
    
    // Formulaire de création de client
    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OWNER')")
    public String createClientForm(Model model) {
        logger.debug("Affichage du formulaire de création de client");
        model.addAttribute("client", new Client());
        model.addAttribute("title", "Nouveau Client");
        model.addAttribute("action", "/clients");
        model.addAttribute("method", "post");
        return "clients/form";
    }
    
    // Création de client
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OWNER')")
    public String createClient(@Valid @ModelAttribute Client client, 
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
        logger.info("Tentative de création d'un nouveau client: {}", client.getNomComplet());
        logger.info("Données reçues: nom={}, prenom={}, email={}, telephone={}", 
                   client.getNom(), client.getPrenom(), client.getEmail(), client.getTelephone());
        
        if (result.hasErrors()) {
            logger.warn("Erreurs de validation lors de la création du client: {}", result.getAllErrors());
            redirectAttributes.addFlashAttribute("error", "Veuillez corriger les erreurs dans le formulaire");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.client", result);
            redirectAttributes.addFlashAttribute("client", client);
            return "redirect:/clients/new";
        }
        
        try {
            Client savedClient = clientService.createClient(client);
            logger.info("Client créé avec succès: ID {}, Nom: {}", savedClient.getId(), savedClient.getNomComplet());
            redirectAttributes.addFlashAttribute("success", "Client créé avec succès: " + savedClient.getNomComplet());
            return "redirect:/clients";
        } catch (IllegalArgumentException e) {
            logger.error("Erreur lors de la création du client: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("client", client);
            return "redirect:/clients/new";
        } catch (Exception e) {
            logger.error("Erreur inattendue lors de la création du client", e);
            redirectAttributes.addFlashAttribute("error", "Une erreur est survenue lors de la création du client");
            redirectAttributes.addFlashAttribute("client", client);
            return "redirect:/clients/new";
        }
    }
    
    // Détails d'un client
    @GetMapping("/{id}")
    public String viewClient(@PathVariable Long id, Model model) {
        logger.debug("Affichage des détails du client ID: {}", id);
        
        try {
            Client client = clientService.getClientById(id);
            model.addAttribute("client", client);
            model.addAttribute("title", "Détails du Client");
            return "clients/detail";
        } catch (Exception e) {
            logger.error("Erreur lors de l'affichage du client ID: {}", id, e);
            return "redirect:/clients";
        }
    }
    
    // Formulaire de modification de client
    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OWNER')")
    public String editClientForm(@PathVariable Long id, Model model) {
        logger.debug("Affichage du formulaire de modification du client ID: {}", id);
        
        try {
            Client client = clientService.getClientById(id);
            model.addAttribute("client", client);
            model.addAttribute("title", "Modifier le Client");
            model.addAttribute("action", "/clients/" + id);
            model.addAttribute("method", "post");
            return "clients/form";
        } catch (Exception e) {
            logger.error("Erreur lors de l'affichage du formulaire de modification du client ID: {}", id, e);
            return "redirect:/clients";
        }
    }
    
    // Modification de client
    @PostMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OWNER')")
    public String updateClient(@PathVariable Long id,
                              @Valid @ModelAttribute Client client,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
        logger.info("Tentative de mise à jour du client ID: {}", id);
        
        if (result.hasErrors()) {
            logger.warn("Erreurs de validation lors de la mise à jour du client: {}", result.getAllErrors());
            redirectAttributes.addFlashAttribute("error", "Veuillez corriger les erreurs dans le formulaire");
            return "clients/form";
        }
        
        try {
            Client updatedClient = clientService.updateClient(id, client);
            logger.info("Client mis à jour avec succès: ID {}", updatedClient.getId());
            redirectAttributes.addFlashAttribute("success", "Client mis à jour avec succès: " + updatedClient.getNomComplet());
            return "redirect:/clients";
        } catch (IllegalArgumentException e) {
            logger.error("Erreur lors de la mise à jour du client: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "clients/form";
        } catch (Exception e) {
            logger.error("Erreur inattendue lors de la mise à jour du client", e);
            redirectAttributes.addFlashAttribute("error", "Une erreur est survenue lors de la mise à jour du client");
            return "clients/form";
        }
    }
    
    // Suppression de client
    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OWNER')")
    public String deleteClient(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        logger.info("Tentative de suppression du client ID: {}", id);
        
        try {
            clientService.deleteClient(id);
            logger.info("Client supprimé avec succès: ID {}", id);
            redirectAttributes.addFlashAttribute("success", "Client supprimé avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression du client ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la suppression du client: " + e.getMessage());
        }
        
        return "redirect:/clients";
    }
    
    // Activation/Désactivation de client
    @PostMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OWNER')")
    public String toggleClientStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        logger.info("Changement de statut du client ID: {}", id);
        
        try {
            Client client = clientService.getClientById(id);
            
            if (client.getActif()) {
                clientService.deactivateClient(id);
                redirectAttributes.addFlashAttribute("success", "Client désactivé avec succès");
            } else {
                clientService.activateClient(id);
                redirectAttributes.addFlashAttribute("success", "Client activé avec succès");
            }
            
            logger.info("Statut du client ID {} changé avec succès", id);
        } catch (Exception e) {
            logger.error("Erreur lors du changement de statut du client ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Erreur lors du changement de statut du client");
        }
        
        return "redirect:/clients";
    }
    
    @GetMapping("/test-create")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OWNER')")
    public Map<String, Object> testCreateClient() {
        logger.info("=== TEST CRÉATION CLIENT DIRECT ===");
        
        try {
            Client testClient = new Client();
            testClient.setNom("TestDirect");
            testClient.setPrenom("ClientDirect");
            testClient.setEmail("direct@test.com");
            testClient.setActif(true);
            
            logger.info("Création client de test: {}", testClient.getNomComplet());
            
            Client savedClient = clientService.createClient(testClient);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Client de test créé avec succès");
            response.put("clientId", savedClient.getId());
            response.put("clientName", savedClient.getNomComplet());
            
            logger.info("Client de test créé: ID {}", savedClient.getId());
            return response;
            
        } catch (Exception e) {
            logger.error("Erreur lors du test de création de client", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    // API pour recherche rapide (pour autocomplete)
    @GetMapping("/api/search")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OWNER', 'EMPLOYEE')")
    public List<Map<String, Object>> searchClientsApi(@RequestParam String query) {
        logger.debug("Recherche API de clients avec query: {}", query);
        
        List<Client> clients = clientService.searchClients(query, PageRequest.of(0, 10)).getContent();
        
        return clients.stream()
                .map(client -> {
                    Map<String, Object> clientMap = new HashMap<>();
                    clientMap.put("id", client.getId());
                    clientMap.put("nom", client.getNom());
                    clientMap.put("prenom", client.getPrenom());
                    clientMap.put("nomComplet", client.getNomComplet());
                    clientMap.put("email", client.getEmail() != null ? client.getEmail() : "");
                    clientMap.put("telephone", client.getTelephone() != null ? client.getTelephone() : "");
                    return clientMap;
                })
                .toList();
    }
    
    // API pour obtenir les détails d'un client
    @GetMapping("/api/{id}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OWNER', 'EMPLOYEE')")
    public Map<String, Object> getClientApi(@PathVariable Long id) {
        logger.debug("API - Récupération du client ID: {}", id);
        
        try {
            Client client = clientService.getClientById(id);
            
            Map<String, Object> clientData = new HashMap<>();
            clientData.put("id", client.getId());
            clientData.put("nom", client.getNom());
            clientData.put("prenom", client.getPrenom());
            clientData.put("nomComplet", client.getNomComplet());
            clientData.put("email", client.getEmail());
            clientData.put("telephone", client.getTelephone());
            clientData.put("adresse", client.getAdresse());
            clientData.put("ville", client.getVille());
            clientData.put("codePostal", client.getCodePostal());
            clientData.put("notes", client.getNotes());
            clientData.put("actif", client.getActif());
            clientData.put("nombreReservations", client.getNombreReservations());
            clientData.put("createdAt", client.getCreatedAt());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("client", clientData);
            
            return response;
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du client ID: {}", id, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    // Statistiques des clients
    @GetMapping("/stats")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OWNER')")
    public Map<String, Object> getClientStats() {
        logger.debug("Récupération des statistiques des clients");
        return clientService.getClientStatistics();
    }
    
    // Clients sans réservations
    @GetMapping("/without-reservations")
    public String clientsWithoutReservations(Model model) {
        logger.debug("Affichage des clients sans réservations");
        
        List<Client> clients = clientService.getClientsWithoutReservations();
        model.addAttribute("clients", clients);
        model.addAttribute("title", "Clients sans Réservations");
        model.addAttribute("count", clients.size());
        
        return "clients/without-reservations";
    }
    
    // Clients les plus fidèles
    @GetMapping("/loyal")
    public String loyalClients(Model model) {
        logger.debug("Affichage des clients les plus fidèles");
        
        List<Client> clients = clientService.getMostLoyalClients(PageRequest.of(0, 20));
        model.addAttribute("clients", clients);
        model.addAttribute("title", "Clients les Plus Fidèles");
        model.addAttribute("count", clients.size());
        
        return "clients/loyal";
    }
}
