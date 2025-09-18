package com.goldresto.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.*;
import java.math.BigDecimal;

import org.hibernate.annotations.Formula;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
public class LignedeProduit {
    private static final Logger logger = LoggerFactory.getLogger(LignedeProduit.class);
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "produit_id")
    private Produit produit;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "panier_id")
    @JsonBackReference
    private Panier panier;

    private Integer quantite;
    private BigDecimal prixUnitaire;
    private BigDecimal sousTotal = BigDecimal.ZERO;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public Panier getPanier() {
        return panier;
    }

    public void setPanier(Panier panier) {
        this.panier = panier;
    }

    public Integer getQuantite() {
        return quantite;
    }

    public void setQuantite(Integer quantite) {
        this.quantite = quantite;
        calculateSousTotal();
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(BigDecimal prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
        calculateSousTotal();
    }

    public BigDecimal getSousTotal() {
        return sousTotal;
    }

    public void setSousTotal(BigDecimal sousTotal) {
        this.sousTotal = sousTotal;
    }

    public void calculateSousTotal() {
        logger.debug("Calculating sousTotal for LignedeProduit {} with quantite={} and prixUnitaire={}",
            id, quantite, prixUnitaire);

        // Validate and initialize values if needed
        if (quantite == null) {
            quantite = 0;
            logger.warn("Null quantite found for LignedeProduit {}, setting to 0", id);
        }

        // Use the set prixUnitaire, don't override it
        if (prixUnitaire == null) {
            if (produit != null && produit.getPrix() != null) {
                prixUnitaire = produit.getPrix();
                logger.debug("Using product price {} for LignedeProduit {}", prixUnitaire, id);
            } else {
                prixUnitaire = BigDecimal.ZERO;
                logger.warn("No price available for LignedeProduit {}, setting to 0", id);
            }
        }

        // Calculate sous-total with optional promotion: X items for Y price
        try {
            BigDecimal total = BigDecimal.ZERO;

            Integer promoQty = (produit != null) ? produit.getPromoQty() : null;
            BigDecimal promoPrice = (produit != null) ? produit.getPromoPrice() : null;

            boolean promoApplicable = promoQty != null && promoQty > 0 && promoPrice != null
                && quantite != null && quantite > 0
                && promoPrice.compareTo(BigDecimal.ZERO) >= 0
                && prixUnitaire.compareTo(BigDecimal.ZERO) >= 0;

            if (promoApplicable) {
                int bundles = quantite / promoQty;
                int remainder = quantite % promoQty;
                BigDecimal bundlesTotal = promoPrice.multiply(BigDecimal.valueOf(bundles));
                BigDecimal remainderTotal = prixUnitaire.multiply(BigDecimal.valueOf(remainder));
                total = bundlesTotal.add(remainderTotal);
            } else {
                total = prixUnitaire.multiply(BigDecimal.valueOf(quantite));
            }

            this.sousTotal = total;
            logger.debug("Calculated sousTotal={} for LignedeProduit {} (promoQty={}, promoPrice={})", sousTotal, id, promoQty, promoPrice);
        } catch (Exception e) {
            logger.error("Error calculating sousTotal for LignedeProduit {}: {}", id, e.getMessage());
            this.sousTotal = BigDecimal.ZERO;
        }
    }
}
