package com.goldresto.repository;

import com.goldresto.entity.ProduitIngredient;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProduitIngredientRepository extends JpaRepository<ProduitIngredient, Long> {
    List<ProduitIngredient> findByProduitId(Long produitId);
}
