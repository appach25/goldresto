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

    @PrePersist
    @PreUpdate
    public void calculateSousTotal() {
        logger.debug("Calculating sousTotal for LignedeProduit {} with quantite={} and prixUnitaire={}",
            id, quantite, prixUnitaire);
        if (quantite != null && prixUnitaire != null) {
            this.sousTotal = prixUnitaire.multiply(BigDecimal.valueOf(quantite));
            logger.debug("Calculated sousTotal={} for LignedeProduit {}", sousTotal, id);
        } else {
            this.sousTotal = BigDecimal.ZERO;
            logger.debug("Set sousTotal to 0 due to null quantite or prixUnitaire for LignedeProduit {}", id);
        }
    }
}
