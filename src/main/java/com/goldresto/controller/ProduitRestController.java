package com.goldresto.controller;

import com.goldresto.entity.Produit;
import com.goldresto.repository.ProduitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/produits")
public class ProduitRestController {

    @Autowired
    private ProduitRepository produitRepository;

    @GetMapping("/{id}")
    public ResponseEntity<Produit> getProduit(@PathVariable Long id) {
        return produitRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
