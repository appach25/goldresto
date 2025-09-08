package com.goldresto.repository;

import com.goldresto.entity.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import com.goldresto.dto.UserPaymentSummary;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {
    List<Paiement> findByNumeroTable(Integer numeroTable);
    List<Paiement> findByDateCreationAfter(LocalDateTime date);
    @Query("""
        SELECT DISTINCT p FROM Paiement p
        JOIN FETCH p.user u
        JOIN FETCH p.panier pan
        WHERE pan.state = 'PAYER'
        AND p.dateCreation BETWEEN :startDate AND :endDate
        ORDER BY u.fullName, p.dateCreation
    """)
    List<Paiement> findPaidPaymentsByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    @Query("""
        SELECT p FROM Paiement p
        WHERE p.user.id = :userId
        AND p.dateCreation BETWEEN :startDate AND :endDate
        ORDER BY p.dateCreation DESC
    """)
    List<Paiement> findByUserIdAndDateCreationBetween(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    List<Paiement> findByPanierUserIdAndDateCreationBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate);
}
