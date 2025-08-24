package com.goldresto.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Paiement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "panier_id")
    private Panier panier;

    private Integer numeroTable;
    private BigDecimal montantAPayer;
    private BigDecimal cashRecu;
    private BigDecimal monnaie;
    private LocalDateTime dateCreation;

    @PrePersist
    public void prePersist() {
        this.dateCreation = LocalDateTime.now();
        if (this.cashRecu != null && this.montantAPayer != null) {
            this.monnaie = this.cashRecu.subtract(this.montantAPayer);
        }
        if (this.panier != null) {
            this.panier.setState(PanierState.PAYER);
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Panier getPanier() {
        return panier;
    }

    public void setPanier(Panier panier) {
        this.panier = panier;
        if (panier != null) {
            this.numeroTable = panier.getNumeroTable();
            this.montantAPayer = panier.getTotal();
        }
    }

    public Integer getNumeroTable() {
        return numeroTable;
    }

    public void setNumeroTable(Integer numeroTable) {
        this.numeroTable = numeroTable;
    }

    public BigDecimal getMontantAPayer() {
        return montantAPayer;
    }

    public void setMontantAPayer(BigDecimal montantAPayer) {
        this.montantAPayer = montantAPayer;
    }

    public BigDecimal getCashRecu() {
        return cashRecu;
    }

    public void setCashRecu(BigDecimal cashRecu) {
        this.cashRecu = cashRecu;
        if (cashRecu != null && this.montantAPayer != null) {
            this.monnaie = cashRecu.subtract(this.montantAPayer);
        }
    }

    public BigDecimal getMonnaie() {
        return monnaie;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }
}
