package com.goldresto.service;

import com.goldresto.entity.Produit;
import com.goldresto.entity.StockHistory;
import com.goldresto.repository.ProduitRepository;
import com.goldresto.repository.StockHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {
    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private StockHistoryRepository stockHistoryRepository;

    @Transactional
    public boolean checkAndDecreaseStock(Produit produit, int quantity) {
        if (produit.getStock() == null || produit.getStock() < quantity) {
            return false;
        }

        produit.setStock(produit.getStock() - (long)quantity);
        produitRepository.save(produit);

        StockHistory history = new StockHistory();
        history.setProduit(produit);
        history.setQuantityChanged(-quantity);
        history.setStockAfterChange(produit.getStock().intValue());
        history.setType("SALE");
        history.setReason("Sale through POS");
        stockHistoryRepository.save(history);

        return true;
    }

    @Transactional
    public void replenishStock(Produit produit, int quantity, String reason) {
        Long currentStock = produit.getStock() != null ? produit.getStock() : 0L;
        produit.setStock(currentStock + (long)quantity);
        produitRepository.save(produit);

        StockHistory history = new StockHistory();
        history.setProduit(produit);
        history.setQuantityChanged(quantity);
        history.setStockAfterChange(produit.getStock().intValue());
        history.setType("RESTOCK");
        history.setReason(reason);
        stockHistoryRepository.save(history);
    }

    public boolean isLowStock(Produit produit) {
        return produit.getStock() <= 10; // Threshold can be configurable
    }
}
