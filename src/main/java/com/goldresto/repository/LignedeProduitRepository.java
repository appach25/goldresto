package com.goldresto.repository;

import com.goldresto.entity.LignedeProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LignedeProduitRepository extends JpaRepository<LignedeProduit, Long> {
    @Query("SELECT SUM(l.sousTotal) FROM LignedeProduit l WHERE l.panier.id = :panierId")
    BigDecimal sumSousTotalByPanierId(@Param("panierId") Long panierId);

    @Query("SELECT l FROM LignedeProduit l WHERE l.panier.date BETWEEN :startDate AND :endDate")
    List<LignedeProduit> findByPanierDateBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
