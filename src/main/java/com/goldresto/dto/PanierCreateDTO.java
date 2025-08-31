package com.goldresto.dto;

import java.math.BigDecimal;
import java.util.List;

public class PanierCreateDTO {
    private Integer numeroTable;
    private String state;
    private BigDecimal total;
    private List<ProduitItemDTO> products;

    public static class ProduitItemDTO {
        private Long produitId;
        private Integer quantite;
        private BigDecimal prixUnitaire;

        public Long getProduitId() { return produitId; }
        public void setProduitId(Long produitId) { this.produitId = produitId; }
        public Integer getQuantite() { return quantite; }
        public void setQuantite(Integer quantite) { this.quantite = quantite; }
        public BigDecimal getPrixUnitaire() { return prixUnitaire; }
        public void setPrixUnitaire(BigDecimal prixUnitaire) { this.prixUnitaire = prixUnitaire; }
    }

    public Integer getNumeroTable() { return numeroTable; }
    public void setNumeroTable(Integer numeroTable) { this.numeroTable = numeroTable; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public List<ProduitItemDTO> getProducts() { return products; }
    public void setProducts(List<ProduitItemDTO> products) { this.products = products; }
}
