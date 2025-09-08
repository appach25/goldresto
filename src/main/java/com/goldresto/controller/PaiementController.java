package com.goldresto.controller;

import com.goldresto.service.PaiementService;
import com.goldresto.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import com.goldresto.entity.Paiement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

@Controller
@RequestMapping("/payments")
@PreAuthorize("hasAuthority('ROLE_OWNER')")
public class PaiementController {
    private static final Logger logger = LoggerFactory.getLogger(PaiementController.class);

    @Autowired
    private PaiementService paiementService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Paiement>> getUserPayments(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            List<Paiement> payments = paiementService.getUserPayments(userId, startDate, endDate);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            logger.error("Error getting user payments: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<Paiement> savePaiement(@RequestBody Paiement paiement) {
        try {
            Paiement savedPaiement = paiementService.save(paiement);
            return ResponseEntity.ok(savedPaiement);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Autowired
    private UserService userService;

    @GetMapping("/by-user")
    public String showUserPayments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Long userId,
            Model model) {
        try {
            logger.debug("Requesting payment report for period {} to {}", startDate, endDate);
            Map<String, Object> report = paiementService.getUserPaymentsReport(startDate, endDate, userId);
            model.addAttribute("users", userService.getAllUsers());
            model.addAttribute("selectedUserId", userId);
            if (report == null) {
                logger.error("Payment report is null");
                model.addAttribute("error", "Le rapport de paiements est vide");
                return "error/500";
            }
            logger.debug("Got payment report with {} user payments", 
                report.get("userPayments") != null ? ((List<?>)report.get("userPayments")).size() : 0);
            model.addAttribute("report", report);
            return "payments/by-user";
        } catch (Exception e) {
            logger.error("Error generating payment report: {}", e.getMessage(), e);
            model.addAttribute("error", "Une erreur est survenue lors du chargement des paiements: " + e.getMessage());
            return "error/500";
        }
    }
}
