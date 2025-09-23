package com.goldresto.service;

import com.goldresto.entity.Panier;
import com.goldresto.entity.LignedeProduit;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;
// Note: Avoid importing javax.print.PrintService to prevent name clash with this class

@Service
public class PrintService {
    private static final Logger logger = LoggerFactory.getLogger(PrintService.class);
    
    private static final String[] PRINTER_NAMES = {"POS-58", "RONGTA"};  // Supported printer names
    private static final int MAX_CHAR_PER_LINE = 32;      // Standard for 58mm receipt printer
    
    public void printKitchenReceipt(Panier panier) {
        try {
            String receipt = generateKitchenReceipt(panier);
            printReceipt(receipt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to print kitchen receipt: " + e.getMessage(), e);
        }
    }

    private String generateKitchenReceipt(Panier panier) {
        StringBuilder receipt = new StringBuilder();
        
        // Header
        receipt.append(centerText("COMMANDE CUISINE")).append("\\n");
        receipt.append(repeatChar('-', MAX_CHAR_PER_LINE)).append("\\n");
        
        // Table number and timestamp
        receipt.append(String.format("TABLE: %d", panier.getNumeroTable())).append("\\n");
        receipt.append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\\n");
        receipt.append(repeatChar('-', MAX_CHAR_PER_LINE)).append("\\n\\n");
        
        // Items
        for (LignedeProduit ligne : panier.getLignesProduits()) {
            receipt.append(String.format("x%d %s\\n", 
                ligne.getQuantite(), 
                ligne.getProduit().getNomProduit()));
        }
        
        receipt.append("\\n").append(repeatChar('=', MAX_CHAR_PER_LINE)).append("\\n\\n\\n");
        
        return receipt.toString();
    }

    private void printReceipt(String receipt) throws Exception {
        logger.info("Attempting to print receipt via Java Print Service");
        logger.debug("Receipt content:\n{}", receipt);

        // Build ESC/POS wrapped content
        StringBuilder formatted = new StringBuilder();
        formatted.append("\u001B@"); // init
        formatted.append("\u001B!0"); // normal
        formatted.append(receipt);
        formatted.append("\n\n\n");
        formatted.append("\u001Bm"); // partial cut

        // Locate printer by known names
        javax.print.PrintService target = null;
        javax.print.PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (javax.print.PrintService svc : services) {
            String name = svc.getName();
            if (name == null) continue;
            for (String candidate : PRINTER_NAMES) {
                if (name.toLowerCase().contains(candidate.toLowerCase())) {
                    target = svc;
                    logger.info("Selected printer: {}", name);
                    break;
                }
            }
            if (target != null) break;
        }

        if (target == null) {
            StringBuilder available = new StringBuilder();
            for (javax.print.PrintService svc : services) {
                available.append("[").append(svc.getName()).append("] ");
            }
            throw new Exception("Receipt printer not found. Available: " + available);
        }

        DocPrintJob job = target.createPrintJob();
        Doc doc = new javax.print.SimpleDoc(
            formatted.toString().getBytes("CP437"),
            DocFlavor.BYTE_ARRAY.AUTOSENSE,
            null
        );
        PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
        attrs.add(new JobName("Kitchen Order", null));

        job.print(doc, attrs);
        logger.info("Print job dispatched to {}", target.getName());
    }

    private String centerText(String text) {
        if (text.length() >= MAX_CHAR_PER_LINE) return text;
        int spaces = (MAX_CHAR_PER_LINE - text.length()) / 2;
        return String.format("%" + spaces + "s%s%" + spaces + "s", "", text, "");
    }

    private String repeatChar(char c, int count) {
        return new String(new char[count]).replace('\0', c);
    }

}
