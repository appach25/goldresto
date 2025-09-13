package com.goldresto.service;

import com.goldresto.entity.Produit;
import com.goldresto.entity.Panier;
import com.goldresto.entity.LignedeProduit;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.print.PrintServiceLookup;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.SimpleDoc;

@Service
public class PrintService {
    private static final Logger logger = LoggerFactory.getLogger(PrintService.class);

    public void printProduitAjoute(Produit produit, Long commandeId) {
        if (produit == null || commandeId == null) {
            throw new IllegalArgumentException("Produit or commandeId is null");
        }
        
        StringBuilder receipt = new StringBuilder();
        receipt.append("ADDED PRODUCT\n\n")
              .append("Commande #: ").append(commandeId).append("\n")
              .append("Date: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n")
              .append("----------------------------------------\n\n")
              .append("1x ").append(produit.getNomProduit()).append("\n")
              .append("\n----------------------------------------\n")
              .append("*** End of Receipt ***\n");

        try {
            printContent(receipt.toString());
            logger.info("Successfully printed added product receipt for commande {}", commandeId);
        } catch (Exception e) {
            logger.error("Failed to print added product receipt: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to print receipt: " + e.getMessage(), e);
        }
    }

    private void printContent(String content) throws Exception {
        logger.debug("Preparing to print receipt");
        javax.print.PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        javax.print.PrintService rongtaPrinter = null;
        
        for (javax.print.PrintService service : services) {
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
