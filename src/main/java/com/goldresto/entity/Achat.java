package com.goldresto.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Achat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fournisseur;

    private LocalDate dateAchat;

    private String referenceFacture;

    @Column(nullable = false)
    private BigDecimal total = BigDecimal.ZERO;

    @OneToMany(mappedBy = "achat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneAchat> lignes = new ArrayList<>();

    @PrePersist
    @PreUpdate
    public void updateTotal() {
        if (lignes == null || lignes.isEmpty()) {
            total = BigDecimal.ZERO;
            return;
        }
        total = lignes.stream()
            .map(LigneAchat::getSousTotal)
            .filter(value -> value != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFournisseur() {
        return fournisseur;
    }

    public void setFournisseur(String fournisseur) {
        this.fournisseur = fournisseur;
    }

    public LocalDate getDateAchat() {
        return dateAchat;
    }

    public void setDateAchat(LocalDate dateAchat) {
        this.dateAchat = dateAchat;
    }

    public String getReferenceFacture() {
        return referenceFacture;
    }

    public void setReferenceFacture(String referenceFacture) {
        this.referenceFacture = referenceFacture;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public List<LigneAchat> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneAchat> lignes) {
        this.lignes = (lignes != null) ? lignes : new ArrayList<>();
        this.lignes.forEach(ligne -> ligne.setAchat(this));
        updateTotal();
    }
}
