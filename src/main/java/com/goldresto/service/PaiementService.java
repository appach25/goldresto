package com.goldresto.service;

import com.goldresto.entity.Paiement;
import com.goldresto.entity.User;
import com.goldresto.repository.PaiementRepository;
import com.goldresto.dto.UserPaymentSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.goldresto.dto.UserPaymentGroup;
import java.math.BigDecimal;

@Service
public class PaiementService {
    private static final Logger logger = LoggerFactory.getLogger(PaiementService.class);

    @Autowired
    private PaiementRepository paiementRepository;

    private List<Paiement> getDetailedPayments(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return paiementRepository.findByUserIdAndDateCreationBetween(userId, startDate, endDate);
    }

    @Transactional
    public List<Paiement> getUserPayments(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) {
            startDate = LocalDateTime.now().minusMonths(1);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        return paiementRepository.findByUserIdAndDateCreationBetween(userId, startDate, endDate);
    }

    public List<UserPaymentGroup> getUserPaymentsGroup(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) {
            startDate = LocalDateTime.now().minusMonths(1);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        List<Paiement> payments = paiementRepository.findPaidPaymentsByDateRange(startDate, endDate);
        return payments.stream()
            .collect(Collectors.groupingBy(Paiement::getUser))
            .entrySet().stream()
            .map(entry -> new UserPaymentGroup(entry.getKey(), entry.getValue()))
            .sorted((g1, g2) -> g2.getTotal().compareTo(g1.getTotal()))
            .collect(Collectors.toList());
    }

    @Transactional
    public Paiement save(Paiement paiement) {
        if (paiement.getPanier() != null && paiement.getPanier().getUser() != null) {
            paiement.setUser(paiement.getPanier().getUser());
            logger.debug("Setting user {} for payment {}", paiement.getUser().getFullName(), paiement.getId());
        } else {
            logger.warn("No user found for payment {}", paiement.getId());
        }
        return paiementRepository.save(paiement);
    }

    public Map<String, Object> getUserPaymentsReport(LocalDateTime startDate, LocalDateTime endDate, Long userId) {
        logger.debug("Generating user payments report from {} to {}", startDate, endDate);
        Map<String, Object> report = new HashMap<>();
        
        try {
            final LocalDateTime finalStartDate = startDate != null ? startDate : LocalDateTime.now().minusMonths(1);
            final LocalDateTime finalEndDate = endDate != null ? endDate : LocalDateTime.now();
            logger.debug("Using dates: {} to {}", finalStartDate, finalEndDate);

            List<Paiement> payments;
            if (userId != null) {
                payments = paiementRepository.findByUserIdAndDateCreationBetween(userId, finalStartDate, finalEndDate);
            } else {
                payments = paiementRepository.findPaidPaymentsByDateRange(finalStartDate, finalEndDate);
            }
            logger.debug("Found {} payments", payments.size());
            
            if (payments.isEmpty()) {
                logger.info("No payments found");
                report.put("startDate", startDate);
                report.put("endDate", endDate);
                report.put("userPayments", new ArrayList<>());
                return report;
            }

            Map<User, List<Paiement>> paymentsByUser = payments.stream()
                .collect(Collectors.groupingBy(Paiement::getUser));

            // Calculate total and max amounts
            BigDecimal grandTotal = payments.stream()
                .map(p -> p.getMontantAPayer() != null ? p.getMontantAPayer() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal maxAmount = paymentsByUser.values().stream()
                .map(userPayments -> userPayments.stream()
                    .map(p -> p.getMontantAPayer() != null ? p.getMontantAPayer() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add))
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

            List<Map<String, Object>> userPayments = paymentsByUser.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> userPayment = new HashMap<>();
                    User user = entry.getKey();
                    List<Paiement> userPaiements = entry.getValue();

                    BigDecimal totalAmount = userPaiements.stream()
                        .map(p -> p.getMontantAPayer() != null ? p.getMontantAPayer() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // Calculate percentages
                    double percentageOfTotal = grandTotal.compareTo(BigDecimal.ZERO) > 0
                        ? totalAmount.multiply(BigDecimal.valueOf(100)).divide(grandTotal, 2, java.math.RoundingMode.HALF_UP).doubleValue()
                        : 0.0;

                    double percentageOfMax = maxAmount.compareTo(BigDecimal.ZERO) > 0
                        ? totalAmount.multiply(BigDecimal.valueOf(100)).divide(maxAmount, 2, java.math.RoundingMode.HALF_UP).doubleValue()
                        : 0.0;

                    userPayment.put("user", user);
                    userPayment.put("totalAmount", totalAmount);
                    userPayment.put("paymentCount", userPaiements.size());
                    userPayment.put("payments", userPaiements);
                    userPayment.put("percentageOfTotal", percentageOfTotal);
                    userPayment.put("percentageOfMax", percentageOfMax);
                    return userPayment;
                })
                .sorted((m1, m2) -> ((BigDecimal)m2.get("totalAmount")).compareTo((BigDecimal)m1.get("totalAmount")))
                .collect(Collectors.toList());

            report.put("startDate", finalStartDate);
            report.put("endDate", finalEndDate);
            report.put("userPayments", userPayments);
            return report;
        } catch (Exception e) {
            logger.error("Error generating user payments report: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la génération du rapport: " + e.getMessage(), e);
        }
    }
}
