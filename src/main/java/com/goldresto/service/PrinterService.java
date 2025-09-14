package com.goldresto.service;

import com.goldresto.entity.Panier;
import com.goldresto.entity.LignedeProduit;
import com.goldresto.entity.Paiement;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.SimpleDoc;

@Service
public class PrinterService {
    private static final Logger logger = LoggerFactory.getLogger(PrinterService.class);

    public void printAddedProduct(Panier panier, LignedeProduit newProduct) {
        logger.debug("Printing added product receipt for panier {}", panier.getId());
        try {
            if (panier == null || newProduct == null) {
                throw new IllegalArgumentException("Panier and product cannot be null");
            }

            // Skip printing if the product is a beverage
            if ("boisson".equalsIgnoreCase(newProduct.getProduit().getCategorie())) {
                logger.debug("Skipping printing for beverage product: {}", newProduct.getProduit().getNomProduit());
                return;
            }

            StringBuilder receipt = new StringBuilder();
            receipt.append("ADDED PRODUCT\n\n")
                  .append(panier.getNumeroTable() >= 51 && panier.getNumeroTable() <= 61 ? "A emporter " : "Table: ")
                  .append(panier.getNumeroTable()).append("\n")
                  .append("Date: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n")
                  .append("----------------------------------------\n\n")
                  .append(newProduct.getQuantite())
                  .append("x ")
                  .append(newProduct.getProduit().getNomProduit())
                  .append("\n\n----------------------------------------\n")
                  .append("*** End of Receipt ***\n");

            printContent(receipt.toString());
            logger.info("Successfully printed added product receipt for panier {}", panier.getId());
        } catch (Exception e) {
            logger.error("Failed to print added product receipt for panier {}: {}", panier.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to print added product receipt: " + e.getMessage(), e);
        }
    }

    public void printKitchenReceipt(Panier panier) {
        logger.debug("Printing kitchen receipt for panier {}", panier.getId());
        try {
            if (panier == null) {
                throw new IllegalArgumentException("Panier cannot be null");
            }

            if (panier.getLignesProduits() == null || panier.getLignesProduits().isEmpty()) {
                throw new IllegalArgumentException("Panier has no products");
            }
            
            StringBuilder receipt = new StringBuilder();
            receipt.append("KITCHEN ORDER\n\n")
                  .append("Commande #: ").append(panier.getId()).append("\n")
                  .append("Date: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n")
                  .append(panier.getNumeroTable() >= 51 && panier.getNumeroTable() <= 61 ? "A emporter " : "Table: ")
                  .append(panier.getNumeroTable()).append("\n")
                  .append("----------------------------------------\n\n");
            
            // Filter out beverages and print only non-beverage items
            for (LignedeProduit ligne : panier.getLignesProduits()) {
                if (!"boisson".equalsIgnoreCase(ligne.getProduit().getCategorie())) {
                    receipt.append(ligne.getQuantite())
                          .append("x ")
                          .append(ligne.getProduit().getNomProduit())
                          .append("\n");
                }
            }
            
            receipt.append("\n----------------------------------------\n")
                  .append("*** End of Order ***\n");

            printContent(receipt.toString());
            logger.info("Successfully printed kitchen receipt for panier {}", panier.getId());
        } catch (Exception e) {
            logger.error("Failed to print kitchen receipt for panier {}: {}", panier.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to print kitchen receipt: " + e.getMessage(), e);
        }
    }
    
    public void printClientBill(Panier panier, Paiement paiement) {
        logger.debug("Printing client bill for panier {}", panier.getId());
        try {
            if (panier == null || paiement == null) {
                throw new IllegalArgumentException("Panier and payment cannot be null");
            }

            StringBuilder receipt = new StringBuilder();
            receipt.append("GOLDEN RESTO\n")
                  .append("Tel: 4452 6904\n")
                  .append("----------------------------------------\n\n")
                  .append("Facture #: ").append(paiement.getId()).append("\n")
                  .append("Date: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n")
                  .append(panier.getNumeroTable() >= 51 && panier.getNumeroTable() <= 61 ? "A emporter " : "Table: ")
                  .append(panier.getNumeroTable()).append("\n")
                  .append("----------------------------------------\n\n");

            // Print all products with their details
            for (LignedeProduit ligne : panier.getLignesProduits()) {
                receipt.append(String.format("%-20s", ligne.getProduit().getNomProduit()))
                      .append(String.format("%3dx ", ligne.getQuantite()))
                      .append(String.format("%8.2f", ligne.getPrixUnitaire()))
                      .append(" = ")
                      .append(String.format("%8.2f\n", ligne.getSousTotal()));
            }

            receipt.append("\n----------------------------------------\n")
                  .append(String.format("%-20s %14.2f\n", "Total:", panier.getTotal()))
                  .append(String.format("%-20s %14.2f\n", "Montant reçu:", paiement.getCashRecu()))
                  .append(String.format("%-20s %14.2f\n", "Monnaie:", paiement.getMonnaie()))
                  .append("\n----------------------------------------\n")
                  .append("Merci de votre visite!\n")
                  .append("A bientôt!\n");

            printContent(receipt.toString());
            logger.info("Successfully printed client bill for panier {}", panier.getId());
        } catch (Exception e) {
            logger.error("Failed to print client bill for panier {}: {}", panier.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to print client bill: " + e.getMessage(), e);
        }
    }

    private void printContent(String content) throws Exception {
        logger.debug("Preparing to print receipt");
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        PrintService rongtaPrinter = null;
        
        for (PrintService service : services) {
            if (service.getName().contains("RONGTA")) {
                rongtaPrinter = service;
                logger.debug("Found RONGTA printer: {}", service.getName());
                break;
            }
        }
        
        if (rongtaPrinter == null) {
            throw new IllegalStateException("RONGTA printer not found");
        }
        
        DocPrintJob job = rongtaPrinter.createPrintJob();
        Doc doc = new SimpleDoc(content.getBytes("CP437"), DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
        
        logger.debug("Sending print job to printer");
        job.print(doc, null);
        logger.debug("Print job sent successfully");
    }
}
