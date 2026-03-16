package com.goldresto.service;

import com.goldresto.entity.Ingredient;
import com.goldresto.entity.ProduitIngredient;
import com.goldresto.entity.StockHistory;
import com.goldresto.repository.IngredientRepository;
import com.goldresto.repository.ProduitIngredientRepository;
import com.goldresto.repository.StockHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class IngredientStockService {
    private static final Logger logger = LoggerFactory.getLogger(IngredientStockService.class);

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private ProduitIngredientRepository produitIngredientRepository;

    @Autowired
    private StockHistoryRepository stockHistoryRepository;

    @Transactional
    public void decreaseStockOnProductSale(Long produitId, int quantitySold) {
        logger.info("Decreasing stock for product {} quantity {}", produitId, quantitySold);
        List<ProduitIngredient> recipeLines = produitIngredientRepository.findByProduitId(produitId);
        logger.info("Found {} recipe lines for product {}", recipeLines.size(), produitId);
        for (ProduitIngredient pi : recipeLines) {
            Ingredient ingredient = pi.getIngredient();
            logger.info("Processing ingredient: {}, needed qty: {}", ingredient.getNom(), pi.getQuantite());
            BigDecimal qtyNeeded = pi.getQuantite().multiply(BigDecimal.valueOf(quantitySold));
            BigDecimal current = ingredient.getStockActuel() != null ? ingredient.getStockActuel() : BigDecimal.ZERO;

            if (current.compareTo(qtyNeeded) < 0) {
                logger.warn("Insufficient stock for ingredient {}: need {}, have {}", ingredient.getNom(), qtyNeeded, current);
                continue;
            }

            BigDecimal newStock = current.subtract(qtyNeeded);
            ingredient.setStockActuel(newStock);
            ingredientRepository.save(ingredient);
            logger.info("Updated stock for ingredient {}: {} -> {} {}", ingredient.getNom(), current, newStock, ingredient.getUniteStock());

            // TODO: Re-enable after DB migration to allow nullable produit_id in stock_history
            /*
            StockHistory history = new StockHistory();
            history.setIngredient(ingredient);
            history.setQuantityChanged(qtyNeeded.negate());
            history.setStockAfterChange(newStock);
            history.setType("SALE");
            history.setReason("Sale of product: " + pi.getProduit().getNomProduit() + " (x" + quantitySold + ")");
            stockHistoryRepository.save(history);
            */

            logger.debug("Decreased stock for ingredient {}: {} {} -> {} {}", 
                ingredient.getNom(), qtyNeeded, ingredient.getUniteStock(), current, ingredient.getUniteStock(), newStock, ingredient.getUniteStock());
        }
    }

    @Transactional
    public void increaseStockOnAchat(Long ingredientId, BigDecimal quantityAdded) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid ingredient Id: " + ingredientId));
        BigDecimal current = ingredient.getStockActuel() != null ? ingredient.getStockActuel() : BigDecimal.ZERO;
        BigDecimal newStock = current.add(quantityAdded);

        ingredient.setStockActuel(newStock);
        ingredientRepository.save(ingredient);

        // TODO: Re-enable after DB migration to allow nullable produit_id in stock_history
        /*
        StockHistory history = new StockHistory();
        history.setIngredient(ingredient);
        history.setQuantityChanged(quantityAdded);
        history.setStockAfterChange(newStock);
        history.setType("ACHAT");
        history.setReason("Purchase received");
        stockHistoryRepository.save(history);
        */

        logger.debug("Increased stock for ingredient {}: {} {} -> {} {}", 
            ingredient.getNom(), quantityAdded, ingredient.getUniteStock(), current, ingredient.getUniteStock(), newStock, ingredient.getUniteStock());
    }
}
