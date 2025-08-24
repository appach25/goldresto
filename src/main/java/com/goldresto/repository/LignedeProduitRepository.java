package com.goldresto.repository;

import com.goldresto.entity.LignedeProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LignedeProduitRepository extends JpaRepository<LignedeProduit, Long> {
}
