package com.goldresto.controller;

import com.goldresto.repository.*;
import com.goldresto.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/reports")
@PreAuthorize("hasRole('OWNER')")
public class ReportController {

    @Autowired
    private PanierRepository panierRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private PaiementRepository paiementRepository;

    @Autowired
    private StockService stockService;

    @GetMapping
    public String showReports(Model model) {
        // Sales summary
        BigDecimal totalSales = paiementRepository.findAll().stream()
            .map(p -> p.getMontantAPayer())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long totalOrders = panierRepository.count();
        
        // Low stock products
        List<Map<String, Object>> lowStockProducts = produitRepository.findAll().stream()
            .filter(p -> p.getStock() <= 10)
            .map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("name", p.getNomProduit());
                map.put("stock", p.getStock());
                map.put("price", p.getPrix());
                return map;
            })
            .collect(Collectors.toList());

        // Today's sales
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        List<Map<String, Object>> todaySales = paiementRepository.findByDateCreationAfter(startOfDay)
            .stream()
            .map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("time", p.getDateCreation());
                map.put("amount", p.getMontantAPayer());
                map.put("method", p.getCashRecu() != null ? "CASH" : "CARD");
                return map;
            })
            .collect(Collectors.toList());

        model.addAttribute("totalSales", totalSales);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("lowStockProducts", lowStockProducts);
        model.addAttribute("todaySales", todaySales);
        
        return "reports/dashboard";
    }

    @GetMapping("/sales")
    public String showSalesReport(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Model model) {
        
        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();

        List<Map<String, Object>> salesData = paiementRepository
            .findByDateCreationBetween(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay())
            .stream()
            .map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("date", p.getDateCreation());
                map.put("amount", p.getMontantAPayer());
                map.put("method", p.getCashRecu() != null ? "CASH" : "CARD");
                return map;
            })
            .collect(Collectors.toList());

        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("salesData", salesData);
        
        return "reports/sales";
    }

    @GetMapping("/stock")
    public String showStockReport(Model model) {
        List<Map<String, Object>> stockData = produitRepository.findAll().stream()
            .map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put(
                "name", p.getNomProduit());
                map.put("stock", p.getStock());
                map.put("price", p.getPrix());
                map.put("value", BigDecimal.valueOf(p.getStock()).multiply(p.getPrix()));
                return map;
            })
            .collect(Collectors.toList());

        double totalStockValue = stockData.stream()
            .mapToDouble(m -> (Double) m.get("value"))
            .sum();

        model.addAttribute("stockData", stockData);
        model.addAttribute("totalStockValue", totalStockValue);
        
        return "reports/stock";
    }
}
