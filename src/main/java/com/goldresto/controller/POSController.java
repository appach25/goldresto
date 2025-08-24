package com.goldresto.controller;

import java.util.List;

import com.goldresto.entity.*;
import com.goldresto.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/pos")
public class POSController {

    @Autowired
    private PanierRepository panierRepository;
    
    @Autowired
    private ProduitRepository produitRepository;
    
    @Autowired
    private LignedeProduitRepository lignedeProduitRepository;

    @Autowired
    private PaiementRepository paiementRepository;

    @GetMapping
    public String posInterface(Model model) {
        model.addAttribute("produits", produitRepository.findAll());
        model.addAttribute("panier", new Panier());
        model.addAttribute("paniersEnCours", panierRepository.findPaniersWithLignesAndProduits(PanierState.EN_COURS));
        return "pos/index";
    }


    @PostMapping("/panier/create")
    @ResponseBody
    public ResponseEntity<?> createPanier(@RequestBody Panier panier) {
    panier.setState(PanierState.EN_COURS);
    panier.calculateTotal();
    Panier savedPanier = panierRepository.save(panier);
    return ResponseEntity.ok(savedPanier);
    }

    @PostMapping("/panier/{panierId}/addProduct")
    @ResponseBody
    public ResponseEntity<?> addProduct(@PathVariable Long panierId, 
                                      @RequestParam Long produitId,
                                      @RequestParam Integer quantite) {
    Panier panier = panierRepository.findByIdWithLignes(panierId)
        .orElseThrow(() -> new IllegalArgumentException("Invalid panier ID"));

    Produit produit = produitRepository.findById(produitId)
        .orElseThrow(() -> new IllegalArgumentException("Invalid produit ID"));

    LignedeProduit ligne = new LignedeProduit();
    ligne.setProduit(produit);
    ligne.setQuantite(quantite);
    ligne.setPrixUnitaire(produit.getPrix());
    ligne.setPanier(panier);
    ligne = lignedeProduitRepository.save(ligne);
    // Fetch all LignedeProduit for this panier from the DB
    List<LignedeProduit> allLignes = new java.util.ArrayList<>(
        lignedeProduitRepository.findAll()
            .stream()
            .filter(l -> l.getPanier() != null && l.getPanier().getId().equals(panierId))
            .toList()
    );
    panier.getLignesProduits().clear();
    panier.getLignesProduits().addAll(allLignes);
    panier.updateAllSousTotals();
    panier.calculateTotal();
    panier = panierRepository.save(panier);
    return ResponseEntity.ok(panier);
    }

    @GetMapping("/panier/{id}")
    @ResponseBody
    public ResponseEntity<?> getPanier(@PathVariable Long id) {
        return ResponseEntity.ok(panierRepository.findByIdWithLignes(id));
    }

    @PostMapping("/paiement/{panierId}")
    @ResponseBody
    public ResponseEntity<?> processPaiement(@PathVariable Long panierId, @RequestBody Paiement paiement) {
        Panier panier = panierRepository.findByIdWithLignes(panierId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid panier ID"));
        
        paiement.setPanier(panier);
        return ResponseEntity.ok(paiementRepository.save(paiement));
    }

    @GetMapping("/panier/check-table/{numeroTable}")
    @ResponseBody
    public ResponseEntity<?> checkNumeroTable(@PathVariable Integer numeroTable) {
        boolean exists = panierRepository.existsByNumeroTableAndState(numeroTable, PanierState.EN_COURS);
        return ResponseEntity.ok(exists);
    }
}
