package com.goldresto.controller;

import com.goldresto.entity.Produit;
import com.goldresto.entity.StockHistory;
import com.goldresto.repository.ProduitRepository;
import com.goldresto.repository.StockHistoryRepository;
import com.goldresto.service.StockService;
import com.goldresto.dto.StockReplenishRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/stock")
@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
@CrossOrigin
public class StockController {
    private static final Logger logger = LoggerFactory.getLogger(StockController.class);
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
    @Transactional
    public String replenishStock(
            @RequestParam Long produitId,
            @RequestParam Integer quantity,
            @RequestParam String reason,
            RedirectAttributes redirectAttributes) {
        try {
            logger.info("Replenishing stock for product ID: {}, quantity: {}, reason: {}", 
                produitId, quantity, reason);
            
            Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé: " + produitId));

            stockService.replenishStock(produit, quantity, reason);
            produit = produitRepository.save(produit);
            
            logger.info("Successfully replenished stock. New stock level: {}", produit.getStock());
            
            redirectAttributes.addFlashAttribute("success", true);
            return "redirect:/stock";
        } catch (Exception e) {
            logger.error("Error replenishing stock: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/stock";
        }
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
