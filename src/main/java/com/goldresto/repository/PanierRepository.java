package com.goldresto.repository;

import com.goldresto.entity.Panier;
import com.goldresto.entity.PanierState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PanierRepository extends JpaRepository<Panier, Long> {
    List<Panier> findByState(PanierState state);
    
    @Query("SELECT DISTINCT p FROM Panier p LEFT JOIN FETCH p.lignesProduits l LEFT JOIN FETCH l.produit WHERE p.state = :state")
    List<Panier> findPaniersWithLignesAndProduits(PanierState state);
    List<Panier> findByNumeroTable(Integer numeroTable);

    @Query("SELECT p FROM Panier p LEFT JOIN FETCH p.lignesProduits l LEFT JOIN FETCH l.produit WHERE p.id = :id")
    Optional<Panier> findByIdWithLignes(Long id);

    boolean existsByNumeroTableAndState(Integer numeroTable, PanierState state);
}
