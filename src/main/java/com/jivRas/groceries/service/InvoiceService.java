package com.jivRas.groceries.service;

import com.jivRas.groceries.entity.Order;
import com.jivRas.groceries.entity.OrderItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class InvoiceService {

    private static final float MARGIN      = 50f;
    private static final float PAGE_WIDTH  = PDRectangle.A4.getWidth();   // 595 pt
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();  // 842 pt

    // Column x-offsets for the item table
    private static final float COL_PRODUCT  = MARGIN;
    private static final float COL_QTY      = MARGIN + 240;
    private static final float COL_RATE     = MARGIN + 330;
    private static final float COL_SUBTOTAL = MARGIN + 420;

    public byte[] generateInvoice(Order order) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {

                float y = PAGE_HEIGHT - MARGIN;

                // ── Header ───────────────────────────────────────────────────
                y = writeText(cs, PDType1Font.HELVETICA_BOLD, 22, MARGIN, y, "JivRas Groceries");
                y -= 6;
                drawLine(cs, MARGIN, y, PAGE_WIDTH - MARGIN, y);
                y -= 20;

                // ── Invoice meta (ID left, Date right) ───────────────────────
                writeText(cs, PDType1Font.HELVETICA_BOLD, 11, MARGIN, y, "Invoice ID: #" + order.getId());

                String dateStr = order.getOrderDate() != null
                        ? order.getOrderDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
                        : "N/A";
                writeText(cs, PDType1Font.HELVETICA, 11, PAGE_WIDTH - MARGIN - 180, y, "Date: " + dateStr);
                y -= 24;

                // ── Customer / Bill-To ────────────────────────────────────────
                writeText(cs, PDType1Font.HELVETICA_BOLD, 11, MARGIN, y, "Bill To:");
                y -= 16;

                String customerLine = safe(order.getCustomerName()) + "   Ph: " + safe(order.getMobile());
                writeText(cs, PDType1Font.HELVETICA, 11, MARGIN, y, customerLine);
                y -= 14;

                String addressLine = safe(order.getAddressLine()) + ", "
                        + safe(order.getCity()) + ", "
                        + safe(order.getState()) + " - "
                        + safe(order.getPincode());
                writeText(cs, PDType1Font.HELVETICA, 11, MARGIN, y, addressLine);
                y -= 24;

                drawLine(cs, MARGIN, y, PAGE_WIDTH - MARGIN, y);
                y -= 16;

                // ── Table header ──────────────────────────────────────────────
                writeText(cs, PDType1Font.HELVETICA_BOLD, 11, COL_PRODUCT,  y, "Product");
                writeText(cs, PDType1Font.HELVETICA_BOLD, 11, COL_QTY,      y, "Qty (kg)");
                writeText(cs, PDType1Font.HELVETICA_BOLD, 11, COL_RATE,     y, "Rate/kg");
                writeText(cs, PDType1Font.HELVETICA_BOLD, 11, COL_SUBTOTAL, y, "Subtotal");
                y -= 6;
                drawLine(cs, MARGIN, y, PAGE_WIDTH - MARGIN, y);
                y -= 16;

                // ── Item rows ─────────────────────────────────────────────────
                for (OrderItem item : order.getItems()) {
                    double subtotal = item.getQuantityKg() * item.getPricePerKg();

                    String name = safe(item.getProductName());
                    if (name.length() > 34) {
                        name = name.substring(0, 31) + "...";
                    }

                    writeText(cs, PDType1Font.HELVETICA, 11, COL_PRODUCT,  y, name);
                    writeText(cs, PDType1Font.HELVETICA, 11, COL_QTY,      y, String.format("%.2f", item.getQuantityKg()));
                    writeText(cs, PDType1Font.HELVETICA, 11, COL_RATE,     y, "Rs. " + String.format("%.2f", item.getPricePerKg()));
                    writeText(cs, PDType1Font.HELVETICA, 11, COL_SUBTOTAL, y, "Rs. " + String.format("%.2f", subtotal));
                    y -= 18;
                }

                // ── Separator before total ────────────────────────────────────
                y -= 4;
                drawLine(cs, MARGIN, y, PAGE_WIDTH - MARGIN, y);
                y -= 18;

                // ── Total row (right-aligned) ─────────────────────────────────
                writeText(cs, PDType1Font.HELVETICA_BOLD, 12, COL_RATE,     y, "Total:");
                writeText(cs, PDType1Font.HELVETICA_BOLD, 12, COL_SUBTOTAL, y,
                        "Rs. " + String.format("%.2f", order.getTotalAmount()));

                // ── Footer ────────────────────────────────────────────────────
                float footerY = MARGIN + 20;
                drawLine(cs, MARGIN, footerY, PAGE_WIDTH - MARGIN, footerY);
                footerY -= 16;
                writeText(cs, PDType1Font.HELVETICA, 10, MARGIN, footerY,
                        "Thank you for shopping with JivRas!");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Writes a single text string and returns the same y (for fluent chaining). */
    private float writeText(PDPageContentStream cs, PDType1Font font, float size,
                            float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
        return y;
    }

    private void drawLine(PDPageContentStream cs, float x1, float y1, float x2, float y2) throws IOException {
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    private String safe(String value) {
        return value != null ? value : "";
    }
}
