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

    // Label start for the GST breakdown rows — placed past the middle of the page
    // so there is a clear visual gap before the right-aligned amounts at COL_SUBTOTAL
    private static final float COL_GST_LABEL = MARGIN + 200;

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

                // ── Invoice meta ──────────────────────────────────────────────
                // Invoice number in GST-compliant format: INV-{orderId}-{yyyyMMdd}
                String invoiceDateTag = order.getOrderDate() != null
                        ? order.getOrderDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                        : "00000000";
                String invoiceNumber = "INV-" + order.getId() + "-" + invoiceDateTag;
                writeText(cs, PDType1Font.HELVETICA_BOLD, 11, MARGIN, y, "Invoice #: " + invoiceNumber);

                // Display date in human-readable form (no time component on a GST invoice)
                String displayDate = order.getOrderDate() != null
                        ? order.getOrderDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                        : "N/A";
                writeText(cs, PDType1Font.HELVETICA, 11, PAGE_WIDTH - MARGIN - 180, y, "Date: " + displayDate);
                y -= 16;

                // GSTIN placeholder — TODO: replace with real GSTIN once GST registration is complete
                writeText(cs, PDType1Font.HELVETICA, 10, MARGIN, y, "GSTIN: 27XXXXX0000X1ZX");
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

                // -- Separator before GST breakdown --
                y -= 4;
                drawLine(cs, MARGIN, y, PAGE_WIDTH - MARGIN, y);
                y -= 18;

                // ── GST Breakdown ─────────────────────────────────────────────
                // Recompute subtotal from items (qty × price) — source of truth,
                // avoids any rounding drift that may exist in order.getTotalAmount()
                double subtotal = 0.0;
                for (OrderItem item : order.getItems()) {
                    subtotal += item.getQuantityKg() * item.getPricePerKg();
                }

                // calculateGst returns [cgst, sgst, totalGst]; slab chosen by subtotal value
                double[] gst       = calculateGst(subtotal);
                double   cgst      = gst[0];
                double   sgst      = gst[1];
                double   grandTotal = subtotal + gst[2]; // subtotal + CGST + SGST

                // Label text reflects the applicable slab so the customer can verify
                String cgstLabel = (subtotal > 1000.0) ? "CGST (6%):"   : "CGST (2.5%):";
                String sgstLabel = (subtotal > 1000.0) ? "SGST (6%):"   : "SGST (2.5%):";

                // Subtotal row (pre-GST amount)
                writeText(cs, PDType1Font.HELVETICA, 11, COL_GST_LABEL, y, "Subtotal (before GST):");
                writeText(cs, PDType1Font.HELVETICA, 11, COL_SUBTOTAL,  y, "Rs. " + String.format("%.2f", subtotal));
                y -= 16;

                // CGST row
                writeText(cs, PDType1Font.HELVETICA, 11, COL_GST_LABEL, y, cgstLabel);
                writeText(cs, PDType1Font.HELVETICA, 11, COL_SUBTOTAL,  y, "Rs. " + String.format("%.2f", cgst));
                y -= 16;

                // SGST row
                writeText(cs, PDType1Font.HELVETICA, 11, COL_GST_LABEL, y, sgstLabel);
                writeText(cs, PDType1Font.HELVETICA, 11, COL_SUBTOTAL,  y, "Rs. " + String.format("%.2f", sgst));
                y -= 10;

                // ── Grand total separator and row ─────────────────────────────
                drawLine(cs, MARGIN, y, PAGE_WIDTH - MARGIN, y);
                y -= 16;

                // Grand total = subtotal + CGST + SGST (bold to draw the eye)
                writeText(cs, PDType1Font.HELVETICA_BOLD, 12, COL_GST_LABEL, y, "TOTAL (incl. GST):");
                writeText(cs, PDType1Font.HELVETICA_BOLD, 12, COL_SUBTOTAL,  y, "Rs. " + String.format("%.2f", grandTotal));
                y -= 6;
                drawLine(cs, MARGIN, y, PAGE_WIDTH - MARGIN, y);

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

    /**
     * Calculates CGST, SGST, and combined GST for a given pre-tax order subtotal.
     *
     * <p>GST slabs applied:
     * <ul>
     *   <li>Subtotal ≤ ₹1000 — standard grocery rate: 5% total (2.5% CGST + 2.5% SGST)</li>
     *   <li>Subtotal  > ₹1000 — higher rate: 12% total (6% CGST + 6% SGST)</li>
     * </ul>
     *
     * @param itemTotal pre-GST sum of all items (quantityKg × pricePerKg)
     * @return double[3] → [cgst, sgst, totalGst]
     */
    private double[] calculateGst(double itemTotal) {

        double cgstRate;
        double sgstRate;

        if (itemTotal > 1000.0) {
            // Higher slab: 12% total (6% CGST + 6% SGST) for orders above ₹1000
            cgstRate = 0.06;
            sgstRate = 0.06;
        } else {
            // Standard grocery slab: 5% total (2.5% CGST + 2.5% SGST)
            cgstRate = 0.025;
            sgstRate = 0.025;
        }

        double cgst     = itemTotal * cgstRate;
        double sgst     = itemTotal * sgstRate;
        double totalGst = cgst + sgst;

        return new double[]{cgst, sgst, totalGst};
    }

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
