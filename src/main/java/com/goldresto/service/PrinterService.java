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
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.Charset;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;

@Service
public class PrinterService {
    private static final Logger logger = LoggerFactory.getLogger(PrinterService.class);
    private static final int MAX_CHAR_58MM = 32;
    private static final int MAX_DOTS_WIDTH = 384;

    public void printAddedProduct(Panier panier, LignedeProduit newProduct, int addedQty) {
        logger.debug("Printing added product receipt for panier {} (addedQty={})", panier != null ? panier.getId() : null, addedQty);
        try {
            if (panier == null || newProduct == null) {
                throw new IllegalArgumentException("Panier and product cannot be null");
            }

            if (addedQty <= 0) {
                logger.debug("Skipping printing: addedQty <= 0 for product {}", newProduct.getProduit() != null ? newProduct.getProduit().getNomProduit() : "?");
                return;
            }

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
                  .append(addedQty)
                  .append("x ")
                  .append(newProduct.getProduit().getNomProduit())
                  .append("\n\n----------------------------------------\n")
                  .append("*** End of Receipt ***\n");

            printTextContent(receipt.toString(), "Added Product");
            logger.info("Successfully printed added product receipt for panier {} (qty={})", panier.getId(), addedQty);
        } catch (Exception e) {
            logger.error("Failed to print added product receipt for panier {}: {}", panier != null ? panier.getId() : null, e.getMessage(), e);
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
            
            boolean hasNonBeverage = false;
            for (LignedeProduit ligne : panier.getLignesProduits()) {
                if (!"boisson".equalsIgnoreCase(ligne.getProduit().getCategorie())) {
                    hasNonBeverage = true;
                    receipt.append(ligne.getQuantite())
                          .append("x ")
                          .append(ligne.getProduit().getNomProduit())
                          .append("\n");
                }
            }
            if (!hasNonBeverage) {
                logger.debug("Skipping kitchen receipt printing: order contains only beverages for panier {}", panier.getId());
                return;
            }
            
            receipt.append("\n----------------------------------------\n")
                  .append("*** End of Order ***\n");

            printTextContent(receipt.toString(), "Kitchen Order");
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

            byte[] escpos = buildClientBillEscPos(panier, paiement);
            printBytes(escpos, "Client Bill");
            logger.info("Successfully printed client bill for panier {}", panier.getId());
        } catch (Exception e) {
            logger.error("Failed to print client bill for panier {}: {}", panier.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to print client bill: " + e.getMessage(), e);
        }
    }

    private void printTextContent(String content, String jobName) throws Exception {
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
        PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
        attrs.add(new JobName(jobName, null));
        logger.debug("Sending print job to printer");
        job.print(doc, attrs);
        logger.debug("Print job sent successfully");
    }

    private void printBytes(byte[] data, String jobName) throws Exception {
        logger.debug("Preparing to print ESC/POS bytes");
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        PrintService target = null;
        for (PrintService service : services) {
            if (service.getName() != null && (service.getName().toUpperCase().contains("RONGTA") || service.getName().toUpperCase().contains("POS"))) {
                target = service;
                logger.debug("Using printer: {}", service.getName());
                break;
            }
        }
        if (target == null) throw new IllegalStateException("Receipt printer not found");

        DocPrintJob job = target.createPrintJob();
        Doc doc = new SimpleDoc(data, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
        PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
        attrs.add(new JobName(jobName, null));
        logger.debug("Sending ESC/POS job to printer");
        job.print(doc, attrs);
        logger.debug("ESC/POS job sent successfully");
    }

    private byte[] buildClientBillEscPos(Panier panier, Paiement paiement) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[]{0x1B, '@'});
        out.write(new byte[]{0x1B, 'a', 0x01});
        byte[] logo = loadLogoEscPos();
        if (logo != null) {
            out.write(logo);
            out.write("\n".getBytes(Charset.forName("CP437")));
        }
        out.write(new byte[]{0x1B, '!', 0x20});
        out.write("GOLDEN RESTO\n".getBytes("CP437"));
        out.write(new byte[]{0x1B, '!', 0x00});
        out.write("Tel: 4452 6904\n".getBytes("CP437"));
        out.write(repeat('-', MAX_CHAR_58MM).getBytes("CP437"));
        out.write("\n".getBytes("CP437"));

        out.write(new byte[]{0x1B, 'a', 0x00});
        out.write(("Facture #: " + paiement.getId() + "\n").getBytes("CP437"));
        out.write(("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n").getBytes("CP437"));
        String tableLabel = (panier.getNumeroTable() >= 51 && panier.getNumeroTable() <= 61 ? "A emporter " : "Table: ") + panier.getNumeroTable();
        out.write((tableLabel + "\n").getBytes("CP437"));
        out.write(repeat('-', MAX_CHAR_58MM).getBytes("CP437"));
        out.write("\n".getBytes("CP437"));

        for (LignedeProduit ligne : panier.getLignesProduits()) {
            String name = safe(ligne.getProduit().getNomProduit());
            String qty = String.format("%dx", ligne.getQuantite());
            String unit = String.format("%.2f", ligne.getPrixUnitaire());
            String subtotal = String.format("%.2f", ligne.getSousTotal());
            out.write((truncate(name, MAX_CHAR_58MM) + "\n").getBytes("CP437"));
            String mid = qty + " x " + unit + " = ";
            String line = padLeft(mid + subtotal, MAX_CHAR_58MM);
            out.write((line + "\n").getBytes("CP437"));
        }

        out.write(repeat('-', MAX_CHAR_58MM).getBytes("CP437"));
        out.write("\n".getBytes("CP437"));
        out.write((padLeft(String.format("Total: %.2f", panier.getTotal()), MAX_CHAR_58MM) + "\n").getBytes("CP437"));
        if (paiement.getCashRecu() != null) {
            out.write((padLeft(String.format("Montant reçu: %.2f", paiement.getCashRecu()), MAX_CHAR_58MM) + "\n").getBytes("CP437"));
        }
        if (paiement.getMonnaie() != null) {
            out.write((padLeft(String.format("Monnaie: %.2f", paiement.getMonnaie()), MAX_CHAR_58MM) + "\n").getBytes("CP437"));
        }
        out.write(repeat('-', MAX_CHAR_58MM).getBytes("CP437"));
        out.write("\n".getBytes("CP437"));

        out.write(new byte[]{0x1B, 'a', 0x01});
        out.write("Merci de votre visite!\n".getBytes("CP437"));
        out.write("A bientôt!\n".getBytes("CP437"));
        out.write("\n\n".getBytes("CP437"));
        out.write(new byte[]{0x1D, 'V', 0x42, 0x03});

        return out.toByteArray();
    }

    private String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) sb.append(c);
        return sb.toString();
    }

    private String padLeft(String text, int width) {
        if (text.length() >= width) return text;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < width - text.length(); i++) sb.append(' ');
        sb.append(text);
        return sb.toString();
    }

    private String truncate(String text, int width) {
        if (text == null) return "";
        return text.length() <= width ? text : text.substring(0, width);
    }

    private String safe(String s) { return s == null ? "" : s; }

    private byte[] loadLogoEscPos() {
        try {
            // Try classpath static/logo.png first
            BufferedImage img = null;
            try {
                img = ImageIO.read(PrinterService.class.getResourceAsStream("/static/logo.png"));
            } catch (Exception ignore) { }
            if (img == null) {
                // Try filesystem uploads/logo.png
                File f = new File("uploads/logo.png");
                if (f.exists()) img = ImageIO.read(f);
            }
            if (img == null) return null;
            BufferedImage mono = toMonochrome(resizeToWidth(img, Math.min(MAX_DOTS_WIDTH, img.getWidth())));
            return rasterImageToEscPos(mono);
        } catch (Exception e) {
            logger.warn("Failed to load/encode logo: {}", e.getMessage());
            return null;
        }
    }

    private BufferedImage resizeToWidth(BufferedImage src, int targetWidth) {
        if (src.getWidth() <= targetWidth) return src;
        int w = targetWidth;
        int h = (int) Math.round(src.getHeight() * (w / (double) src.getWidth()));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private BufferedImage toMonochrome(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = out.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }

    private byte[] rasterImageToEscPos(BufferedImage img) throws Exception {
        int width = img.getWidth();
        int height = img.getHeight();
        int bytesPerRow = (width + 7) / 8;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[]{0x1B, 'a', 0x01});
        out.write(new byte[]{0x1D, 'v', '0', 0x00});
        out.write((byte) (bytesPerRow & 0xFF));
        out.write((byte) ((bytesPerRow >> 8) & 0xFF));
        out.write((byte) (height & 0xFF));
        out.write((byte) ((height >> 8) & 0xFF));
        for (int y = 0; y < height; y++) {
            int bit = 0;
            int current = 0;
            for (int x = 0; x < width; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb) & 0xFF;
                int luminance = (r * 30 + g * 59 + b * 11) / 100;
                boolean black = luminance < 128;
                current <<= 1;
                if (black) current |= 1;
                bit++;
                if (bit == 8) {
                    out.write((byte) current);
                    bit = 0;
                    current = 0;
                }
            }
            if (bit != 0) {
                current <<= (8 - bit);
                out.write((byte) current);
            }
        }
        return out.toByteArray();
    }
}
