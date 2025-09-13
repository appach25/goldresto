package com.goldresto.controller;

import java.util.List;
import java.util.ArrayList;

import com.goldresto.entity.*;
import com.goldresto.repository.*;
import com.goldresto.service.StockService;
import com.goldresto.service.PanierService;
import com.goldresto.service.PrinterService;
import com.goldresto.dto.PanierCreateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Controller
@RequestMapping("/pos")
@CrossOrigin(origins = "*")
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

    @Autowired
    private PrinterService printService;

    @GetMapping
    public String posInterface(Model model) {
        try {
            List<Produit> produits = produitRepository.findAll();
            List<Panier> paniersEnCours = panierRepository.findPaniersWithLignesAndProduits(PanierState.EN_COURS);
            
            model.addAttribute("produits", produits);
            model.addAttribute("panier", new Panier());
            model.addAttribute("paniersEnCours", paniersEnCours);
            model.addAttribute("csrfToken", "dummy"); // Add dummy CSRF token for now
            
            return "pos/index";
        } catch (Exception e) {
            logger.error("Error loading POS interface: ", e);
            model.addAttribute("error", "Une erreur est survenue lors du chargement de l'interface POS.");
            return "error/500";
        }
    }


    @GetMapping("/paniers")
    @Transactional(readOnly = true)
    public String paniers(Model model) {
        try {
            // Fetch active paniers
            List<Panier> paniers = panierRepository.findPaniersWithLignesAndProduits(PanierState.EN_COURS);
            model.addAttribute("paniers", paniers);
            
            // Fetch all products for the add product modal
            List<Produit> produits = produitRepository.findAll();
            model.addAttribute("produits", produits);
            
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
            // Check if table is already in use
            if (panierRepository.existsByNumeroTableAndState(dto.getNumeroTable(), PanierState.EN_COURS)) {
                return ResponseEntity.badRequest().body("La table " + dto.getNumeroTable() + " est déjà occupée");
            }

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
                    
                    // Decrease stock
                    stockService.checkAndDecreaseStock(produit, item.getQuantite());
                    
                    logger.debug("Ligne before save - produit: {}, quantite: {}, prix: {}, sousTotal: {}",
                        ligne.getProduit().getId(), ligne.getQuantite(), 
                        ligne.getPrixUnitaire(), ligne.getSousTotal());
                }
            }
            
            // Set lignesProduits to panier
            panier.setLignesProduits(lignesProduits);

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
            
            // Print kitchen receipt
            printService.printKitchenReceipt(savedPanier);
            
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
        try {
            logger.info("Adding product {} (quantity: {}) to panier {}", produitId, quantite, panierId);
            
            Panier panier = panierRepository.findByIdWithLignes(panierId)
                .orElseThrow(() -> new IllegalArgumentException("Panier non trouvé: " + panierId));

            if (!panier.getState().equals(PanierState.EN_COURS)) {
                throw new IllegalStateException("Ce panier n'est plus modifiable");
            }

            Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé: " + produitId));

            // Check if product has enough stock
            if (produit.getStock() < quantite) {
                throw new IllegalStateException("Stock insuffisant pour " + produit.getNomProduit());
            }

            // Find existing ligne or create new one
            LignedeProduit ligne = panier.getLignesProduits().stream()
                .filter(l -> l.getProduit().getId().equals(produitId))
                .findFirst()
                .orElse(null);

            boolean isNewProduct = ligne == null;
            if (isNewProduct) {
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

            // Print only the new/updated product
            List<LignedeProduit> productsToPrint = new ArrayList<>();
            productsToPrint.add(ligne);
            printService.printKitchenReceipt(panier, productsToPrint);

            // Fetch updated panier with recalculated total
            Panier updatedPanier = panierRepository.findByIdWithLignes(panier.getId())
                .orElseThrow(() -> new IllegalArgumentException("Panier non trouvé"));
            
            logger.info("Successfully added product {} to panier {}", produitId, panierId);
            return ResponseEntity.ok(updatedPanier);
        } catch (Exception e) {
            logger.error("Error adding product to panier {}: {}", panierId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Place this method BEFORE getPanier(Long id)
    @GetMapping("/panier/occupiedTables")
    @ResponseBody
    public ResponseEntity<?> getOccupiedTables() {
        List<Integer> occupiedTables = panierRepository.findByState(PanierState.EN_COURS)
            .stream()
            .map(Panier::getNumeroTable)
            .collect(Collectors.toList());
        return ResponseEntity.ok(occupiedTables);
    }

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
    @Transactional
    public ResponseEntity<?> processPaiement(@PathVariable Long panierId, @RequestBody Paiement paiement) {
        try {
            logger.info("Processing payment for panier {}", panierId);
            
            Panier panier = panierRepository.findByIdWithLignes(panierId)
                    .orElseThrow(() -> new IllegalArgumentException("Panier non trouvé: " + panierId));
            
            if (!panier.getState().equals(PanierState.EN_COURS)) {
                throw new IllegalStateException("Ce panier n'est plus modifiable");
            }
            
            // Save the panier with current user before linking to payment
            panier = panierService.savePanier(panier);
            
            // Validate payment amount
            if (paiement.getCashRecu() == null || paiement.getCashRecu().compareTo(panier.getTotal()) < 0) {
                throw new IllegalArgumentException("Le montant reçu doit être supérieur ou égal au montant à payer");
            }

            // Set panier and save payment
            paiement.setPanier(panier);
            Paiement savedPaiement = paiementRepository.save(paiement);

            // Create journal entry
            Journal journal = new Journal();
            journal.setPanierId(panier.getId());
            journal.setNumeroTable(panier.getNumeroTable());
            journal.setTotal(savedPaiement.getMontantAPayer() != null ? savedPaiement.getMontantAPayer().doubleValue() : null);
            journal.setDatePaiement(java.time.LocalDateTime.now());
            journal.setMontantRecu(savedPaiement.getCashRecu() != null ? savedPaiement.getCashRecu().doubleValue() : null);
            journal.setMonnaieRendue(savedPaiement.getMonnaie() != null ? savedPaiement.getMonnaie().doubleValue() : null);

            // Add journal lines
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

            // Print client bill
            printService.printClientBill(panier, savedPaiement.getCashRecu(), savedPaiement.getMonnaie());

            // Update panier state to PAYER and save
            panier.setState(PanierState.PAYER);
            panierRepository.save(panier);

            logger.info("Successfully processed payment for panier {}", panierId);
            return ResponseEntity.ok(savedPaiement);
        } catch (Exception e) {
            logger.error("Error processing payment: ", e);
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
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
        try {
            logger.info("Validating panier {}", panierId);
            
            // Always recalculate total from DB, do not trust frontend
            panierService.recalculateTotal(panierId);
            
            // Get panier with products
            Panier panier = panierRepository.findByIdWithLignes(panierId)
                .orElseThrow(() -> new IllegalArgumentException("Panier non trouvé: " + panierId));

            if (panier.getLignesProduits() == null || panier.getLignesProduits().isEmpty()) {
                throw new IllegalStateException("Le panier est vide");
            }
            
            // Print kitchen receipt
            logger.info("Printing kitchen receipt for panier {} with {} products", 
                panierId, panier.getLignesProduits().size());
            printService.printKitchenReceipt(panier);
            
            logger.info("Successfully validated panier {}", panierId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error validating panier {}: {}", panierId, e.getMessage(), e);
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }

    @PostMapping("/panier/{panierId}/print")
    @ResponseBody
    public ResponseEntity<?> printKitchenReceipt(@PathVariable Long panierId) {
        try {
            Panier panier = panierRepository.findByIdWithLignes(panierId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid panier ID"));
            printService.printKitchenReceipt(panier);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error printing receipt: " + e.getMessage());
        }
    }

}



