package com.goldresto.service;

import com.goldresto.entity.Panier;
import com.goldresto.entity.PanierState;
import com.goldresto.entity.LignedeProduit;
import com.goldresto.repository.PanierRepository;
import com.goldresto.repository.LignedeProduitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import com.goldresto.entity.User;
import com.goldresto.repository.UserRepository;
import java.math.BigDecimal;

@Service
public class PanierService {
    private static final Logger logger = LoggerFactory.getLogger(PanierService.class);

    @Autowired
    private PanierRepository panierRepository;

    @Autowired
    private LignedeProduitRepository lignedeProduitRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public boolean isTableInUse(Integer numeroTable) {
        return panierRepository.existsByNumeroTableAndState(numeroTable, PanierState.EN_COURS);
    }

    @Transactional
    public Panier savePanier(Panier panier) throws IllegalStateException {
        // Only check table if this is a new panier
        if (panier.getId() == null && isTableInUse(panier.getNumeroTable())) {
            throw new IllegalStateException("La table " + panier.getNumeroTable() + " est déjà occupée");
        }

        // Set the current user as the owner of the panier if not already set
        if (panier.getUser() == null) {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetails) {
                String username = ((UserDetails)principal).getUsername();
                User currentUser = userRepository.findByUsername(username).orElse(null);
                if (currentUser != null) {
                    panier.setUser(currentUser);
                } else {
                    logger.warn("Could not find user {} for panier", username);
                }
            }
        }

        // Save the panier
        return panierRepository.save(panier);
    }

    @Transactional
    public Panier savePanierWithTotal(Panier panier, BigDecimal frontendTotal) {
        // Set the current user as the owner of the panier
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            String username = ((UserDetails)principal).getUsername();
            User currentUser = userRepository.findByUsername(username).orElse(null);
            if (currentUser != null) {
                panier.setUser(currentUser);
            } else {
                logger.warn("Could not find user {} for panier", username);
            }
        }

        panier.setTotal(frontendTotal);
        return panierRepository.save(panier);
    }

    private void validatePanierTotal(Long panierId) {
        Object[] result = panierRepository.validatePanierTotal(panierId);
        if (result != null && result.length >= 3) {
            BigDecimal storedTotal = (BigDecimal) result[1];
            BigDecimal calculatedTotal = (BigDecimal) result[2];
            
            if (calculatedTotal != null && !calculatedTotal.equals(storedTotal)) {
                logger.error("Total mismatch for Panier {}: stored={}, calculated={}",
                    panierId, storedTotal, calculatedTotal);
                
                // Fix the total
                Panier panier = panierRepository.findByIdWithLignes(panierId).orElse(null);
                if (panier != null) {
                    panier.setTotal(calculatedTotal);
                    panierRepository.save(panier);
                    logger.info("Fixed total for Panier {}: new total={}", panierId, calculatedTotal);
                }
            }
        }
    }

    @Transactional
    public void recalculateTotal(Long panierId) {
        Panier panier = panierRepository.findByIdWithLignes(panierId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid panier ID"));
        
        // Calculate the sum of sousTotal for all LignedeProduit in this panier using stream
        BigDecimal streamTotal = panier.getLignesProduits().stream()
            .map(LignedeProduit::getSousTotal)
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get the sum from database query
        BigDecimal dbTotal = lignedeProduitRepository.sumSousTotalByPanierId(panierId);
        
        // Handle null dbTotal from database
        if (dbTotal == null) {
            dbTotal = BigDecimal.ZERO;
        }
        
        // Console debugging output
        System.out.println("=== PANIER TOTAL CALCULATION DEBUG ===");
        System.out.println("Panier ID: " + panierId);
        System.out.println("Stream calculated total: " + streamTotal);
        System.out.println("Database query total: " + dbTotal);
        System.out.println("Number of lignes in panier: " + (panier.getLignesProduits() != null ? panier.getLignesProduits().size() : 0));
        
        // Log individual line details for debugging
        if (panier.getLignesProduits() != null) {
            panier.getLignesProduits().forEach(ligne -> {
                System.out.println("  Ligne ID: " + ligne.getId() + 
                                   ", Produit ID: " + (ligne.getProduit() != null ? ligne.getProduit().getId() : "null") +
                                   ", Quantite: " + ligne.getQuantite() + 
                                   ", Prix unitaire: " + ligne.getPrixUnitaire() + 
                                   ", Sous-total: " + ligne.getSousTotal());
            });
        }
        
        // Use the database total as the source of truth (it's more reliable)
        BigDecimal finalTotal = dbTotal;
        System.out.println("Using final total: " + finalTotal);
        System.out.println("=======================================");
        
        // Logging for application logs
        logger.info("Panier {} - Stream total: {}, DB total: {}, Final total: {}", 
                    panierId, streamTotal, dbTotal, finalTotal);

        // Use the database calculated total to update the Panier
        panier.setTotal(finalTotal);
        panierRepository.save(panier);
    }
}