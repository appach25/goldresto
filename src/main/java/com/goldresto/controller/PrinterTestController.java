package com.goldresto.controller;

import com.goldresto.service.PrinterService;
import com.goldresto.entity.Panier;
import com.goldresto.entity.Produit;
import com.goldresto.entity.LignedeProduit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/printer")
@PreAuthorize("permitAll()")
public class PrinterTestController {

    @Autowired
    private PrinterService printerService;

    @GetMapping("/test")
    public ResponseEntity<String> testPrinter() {
        try {
            // Create a sample panier
            Panier panier = new Panier();
            panier.setNumeroTable(1);
            
            // Create some test products
            List<LignedeProduit> lignes = new ArrayList<>();
            
            Produit produit1 = new Produit();
            produit1.setNomProduit("Chicken Rice");
            produit1.setPrix(new BigDecimal("15.99"));
            
            Produit produit2 = new Produit();
            produit2.setNomProduit("Fish and Chips");
            produit2.setPrix(new BigDecimal("18.99"));
            
            Produit produit3 = new Produit();
            produit3.setNomProduit("Caesar Salad");
            produit3.setPrix(new BigDecimal("12.99"));
            
            // Create ligne de produits
            LignedeProduit ligne1 = new LignedeProduit();
            ligne1.setProduit(produit1);
            ligne1.setQuantite(2);
            ligne1.setPanier(panier);
            
            LignedeProduit ligne2 = new LignedeProduit();
            ligne2.setProduit(produit2);
            ligne2.setQuantite(1);
            ligne2.setPanier(panier);
            
            LignedeProduit ligne3 = new LignedeProduit();
            ligne3.setProduit(produit3);
            ligne3.setQuantite(3);
            ligne3.setPanier(panier);
            
            lignes.add(ligne1);
            lignes.add(ligne2);
            lignes.add(ligne3);
            
            panier.setLignesProduits(lignes);

            // Print the test order
            printerService.printKitchenReceipt(panier);

            return ResponseEntity.ok("Test print successful");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Printer error: " + e.getMessage());
        }
    }

}
