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

@Service
public class PrinterService {
    private static final Logger logger = LoggerFactory.getLogger(PrinterService.class);

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
            
            // Add header
            receipt.append("KITCHEN ORDER\n\n")
                  .append("Commande #: ").append(panier.getId()).append("\n")
                  .append("Date: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n")
                  .append("Table: ").append(panier.getNumeroTable()).append("\n")
                  .append("----------------------------------------\n\n");
            
            // Add items
            for (LignedeProduit ligne : panier.getLignesProduits()) {
                receipt.append(ligne.getQuantite())
                      .append("x ")
                      .append(ligne.getProduit().getNomProduit())
                      .append("\n");
            }
            
            // Add footer
            receipt.append("\n----------------------------------------\n")
                  .append("*** End of Order ***\n");

            // Show print dialog and print
            printContent(receipt.toString());
            logger.info("Successfully printed kitchen receipt for panier {}", panier.getId());
            
        } catch (Exception e) {
            logger.error("Failed to print kitchen receipt for panier {}: {}", panier.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to print kitchen receipt: " + e.getMessage(), e);
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
