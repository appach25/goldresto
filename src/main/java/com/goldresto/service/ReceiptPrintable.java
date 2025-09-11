package com.goldresto.service;

import java.awt.*;
import java.awt.print.*;

public class ReceiptPrintable implements Printable {
    private final String content;
    private final Font headerFont = new Font("Arial", Font.BOLD, 14);
    private final Font normalFont = new Font("Arial", Font.PLAIN, 12);
    private final int lineHeight = 14;

    public ReceiptPrintable(String content) {
        this.content = content;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex > 0) {
            return NO_SUCH_PAGE;
        }

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        g2d.setColor(Color.BLACK);

        // Set initial position
        int y = 20;
        int x = 10;
        int width = (int) pageFormat.getImageableWidth();

        // Split content into lines
        String[] lines = content.split("\n");
        
        for (String line : lines) {
            // Check if line is a header (contains ESC commands)
            if (line.contains("\u001B")) {
                // Strip ESC commands
                line = line.replaceAll("\u001B\\[.*?m", "")
                         .replaceAll("\u001B[@a].*?", "");
            }

            // Center headers
            if (line.contains("KITCHEN ORDER") || line.contains("End of Order")) {
                g2d.setFont(headerFont);
                FontMetrics fm = g2d.getFontMetrics();
                x = (width - fm.stringWidth(line)) / 2;
            } else {
                g2d.setFont(normalFont);
                x = 10;
            }

            // Draw line
            if (!line.trim().isEmpty()) {
                g2d.drawString(line.trim(), x, y);
                y += lineHeight;
            }
        }

        return PAGE_EXISTS;
    }
}
