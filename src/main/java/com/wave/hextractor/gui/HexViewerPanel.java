package com.wave.hextractor.gui;

import javax.swing.*;
import java.awt.*;

public class HexViewerPanel extends JPanel {
    private byte[] fileBytes;
    private int offset;
    private int visibleColumns;
    private int visibleRows = 32;
    private Font baseFont;
    // Simple caret/selection support
    private int caretIndex = -1; // absolute index in fileBytes
    private boolean caretInAscii = false;

    public HexViewerPanel(byte[] fileBytes, int offset, int visibleColumns, Font baseFont) {
        this.fileBytes = fileBytes;
        this.offset = offset;
        this.visibleColumns = visibleColumns;
        this.baseFont = baseFont;
        setBackground(Color.BLACK);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setFont(baseFont);
        FontMetrics fm = g.getFontMetrics();
        int charHeight = fm.getHeight();
        int charWidth = fm.charWidth('0');
        int bytesPerRow = visibleColumns;
        int totalRows = (int) Math.ceil((double) fileBytes.length / bytesPerRow);

        // Calcular posiciones y anchos de las 3 tablas
        int xOffset = 5;
        int offsetWidth = fm.stringWidth("00000000");
        
        int xHex = xOffset + offsetWidth + 10;
        int hexWidth = bytesPerRow * 3 * charWidth;
        
        int xAscii = xHex + hexWidth + 10;
        int asciiWidth = bytesPerRow * charWidth;
        
        int totalHeight = totalRows * charHeight;

        // Dibujar bordes AMARILLOS visibles alrededor de las 3 tablas
        g.setColor(Color.YELLOW);
        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new java.awt.BasicStroke(2));
        g2.drawRect(xOffset - 2, 2, offsetWidth + 4, totalHeight + 4);
        g2.drawRect(xHex - 2, 2, hexWidth + 4, totalHeight + 4);
        g2.drawRect(xAscii - 2, 2, asciiWidth + 4, totalHeight + 4);

        // DIBUJAR SÓLO LAS FILAS VISIBLES según offset y visibleRows
        int startRow = Math.max(0, offset / bytesPerRow);
        int rowsToDraw = Math.min(visibleRows, Math.max(0, totalRows - startRow));
        for (int r = 0; r < rowsToDraw; r++) {
            int row = startRow + r;
            int y = (r + 1) * charHeight;
            int rowOffset = row * bytesPerRow;
            if (rowOffset >= fileBytes.length) break;
            // Offset
            g.setColor(Color.LIGHT_GRAY);
            g.drawString(String.format("%08X", rowOffset), xOffset, y);
            // Hex
            g.setColor(Color.WHITE);
            StringBuilder hex = new StringBuilder();
            StringBuilder ascii = new StringBuilder();
            for (int col = 0; col < bytesPerRow; col++) {
                int idx = rowOffset + col;
                if (idx < fileBytes.length) {
                    byte b = fileBytes[idx];
                    hex.append(String.format("%02X ", b));
                    ascii.append((b >= 32 && b <= 126) ? (char) b : '.');
                } else {
                    hex.append("   ");
                    ascii.append(' ');
                }
            }
            g.drawString(hex.toString(), xHex, y);
            // ASCII
            g.setColor(Color.GREEN);
            g.drawString(ascii.toString(), xAscii, y);

            // Dibujar caret si está en esta fila
            if (caretIndex >= rowOffset && caretIndex < rowOffset + bytesPerRow) {
                int caretCol = caretIndex - rowOffset;
                g.setColor(new Color(255, 255, 0, 128));
                if (caretInAscii) {
                    int cx = xAscii + caretCol * charWidth;
                    g.fillRect(cx, y - charHeight + 2, Math.max(2, charWidth), charHeight);
                } else {
                    int cx = xHex + caretCol * 3 * charWidth;
                    g.fillRect(cx, y - charHeight + 2, Math.max(2, 2 * charWidth), charHeight);
                }
            }
        }
    }

    public void setOffset(int offset) {
        this.offset = offset;
        repaint();
    }

    public void setVisibleColumns(int visibleColumns) {
        this.visibleColumns = visibleColumns;
        repaint();
    }
    public void setVisibleRows(int visibleRows) {
        this.visibleRows = visibleRows;
        repaint();
    }
    public int getTotalRows() {
        int bytesPerRow = visibleColumns;
        return (int) Math.ceil((double) fileBytes.length / bytesPerRow);
    }
    
    public int getRequiredWidth() {
        if (baseFont == null) return 800;
        FontMetrics fm = getFontMetrics(baseFont);
        int charWidth = fm.charWidth('0');
        int bytesPerRow = visibleColumns;
        
        int xOffset = 5;
        int offsetWidth = fm.stringWidth("00000000");
        int xHex = xOffset + offsetWidth + 10;
        int hexWidth = bytesPerRow * 3 * charWidth;
        int xAscii = xHex + hexWidth + 10;
        int asciiWidth = bytesPerRow * charWidth;
        
        return xAscii + asciiWidth + 10;
    }
    
    public int getRequiredHeight() {
        // Height needed to display the currently configured number of visible rows.
        if (baseFont == null) return 600;
        FontMetrics fm = getFontMetrics(baseFont);
        int charHeight = fm.getHeight();
        int rows = Math.max(1, visibleRows);
        return rows * charHeight + 10;
    }

    public void setFileBytes(byte[] fileBytes) {
        this.fileBytes = fileBytes;
        repaint();
    }

    public void setBaseFont(Font baseFont) {
        this.baseFont = baseFont;
        repaint();
    }

    // Mouse click support to set caret
    public void enableMouseSelection() {
        this.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (baseFont == null) return;
                Graphics g = getGraphics();
                if (g == null) return;
                g.setFont(baseFont);
                FontMetrics fm = g.getFontMetrics();
                int charHeight = fm.getHeight();
                int charWidth = fm.charWidth('0');
                int bytesPerRow = visibleColumns;
                int startRow = Math.max(0, offset / bytesPerRow);
                int x = e.getX();
                int y = e.getY();
                int relRow = Math.max(0, (y / Math.max(1, charHeight)) - 1);
                int row = startRow + relRow;
                if (row < 0) return;
                int rowOffset = row * bytesPerRow;
                // Recompute columns x positions
                int xOffset = 5;
                int offsetWidth = fm.stringWidth("00000000");
                int xHex = xOffset + offsetWidth + 10;
                int hexWidth = bytesPerRow * 3 * charWidth;
                int xAscii = xHex + hexWidth + 10;
                int asciiWidth = bytesPerRow * charWidth;

                if (x >= xAscii && x < xAscii + asciiWidth) {
                    int col = Math.min(bytesPerRow - 1, Math.max(0, (x - xAscii) / Math.max(1, charWidth)));
                    caretInAscii = true;
                    caretIndex = Math.min(fileBytes.length - 1, rowOffset + col);
                    repaint();
                } else if (x >= xHex && x < xHex + hexWidth) {
                    int col = Math.min(bytesPerRow - 1, Math.max(0, (x - xHex) / Math.max(1, 3 * charWidth)));
                    caretInAscii = false;
                    caretIndex = Math.min(fileBytes.length - 1, rowOffset + col);
                    repaint();
                }
            }
        });
    }

    public int getCaretIndex() { return caretIndex; }
    public boolean isCaretInAscii() { return caretInAscii; }
    public void setCaretIndex(int idx) {
        if (fileBytes == null || fileBytes.length == 0) return;
        caretIndex = Math.max(0, Math.min(idx, fileBytes.length - 1));
        ensureCaretVisible();
        repaint();
    }
    public void moveCaretBy(int delta) {
        setCaretIndex((caretIndex < 0 ? 0 : caretIndex) + delta);
    }
    public void ensureCaretVisible() {
        if (baseFont == null || fileBytes == null) return;
        int bytesPerRow = Math.max(1, visibleColumns);
        int rowOfCaret = Math.max(0, caretIndex / bytesPerRow);
        int startRow = Math.max(0, offset / bytesPerRow);
        int endRow = startRow + Math.max(1, visibleRows) - 1;
        if (rowOfCaret < startRow) {
            offset = rowOfCaret * bytesPerRow;
        } else if (rowOfCaret > endRow) {
            offset = Math.max(0, (rowOfCaret - visibleRows + 1) * bytesPerRow);
        }
    }

    public int getOffset() { return offset; }
}
