package com.goldresto.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@JsonInclude(JsonInclude.Include.ALWAYS)
public class Panier {
    private static final Logger logger = LoggerFactory.getLogger(Panier.class);
    public void calculateTotal() {
        logger.debug("Calculating total for Panier {}", id);
        
        // First ensure all lignes have valid sous-totals
        if (lignesProduits != null) {
            for (LignedeProduit ligne : lignesProduits) {
                if (ligne != null) {
                    ligne.calculateSousTotal();
                    logger.debug("Ligne {} sous-total: {}", ligne.getId(), ligne.getSousTotal());
                }
            }
        }

        // Now sum all sous-totals
        this.total = sumSousTotal();
        logger.debug("New total for Panier {}: {}", id, total);
    }

    /**
     * Sums the sousTotal of all LignedeProduit in this Panier and returns the value.
     */
    public BigDecimal sumSousTotal() {
        if (lignesProduits == null) return BigDecimal.ZERO;
        
        BigDecimal sum = BigDecimal.ZERO;
        for (LignedeProduit ligne : lignesProduits) {
            if (ligne != null && ligne.getSousTotal() != null) {
                sum = sum.add(ligne.getSousTotal());
                logger.debug("Adding sous-total {} for ligne {}, running sum: {}", 
                    ligne.getSousTotal(), ligne.getId(), sum);
            }
        }
        return sum;
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer numeroTable;

    @OneToMany(mappedBy = "panier", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<LignedeProduit> lignesProduits = new ArrayList<>();

    private LocalDateTime date = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private PanierState state = PanierState.EN_COURS;

    @OneToOne(mappedBy = "panier")
    private Paiement paiement;

    @Column(nullable = false)
    private BigDecimal total = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getNumeroTable() {
        return numeroTable;
    }

    public void setNumeroTable(Integer numeroTable) {
        this.numeroTable = numeroTable;
    }

    public List<LignedeProduit> getLignesProduits() {
        return lignesProduits;
    }

    public void setLignesProduits(List<LignedeProduit> lignesProduits) {
        this.lignesProduits = (lignesProduits != null) ? lignesProduits : new ArrayList<>();
        this.lignesProduits.forEach(ligne -> ligne.setPanier(this));
        updateAllSousTotals();
        calculateTotal();
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public PanierState getState() {
        return state;
    }

    public void setState(PanierState state) {
        this.state = state;
    }

    public Paiement getPaiement() {
        return paiement;
    }

    public void setPaiement(Paiement paiement) {
        this.paiement = paiement;
    }

    @JsonProperty("total")
    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }


    // Helper methods
    public void updateAllSousTotals() {
        logger.debug("Updating all sousTotals for Panier {}", id);
        if (lignesProduits != null) {
            for (LignedeProduit ligne : lignesProduits) {
                if (ligne != null) {
                    logger.debug("Processing ligne {} with produit {}", ligne.getId(), 
                        ligne.getProduit() != null ? ligne.getProduit().getId() : "null");
                    if (ligne.getProduit() != null && ligne.getProduit().getPrix() != null) {
                        ligne.setPrixUnitaire(ligne.getProduit().getPrix());
                        logger.debug("Set prixUnitaire to {} from produit", ligne.getPrixUnitaire());
                    } else {
                        ligne.setPrixUnitaire(BigDecimal.ZERO);
                        logger.debug("Set prixUnitaire to 0 due to null produit or prix");
                    }
                    ligne.calculateSousTotal();
                    logger.debug("Calculated sousTotal for ligne {}: {}", ligne.getId(), ligne.getSousTotal());
                }
            }
        } else {
            logger.debug("No lignesProduits found for Panier {}", id);
        }
    }

    public void recalculateTotal() {
        calculateTotal();
    }

    @Transactional
    public void addLigneProduit(LignedeProduit newLigne) {
        logger.debug("Adding ligne produit to Panier {}", id);
        
        // Validate inputs
        if (newLigne == null || newLigne.getProduit() == null) {
            logger.error("Cannot add null ligne or ligne with null produit to Panier {}", id);
            return;
        }

        // Find existing line with same product
        LignedeProduit existingLigne = lignesProduits.stream()
            .filter(l -> l.getProduit().getId().equals(newLigne.getProduit().getId()))
            .findFirst()
            .orElse(null);

        if (existingLigne != null) {
            logger.debug("Found existing ligne for produit {} in Panier {}", 
                newLigne.getProduit().getId(), id);
            int newQuantite = existingLigne.getQuantite() + newLigne.getQuantite();
            existingLigne.setQuantite(newQuantite);
            existingLigne.calculateSousTotal();
        } else {
            logger.debug("Adding new ligne for produit {} to Panier {}", 
                newLigne.getProduit().getId(), id);
            if (newLigne.getProduit() != null && newLigne.getProduit().getPrix() != null) {
                newLigne.setPrixUnitaire(newLigne.getProduit().getPrix());
                newLigne.calculateSousTotal(); 
            } else {
                newLigne.setPrixUnitaire(BigDecimal.ZERO);
                newLigne.setSousTotal(BigDecimal.ZERO);
            }
            lignesProduits.add(newLigne);
            newLigne.setPanier(this);
        }

        // Always recalculate total after modifying lignes
        updateAllSousTotals();
        calculateTotal();
        logger.debug("Updated Panier {} total to {} after adding ligne", id, total);
    }

    @Transactional
    public void removeLigneProduit(LignedeProduit ligne) {
        if (ligne != null) {
            logger.debug("Removing ligne {} from Panier {}", ligne.getId(), id);
            lignesProduits.remove(ligne);
            ligne.setPanier(null);
            ligne.setSousTotal(BigDecimal.ZERO);
            updateAllSousTotals();
            calculateTotal();
            logger.debug("Updated Panier {} total to {} after removing ligne", id, total);
        }
    }
}


