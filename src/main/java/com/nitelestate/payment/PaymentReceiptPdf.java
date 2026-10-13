package com.nitelestate.payment;

import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Renders the one-page PDF attached to a payment confirmation email (see
 * PaymentService.sendReceipt) — same brand colours as sender.EmailTemplate's HTML (the #145a32
 * header green), reproduced here in java.awt.Color since a PDF renders independently of the
 * email body's own markup.
 */
public final class PaymentReceiptPdf {

    private static final Color BRAND_GREEN = new Color(0x14, 0x5A, 0x32);
    private static final Color BRAND_GREEN_LIGHT = new Color(0xBF, 0xE3, 0xCC);
    private static final Color INK = new Color(0x1F, 0x2A, 0x24);
    private static final Color MUTED = new Color(0x6D, 0x7A, 0x73);
    private static final Color BORDER = new Color(0xE6, 0xEB, 0xE8);
    private static final ZoneId ESTATE_ZONE = ZoneId.of("Africa/Lagos");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a");

    private PaymentReceiptPdf() {
    }

    public static byte[] render(String residentFullName, String receiptNumber, String levyDescription,
                                 BigDecimal amount, String method, Instant paidAt, String houseNumber, String fullAddress) {
        Document document = new Document(PageSize.A4, 40, 40, 0, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(headerBand());

            Paragraph title = new Paragraph("Payment Receipt", new Font(Font.HELVETICA, 18, Font.BOLD, INK));
            title.setSpacingBefore(28);
            title.setSpacingAfter(2);
            document.add(title);

            Paragraph receiptNo = new Paragraph(receiptNumber, new Font(Font.HELVETICA, 11, Font.NORMAL, MUTED));
            receiptNo.setSpacingAfter(20);
            document.add(receiptNo);

            PdfPTable rows = new PdfPTable(2);
            rows.setWidthPercentage(100);
            rows.setWidths(new float[]{1, 2});
            addRow(rows, "Resident", residentFullName);
            if (houseNumber != null) {
                addRow(rows, "Property", houseNumber);
            }
            if (fullAddress != null) {
                addRow(rows, "Address", fullAddress);
            }
            if (levyDescription != null) {
                addRow(rows, "For", levyDescription);
            }
            addRow(rows, "Amount", formatAmount(amount));
            addRow(rows, "Payment method", method.replace('_', ' '));
            addRow(rows, "Date paid", paidAt.atZone(ESTATE_ZONE).format(DATE_FORMAT));
            addRow(rows, "Status", "PAID");
            document.add(rows);

            Paragraph footer = new Paragraph(
                    "This receipt confirms payment received by Nitel Estate Management System. Keep it for your records.",
                    new Font(Font.HELVETICA, 9, Font.ITALIC, MUTED));
            footer.setSpacingBefore(28);
            document.add(footer);

            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate payment receipt PDF", e);
        }
        return out.toByteArray();
    }

    private static PdfPTable headerBand() {
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(BRAND_GREEN);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(22);
        cell.addElement(new Paragraph("Nitel Estate", new Font(Font.HELVETICA, 20, Font.BOLD, Color.WHITE)));
        Paragraph subtitle = new Paragraph("Estate Management System", new Font(Font.HELVETICA, 10, Font.NORMAL, BRAND_GREEN_LIGHT));
        subtitle.setSpacingBefore(3);
        cell.addElement(subtitle);
        header.addCell(cell);
        return header;
    }

    private static void addRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, new Font(Font.HELVETICA, 10, Font.BOLD, MUTED)));
        labelCell.setBorder(Rectangle.BOTTOM);
        labelCell.setBorderColor(BORDER);
        labelCell.setPadding(8);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, new Font(Font.HELVETICA, 11, Font.NORMAL, INK)));
        valueCell.setBorder(Rectangle.BOTTOM);
        valueCell.setBorderColor(BORDER);
        valueCell.setPadding(8);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private static String formatAmount(BigDecimal amount) {
        return "₦" + String.format("%,.2f", amount);
    }
}
