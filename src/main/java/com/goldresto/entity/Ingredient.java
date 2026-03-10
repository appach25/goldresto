package com.goldresto.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Ingredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "produit_id", nullable = true)
    private Produit produit;

    @Column(nullable = false, unique = true)
    private String nom;

    @Column(nullable = false)
    private String uniteStock;

    @Column(nullable = false)
    private BigDecimal stockActuel = BigDecimal.ZERO;

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

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getUniteStock() {
        return uniteStock;
    }

    public void setUniteStock(String uniteStock) {
        this.uniteStock = uniteStock;
    }

    public BigDecimal getStockActuel() {
        return stockActuel;
    }

    public void setStockActuel(BigDecimal stockActuel) {
        this.stockActuel = stockActuel;
    }
}
