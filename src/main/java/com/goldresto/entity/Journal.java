package com.goldresto.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
public class Journal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer numeroTable;
    private Double total;
    private LocalDateTime datePaiement;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "journal_id")
    private List<JournalLigne> lignesProduits;

    private Double montantRecu;
    private Double monnaieRendue;
    private Long panierId; // Add this field to store the original Panier ID

    // Constructors, getters, setters

    public Journal() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getNumeroTable() { return numeroTable; }
    public void setNumeroTable(Integer numeroTable) { this.numeroTable = numeroTable; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public LocalDateTime getDatePaiement() { return datePaiement; }
    public void setDatePaiement(LocalDateTime datePaiement) { this.datePaiement = datePaiement; }

    public List<JournalLigne> getLignesProduits() { return lignesProduits; }
    public void setLignesProduits(List<JournalLigne> lignesProduits) { this.lignesProduits = lignesProduits; }

    public Double getMontantRecu() { return montantRecu; }
    public void setMontantRecu(Double montantRecu) { this.montantRecu = montantRecu; }

    public Double getMonnaieRendue() { return monnaieRendue; }
    public void setMonnaieRendue(Double monnaieRendue) { this.monnaieRendue = monnaieRendue; }

    public Long getPanierId() { return panierId; }
    public void setPanierId(Long panierId) { this.panierId = panierId; }
}
