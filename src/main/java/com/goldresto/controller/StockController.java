package com.goldresto.controller;

import com.goldresto.entity.Produit;
import com.goldresto.entity.StockHistory;
import com.goldresto.repository.ProduitRepository;
import com.goldresto.repository.StockHistoryRepository;
import com.goldresto.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/stock")
@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
public class StockController {
    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private StockHistoryRepository stockHistoryRepository;

    @Autowired
    private StockService stockService;

    @GetMapping
    public String stockManagement(Model model) {
        List<Produit> lowStockProducts = produitRepository.findAll().stream()
            .filter(p -> stockService.isLowStock(p))
            .collect(Collectors.toList());

        model.addAttribute("produits", produitRepository.findAll());
        model.addAttribute("lowStockProducts", lowStockProducts);
        return "stock/index";
    }

    @GetMapping("/history/{produitId}")
    @ResponseBody
    public ResponseEntity<?> getStockHistory(@PathVariable Long produitId) {
        return ResponseEntity.ok(stockHistoryRepository.findByProduitIdOrderByTimestampDesc(produitId));
    }

    @PostMapping("/replenish")
    @ResponseBody
    public ResponseEntity<?> replenishStock(
            @RequestParam Long produitId,
            @RequestParam Integer quantity,
            @RequestParam String reason) {
        
        Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid produit ID"));

        stockService.replenishStock(produit, quantity, reason);
        return ResponseEntity.ok(produit);
    }

    @GetMapping("/alerts")
    @ResponseBody
    public ResponseEntity<?> getLowStockAlerts() {
        List<Produit> lowStockProducts = produitRepository.findAll().stream()
            .filter(p -> stockService.isLowStock(p))
            .collect(Collectors.toList());
        return ResponseEntity.ok(lowStockProducts);
    }
}
