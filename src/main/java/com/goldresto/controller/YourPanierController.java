package com.goldresto.controller;

import com.goldresto.entity.Panier;
import com.goldresto.entity.Produit;
import com.goldresto.entity.LignedeProduit;
import com.goldresto.repository.PanierRepository;
import com.goldresto.repository.ProduitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class YourPanierController {

    @Autowired
    private PanierRepository panierRepository;

    @Autowired
    private ProduitRepository produitRepository;

    // Example method for adding a product to a panier
    @PostMapping("/pos/panier/{panierId}/addProduct")
    public ResponseEntity<?> addProductToPanier(@PathVariable Long panierId, @RequestParam Long produitId, @RequestParam int quantite) {
        Panier panier = panierRepository.findById(panierId).orElseThrow();
        Produit produit = produitRepository.findById(produitId).orElseThrow();

        LignedeProduit ligne = new LignedeProduit();
        ligne.setProduit(produit);
        ligne.setQuantite(quantite);
        // ...set other fields as needed...

        panier.addLigneProduit(ligne);
        panier.recalculateTotal(); // Ensure total is up-to-date
        panierRepository.save(panier); // Persist the updated total

        return ResponseEntity.ok(panier);
    }

    // ...existing code...
}