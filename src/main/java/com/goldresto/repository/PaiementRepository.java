package com.goldresto.repository;

import com.goldresto.entity.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {
    List<Paiement> findByNumeroTable(Integer numeroTable);
    List<Paiement> findByDateCreationAfter(LocalDateTime date);
    List<Paiement> findByDateCreationBetween(LocalDateTime startDate, LocalDateTime endDate);
}
