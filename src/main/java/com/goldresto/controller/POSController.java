package com.goldresto.controller;

import java.util.List;

import com.goldresto.entity.*;
import com.goldresto.repository.*;
import com.goldresto.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/pos")
@PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'OWNER')")
public class POSController {
    private static final Logger logger = LoggerFactory.getLogger(POSController.class);

    @Autowired
    private PanierRepository panierRepository;
    
    @Autowired
    private ProduitRepository produitRepository;
    
    @Autowired
    private LignedeProduitRepository lignedeProduitRepository;

    @Autowired
    private PaiementRepository paiementRepository;

    @Autowired
    private JournalRepository journalRepository;

    @Autowired
    private StockService stockService;

    @GetMapping
    public String posInterface(Model model) {
        model.addAttribute("produits", produitRepository.findAll());
        model.addAttribute("panier", new Panier());
        model.addAttribute("paniersEnCours", panierRepository.findPaniersWithLignesAndProduits(PanierState.EN_COURS));
        return "pos/index";
    }


    @GetMapping("/paniers")
    @Transactional(readOnly = true)
    public String paniers(Model model) {
        try {
            logger.debug("Fetching active paniers");
            logger.debug("Fetching active paniers with products");
            List<Panier> paniers = panierRepository.findPaniersWithLignesAndProduits(PanierState.EN_COURS);
            logger.debug("Found {} active paniers", paniers.size());
            
            // Log details for debugging
            for (Panier panier : paniers) {
                logger.debug("Panier {} has {} products", panier.getId(), panier.getLignesProduits().size());
                for (LignedeProduit ligne : panier.getLignesProduits()) {
                    logger.debug("  - Product: {}, Quantity: {}", 
                        ligne.getProduit().getNomProduit(), ligne.getQuantite());
                }
            }
            logger.debug("Found {} active paniers", paniers.size());
            
            // Initialize the collections
            for (Panier panier : paniers) {
                logger.debug("Initializing panier {} with {} products", 
                    panier.getId(), panier.getLignesProduits().size());
            }
            
            model.addAttribute("paniers", paniers);
            return "pos/paniers";
        } catch (Exception e) {
            logger.error("Error fetching paniers: ", e);
            model.addAttribute("error", "Une erreur est survenue lors du chargement des paniers.");
            return "pos/paniers";
        }
    }

    @PostMapping("/panier/create")
    @ResponseBody
    public ResponseEntity<?> createPanier(@RequestBody Panier panier) {
    panier.setState(PanierState.EN_COURS);
    panier.calculateTotal();
    Panier savedPanier = panierRepository.save(panier);
    return ResponseEntity.ok(savedPanier);
    }

    @PostMapping("/panier/{panierId}/addProduct")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> addProduct(@PathVariable Long panierId, 
                                      @RequestParam Long produitId,
                                      @RequestParam Integer quantite) {
        Panier panier = panierRepository.findByIdWithLignes(panierId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid panier ID"));

        Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid produit ID"));

        // Check stock availability
        if (produit.getStock() < quantite) {
            return ResponseEntity.badRequest().body("Stock insuffisant. Disponible: " + produit.getStock());
        }

        // Look for existing LignedeProduit with the same product
        Optional<LignedeProduit> existingLigne = panier.getLignesProduits().stream()
            .filter(l -> l.getProduit().getId().equals(produitId))
            .findFirst();

        if (existingLigne.isPresent()) {
            // Update existing line
            LignedeProduit ligne = existingLigne.get();
            int newQuantity = ligne.getQuantite() + quantite;
            
            // Check if total quantity exceeds stock
            if (produit.getStock() < newQuantity) {
                return ResponseEntity.badRequest().body("Stock insuffisant pour ajouter " + quantite + 
                    " produits supplémentaires. Disponible: " + produit.getStock());
            }
            
            ligne.setQuantite(newQuantity);
            ligne.setPrixUnitaire(produit.getPrix());
            ligne.calculateSousTotal();
            lignedeProduitRepository.save(ligne);
        } else {
            // Create new line
            LignedeProduit ligne = new LignedeProduit();
            ligne.setProduit(produit);
            ligne.setQuantite(quantite);
            ligne.setPrixUnitaire(produit.getPrix());
            ligne.setPanier(panier);
            ligne.calculateSousTotal();
            lignedeProduitRepository.save(ligne);
            panier.getLignesProduits().add(ligne);
        }

        // Decrease stock
        stockService.checkAndDecreaseStock(produit, quantite);

        panier.calculateTotal();
        panier = panierRepository.save(panier);
        return ResponseEntity.ok(panier);
    }

    // Place this method BEFORE getPanier(Long id)
    @GetMapping("/panier/checkNumeroTable")
    @ResponseBody
    public ResponseEntity<?> checkNumeroTable(@RequestParam Integer numeroTable) {
        boolean exists = panierRepository.existsByNumeroTableAndState(numeroTable, PanierState.EN_COURS);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/panier/{id}")
    @ResponseBody
    public ResponseEntity<?> getPanier(@PathVariable Long id) {
        return ResponseEntity.ok(panierRepository.findByIdWithLignes(id));
    }

    @PostMapping("/paiement/{panierId}")
    @ResponseBody
    public ResponseEntity<?> processPaiement(@PathVariable Long panierId, @RequestBody Paiement paiement) {
        Panier panier = panierRepository.findByIdWithLignes(panierId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid panier ID"));
        
        paiement.setPanier(panier);
        Paiement savedPaiement = paiementRepository.save(paiement);

        // Persist Journal after payment
        Journal journal = new Journal();
        journal.setPanierId(panier.getId()); // Set the original Panier ID
        journal.setNumeroTable(panier.getNumeroTable());
        journal.setTotal(panier.getTotal() != null ? panier.getTotal().doubleValue() : null);
        journal.setDatePaiement(java.time.LocalDateTime.now());
        journal.setMontantRecu(paiement.getCashRecu() != null ? paiement.getCashRecu().doubleValue() : null);
        journal.setMonnaieRendue(
            (paiement.getCashRecu() != null && panier.getTotal() != null)
                ? paiement.getCashRecu().subtract(panier.getTotal()).doubleValue()
                : null
        );
        if (panier.getLignesProduits() != null) {
            journal.setLignesProduits(
                panier.getLignesProduits().stream().map(ligne -> {
                    JournalLigne jl = new JournalLigne();
                    jl.setNomProduit(ligne.getProduit().getNomProduit());
                    jl.setQuantite(ligne.getQuantite());
                    jl.setPrixUnitaire(ligne.getPrixUnitaire() != null ? ligne.getPrixUnitaire().doubleValue() : null);
                    jl.setSousTotal(ligne.getSousTotal() != null ? ligne.getSousTotal().doubleValue() : null);
                    return jl;
                }).collect(java.util.stream.Collectors.toList())
            );
        }
        journalRepository.save(journal);

        return ResponseEntity.ok(savedPaiement);
    }

    // Endpoint to clear all products from a panier (for editing)
    @PostMapping("/panier/{panierId}/clearProducts")
    @ResponseBody
    public ResponseEntity<?> clearProducts(@PathVariable Long panierId) {
        Panier panier = panierRepository.findByIdWithLignes(panierId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid panier ID"));
        panier.getLignesProduits().clear();
        panier.calculateTotal();
        panierRepository.save(panier);
        // Optionally, also delete lines from LignedeProduitRepository if needed
        return ResponseEntity.ok().build();
    }
}

