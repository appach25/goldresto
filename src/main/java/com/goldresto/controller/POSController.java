package com.goldresto.controller;

import java.util.List;
import java.util.ArrayList;

import com.goldresto.entity.*;
import com.goldresto.repository.*;
import com.goldresto.service.StockService;
import com.goldresto.service.PanierService;
import com.goldresto.dto.PanierCreateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    @Autowired
    private PanierService panierService;

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
    @Transactional
    public ResponseEntity<?> createPanier(@RequestBody PanierCreateDTO dto) {
        try {
            logger.debug("Creating new panier with {} products", 
                dto.getProducts() != null ? dto.getProducts().size() : 0);
            Panier panier = new Panier();
            panier.setState(PanierState.EN_COURS);
            panier.setNumeroTable(dto.getNumeroTable());
            
            // Initialize lignesProduits list
            List<LignedeProduit> lignesProduits = new ArrayList<>();
            
            if (dto.getProducts() != null) {
                for (PanierCreateDTO.ProduitItemDTO item : dto.getProducts()) {
                    Produit produit = produitRepository.findById(item.getProduitId())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid product ID: " + item.getProduitId()));
                    
                    LignedeProduit ligne = new LignedeProduit();
                    ligne.setProduit(produit);
                    ligne.setQuantite(item.getQuantite());
                    ligne.setPrixUnitaire(produit.getPrix());
                    ligne.setPanier(panier);
                    ligne.calculateSousTotal();
                    
                    lignesProduits.add(ligne);
                    
                    // Update stock
                    stockService.checkAndDecreaseStock(produit, item.getQuantite());
                }
            }
            
            // Set the lignesProduits list to panier
            logger.debug("Setting {} lignesProduits to panier", lignesProduits.size());
            panier.setLignesProduits(lignesProduits);
            
            // Log the state before saving
            lignesProduits.forEach(ligne -> {
                logger.debug("Ligne before save - produit: {}, quantite: {}, prix: {}, sousTotal: {}",
                    ligne.getProduit().getId(), ligne.getQuantite(), 
                    ligne.getPrixUnitaire(), ligne.getSousTotal());
            });
            
            // Save panier with all its lignes
            logger.debug("Saving panier with total: {}", panier.getTotal());
            Panier savedPanier = panierRepository.save(panier);
            
            // Force recalculate total
            logger.debug("Recalculating total for saved panier");
            savedPanier.updateAllSousTotals();
            savedPanier.recalculateTotal();
            
            // Log the state after recalculation
            savedPanier.getLignesProduits().forEach(ligne -> {
                logger.debug("Ligne after recalc - produit: {}, quantite: {}, prix: {}, sousTotal: {}",
                    ligne.getProduit().getId(), ligne.getQuantite(), 
                    ligne.getPrixUnitaire(), ligne.getSousTotal());
            });
            
            logger.debug("Final total after recalc: {}", savedPanier.getTotal());
            savedPanier = panierRepository.save(savedPanier);
            
            // Return updated panier
            return ResponseEntity.ok(savedPanier);
            
        } catch (Exception e) {
            logger.error("Error creating panier: ", e);
            return ResponseEntity.badRequest().body("Error creating panier: " + e.getMessage());
        }
    }

    @PostMapping("/panier/{panierId}/addProduct")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> addProduct(
        @PathVariable Long panierId, 
        @RequestParam Long produitId,
        @RequestParam Integer quantite,
        @RequestParam(required = false) BigDecimal frontendTotal
    ) {
        Panier panier = panierRepository.findByIdWithLignes(panierId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid panier ID"));
        Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid product ID"));

        // Check if product has enough stock
        if (produit.getStock() < quantite) {
            return ResponseEntity.badRequest().body("Not enough stock");
        }

        // Find existing ligne or create new one
        LignedeProduit ligne = panier.getLignesProduits().stream()
            .filter(l -> l.getProduit().getId().equals(produitId))
            .findFirst()
            .orElse(null);

        if (ligne == null) {
            ligne = new LignedeProduit();
            ligne.setPanier(panier);
            ligne.setProduit(produit);
            ligne.setQuantite(quantite);
            panier.getLignesProduits().add(ligne);
        } else {
            // Increment quantity if line exists
            ligne.setQuantite(ligne.getQuantite() + quantite);
        }

        // Update price and recalculate sous-total
        ligne.setPrixUnitaire(produit.getPrix());
        ligne.calculateSousTotal();

        // Decrease stock
        stockService.checkAndDecreaseStock(produit, quantite);

        // Save using service to ensure proper total calculation
        panier = panierService.savePanier(panier);

        // Recalculate and persist total after modification
        panierService.recalculateTotal(panier.getId());

        // Fetch updated panier with recalculated total
        Panier updatedPanier = panierRepository.findByIdWithLignes(panier.getId())
            .orElseThrow(() -> new IllegalArgumentException("Invalid panier ID"));
        return ResponseEntity.ok(updatedPanier);
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
        Panier panier = panierRepository.findByIdWithLignes(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid panier ID"));
        return ResponseEntity.ok(panier);
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
        panierRepository.save(panier);

        // Recalculate and persist total after clearing
        panierService.recalculateTotal(panierId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/panier/{panierId}/recalculateTotal")
    @ResponseBody
    public ResponseEntity<?> recalculateTotal(@PathVariable Long panierId) {
        panierService.recalculateTotal(panierId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/panier/{panierId}/total")
    @ResponseBody
    public ResponseEntity<?> getPanierTotal(@PathVariable Long panierId) {
        // Always fetch the total from the Panier entity for consistency
        Panier panier = panierRepository.findByIdWithLignes(panierId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid panier ID"));
        BigDecimal total = panier.getTotal();
        return ResponseEntity.ok(total != null ? total : BigDecimal.ZERO);
    }

    @PostMapping("/panier/{panierId}/validate")
    @ResponseBody
    public ResponseEntity<?> validatePanier(
        @PathVariable Long panierId,
        @RequestParam(required = false) BigDecimal total // frontend total ignored
    ) {
        // Always recalculate total from DB, do not trust frontend
        panierService.recalculateTotal(panierId);
        return ResponseEntity.ok().build();
    }

}



