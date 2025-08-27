package com.goldresto.repository;

import com.goldresto.entity.StockHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {
    List<StockHistory> findByProduitIdOrderByTimestampDesc(Long produitId);
    
    @Query("SELECT sh FROM StockHistory sh WHERE sh.produit.id = :produitId AND sh.timestamp >= CURRENT_DATE")
    List<StockHistory> findTodayChangesByProduitId(@Param("produitId") Long produitId);
}
