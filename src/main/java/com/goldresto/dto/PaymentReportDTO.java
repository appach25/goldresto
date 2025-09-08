package com.goldresto.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentReportDTO {
    private Long id;
    private Long panierId;
    private Integer numeroTable;
    private BigDecimal cashRecu;
    private BigDecimal monnaie;
    private BigDecimal montantAPayer;
    private BigDecimal montant;
    private String methodePaiement;
    private LocalDateTime dateCreation;
    private BigDecimal panierTotal;
    private String panierState;
    private Long userId;
    private String username;
    private String fullName;

    // Constructor for JPA native query
    public PaymentReportDTO(Long id, Long panierId, Integer numeroTable, BigDecimal cashRecu, 
            BigDecimal monnaie, BigDecimal montantAPayer, BigDecimal montant, String methodePaiement, 
            LocalDateTime dateCreation, BigDecimal panierTotal, String panierState, 
            Long userId, String username, String fullName) {
        this.id = id;
        this.panierId = panierId;
        this.numeroTable = numeroTable;
        this.cashRecu = cashRecu;
        this.monnaie = monnaie;
        this.montantAPayer = montantAPayer;
        this.montant = montant;
        this.methodePaiement = methodePaiement;
        this.dateCreation = dateCreation;
        this.panierTotal = panierTotal;
        this.panierState = panierState;
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
    }

    // Getters
    public Long getId() { return id; }
    public Long getPanierId() { return panierId; }
    public Integer getNumeroTable() { return numeroTable; }
    public BigDecimal getCashRecu() { return cashRecu; }
    public BigDecimal getMonnaie() { return monnaie; }
    public BigDecimal getMontantAPayer() { return montantAPayer; }
    public BigDecimal getMontant() { return montant; }
    public String getMethodePaiement() { return methodePaiement; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public BigDecimal getPanierTotal() { return panierTotal; }
    public String getPanierState() { return panierState; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
}
