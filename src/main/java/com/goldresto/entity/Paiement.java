package com.goldresto.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
public class Paiement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "panier_id")
    @JsonManagedReference
    private Panier panier;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"password", "roles"})
    private User user;

    private Integer numeroTable;
    
    @Column(name = "cash_recu")
    private BigDecimal cashRecu;
    
    @Column(name = "monnaie")
    private BigDecimal monnaie;
    
    @Column(name = "montantapayer")
    private BigDecimal montantAPayer;
    
    @Column(name = "montant")
    private BigDecimal montant;
    
    @Column(name = "methode_paiement")
    private String methodePaiement;
    
    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @PrePersist
    public void prePersist() {
        this.dateCreation = LocalDateTime.now();
        if (this.panier != null) {
            this.montantAPayer = this.panier.getTotal();
            this.montant = this.panier.getTotal();
            if (this.cashRecu != null && this.montantAPayer != null) {
                this.monnaie = this.cashRecu.subtract(this.montantAPayer);
            }
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
            this.montant = panier.getTotal();
            this.user = panier.getUser(); // Set the user from panier
        }
    }

    public Integer getNumeroTable() {
        return numeroTable;
    }

    public void setNumeroTable(Integer numeroTable) {
        this.numeroTable = numeroTable;
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

    public void setMonnaie(BigDecimal monnaie) {
        this.monnaie = monnaie;
    }

    public BigDecimal getMontantAPayer() {
        return montantAPayer;
    }

    public void setMontantAPayer(BigDecimal montantAPayer) {
        this.montantAPayer = montantAPayer;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public String getMethodePaiement() {
        return methodePaiement;
    }

    public void setMethodePaiement(String methodePaiement) {
        this.methodePaiement = methodePaiement;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
