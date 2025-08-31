package com.goldresto.service;

import com.goldresto.entity.Panier;
import com.goldresto.entity.LignedeProduit;
import com.goldresto.repository.PanierRepository;
import com.goldresto.repository.LignedeProduitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class PanierService {
    private static final Logger logger = LoggerFactory.getLogger(PanierService.class);

    @Autowired
    private PanierRepository panierRepository;

    @Autowired
    private LignedeProduitRepository lignedeProduitRepository;

    @Transactional
    public Panier savePanier(Panier panier) {
        // First save to ensure all IDs are assigned
        Panier savedPanier = panierRepository.save(panier);
        return savedPanier;
    }

    @Transactional
    public Panier savePanierWithTotal(Panier panier, BigDecimal frontendTotal) {
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
        // Calculate the sum of sousTotal for all LignedeProduit in this panier
        BigDecimal total = panier.getLignesProduits().stream()
            .map(LignedeProduit::getSousTotal)
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Log the sum returned by the SQL query for comparison
        BigDecimal dbTotal = lignedeProduitRepository.sumSousTotalByPanierId(panierId);
        logger.debug("Panier {} - Calculated total: {}, DB total: {}", panierId, total, dbTotal);
        logger.info("Panier {} - DB total: {}", panierId, dbTotal); // This will always show in console if log level is INFO or lower
        System.out.println("Panier " + panierId + " - DB total: " + dbTotal);

        // Use this calculated total to update the Panier
        panier.setTotal(total);
        panierRepository.save(panier);
    }
}