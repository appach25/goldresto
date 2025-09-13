package com.goldresto.service;

import com.goldresto.entity.Produit;
import com.goldresto.entity.Panier;
import com.goldresto.entity.LignedeProduit;
import com.goldresto.repository.PanierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CommandeService {
    @Autowired
    private PrinterService printService;

    @Autowired
    private PanierRepository panierRepository;

    public void addProduitToCommande(Long panierId, Produit produit, int quantite) {
        Panier panier = panierRepository.findById(panierId)
            .orElseThrow(() -> new IllegalArgumentException("Panier not found: " + panierId));

        LignedeProduit ligne = new LignedeProduit();
        ligne.setProduit(produit);
        ligne.setQuantite(quantite);
        ligne.setPanier(panier);

        // Print the newly added product
        printService.printAddedProduct(panier, ligne);
    }
}
