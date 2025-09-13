package com.goldresto.service;

import com.goldresto.entity.Panier;
import com.goldresto.entity.LignedeProduit;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.awt.print.PrinterJob;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.SimpleDoc;
import java.util.List;
import java.math.BigDecimal;

@Service
public class PrinterService {
    private static final Logger logger = LoggerFactory.getLogger(PrinterService.class);

    public void printKitchenReceipt(Panier panier) {
        printKitchenReceipt(panier, null);
    }

    public void printKitchenReceipt(Panier panier, List<LignedeProduit> specificProducts) {
        logger.debug("Printing kitchen receipt for panier {}", panier.getId());
        try {
            if (panier == null) {
                throw new IllegalArgumentException("Panier cannot be null");
            }

            List<LignedeProduit> productsToPrint = specificProducts != null ? specificProducts : panier.getLignesProduits();
            if (productsToPrint == null || productsToPrint.isEmpty()) {
                throw new IllegalArgumentException("No products to print");
            }
            
            StringBuilder receipt = new StringBuilder();
            
            // Add header
            receipt.append("KITCHEN ORDER\n\n")
                  .append("Commande #: ").append(panier.getId()).append("\n")
                  .append("Date: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n")
                  .append("Table: ").append(panier.getNumeroTable()).append("\n");

            // Add update indicator if printing specific products
            if (specificProducts != null) {
                receipt.append("*** MISE A JOUR DE COMMANDE ***\n");
            }
            receipt.append("----------------------------------------\n\n");
            
            // Add items
            for (LignedeProduit ligne : productsToPrint) {
                receipt.append(ligne.getQuantite())
                      .append("x ")
                      .append(ligne.getProduit().getNomProduit())
                      .append("\n");
            }
            
            // Add footer
            receipt.append("\n----------------------------------------\n")
                  .append("*** End of Order ***\n");

            // Print without dialog
            printContent(receipt.toString());
            logger.info("Successfully printed kitchen receipt for panier {}", panier.getId());
            
        } catch (Exception e) {
            logger.error("Failed to print kitchen receipt for panier {}: {}", panier.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to print kitchen receipt: " + e.getMessage(), e);
        }
    }
    
    public void printClientBill(Panier panier, BigDecimal cashRecu, BigDecimal monnaie) {
        logger.debug("Printing client bill for panier {}", panier.getId());
        try {
            if (panier == null) {
                throw new IllegalArgumentException("Panier cannot be null");
            }

            StringBuilder receipt = new StringBuilder();
            
            // Add header with restaurant name
            receipt.append("GOLD RESTO\n")
                  .append("----------------------------------------\n\n")
                  .append("Facture #: ").append(panier.getId()).append("\n")
                  .append("Date: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n")
                  .append("Table: ").append(panier.getNumeroTable()).append("\n")
                  .append("----------------------------------------\n\n");
            
            // Add items with prices
            for (LignedeProduit ligne : panier.getLignesProduits()) {
                receipt.append(String.format("%-3d %-20s $%6.2f\n", 
                    ligne.getQuantite(),
                    ligne.getProduit().getNomProduit(),
                    ligne.getSousTotal().doubleValue()));
            }
            
            // Add totals section
            receipt.append("\n----------------------------------------\n")
                  .append(String.format("%-24s $%6.2f\n", "TOTAL:", panier.getTotal().doubleValue()))
                  .append(String.format("%-24s $%6.2f\n", "MONTANT RECU:", cashRecu.doubleValue()))
                  .append(String.format("%-24s $%6.2f\n", "MONNAIE:", monnaie.doubleValue()))
                  .append("----------------------------------------\n\n")
                  .append("          Merci de votre visite!\n")
                  .append("            A bientot chez\n")
                  .append("             GOLD RESTO\n");

            // Print the receipt
            printContent(receipt.toString());
            logger.info("Successfully printed client bill for panier {}", panier.getId());
            
        } catch (Exception e) {
            logger.error("Failed to print client bill for panier {}: {}", panier.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to print client bill: " + e.getMessage(), e);
        }
    }

    private void printContent(String content) throws Exception {
        logger.debug("Preparing to print receipt");
        
        // Find RONGTA printer
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
        
        // Create print job
        DocPrintJob job = rongtaPrinter.createPrintJob();
        Doc doc = new SimpleDoc(
            content.getBytes("CP437"),
            DocFlavor.BYTE_ARRAY.AUTOSENSE,
            null
        );
        
        // Print without showing dialog
        logger.debug("Sending print job to printer");
        job.print(doc, null);
        logger.debug("Print job sent successfully");
    }
}
