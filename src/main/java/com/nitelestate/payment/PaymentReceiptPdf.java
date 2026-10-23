package com.nitelestate.payment;

import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfGState;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Renders the one-page PDF attached to a payment confirmation email (see
 * PaymentService.sendReceipt), laid out to match the CDA's own printed receipt book: letterhead
 * with the estate logo, a navy RECEIPT banner, the amount spelled out in words, and the estate
 * logo again as a faint full-page watermark behind it all.
 */
public final class PaymentReceiptPdf {

    private static final Color NAVY = new Color(0x1E, 0x11, 0x6B);
    private static final Color LAVENDER = new Color(0xB3, 0xB0, 0xD8);
    private static final Color INK = new Color(0x2A, 0x2A, 0x3C);
    private static final Color MUTED = new Color(0x6D, 0x6D, 0x85);
    private static final Color BORDER = new Color(0xDC, 0xDC, 0xE6);

    private static final String ESTATE_NAME = "NITEL ESTATE CDA";
    private static final String[] ESTATE_ADDRESS = {"Off Obafemi Awolowo Road", "Oke Ota-Ona, Ikorodu", "Lagos State"};

    private static final ZoneId ESTATE_ZONE = ZoneId.of("Africa/Lagos");
    private static final DateTimeFormatter RECEIPT_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter PAID_AT = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a");

    /** Read once at class load - the logo ships in the jar, so a miss here is a packaging fault, not a runtime condition. */
    private static final byte[] LOGO = loadLogo();

    private PaymentReceiptPdf() {
    }

    public static byte[] render(String residentFullName, String receiptNumber, String levyDescription,
                                 BigDecimal amount, String method, Instant paidAt, String houseNumber, String fullAddress) {
        Document document = new Document(PageSize.A4, 42, 42, 36, 42);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();
            drawWatermark(writer);

            document.add(letterhead());
            document.add(receiptMeta(receiptNumber, paidAt));
            document.add(detailRows(residentFullName, levyDescription, amount, method, houseNumber, fullAddress, paidAt));
            document.add(amountBox(amount));
            document.add(signatureBlock());

            Paragraph footer = new Paragraph(
                    "This receipt confirms payment received by " + ESTATE_NAME + ". Keep it for your records.",
                    new Font(Font.HELVETICA, 8.5f, Font.ITALIC, MUTED));
            footer.setSpacingBefore(26);
            document.add(footer);

            document.close();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("Failed to generate payment receipt PDF", e);
        }
        return out.toByteArray();
    }

    /** Faint, centred full-page logo drawn on the layer under the flowed content. */
    private static void drawWatermark(PdfWriter writer) throws DocumentException, IOException {
        if (LOGO == null) {
            return;
        }
        Image logo = Image.getInstance(LOGO);
        float width = PageSize.A4.getWidth() * 0.62f;
        logo.scaleAbsolute(width, width * logo.getHeight() / logo.getWidth());
        logo.setAbsolutePosition((PageSize.A4.getWidth() - logo.getScaledWidth()) / 2,
                (PageSize.A4.getHeight() - logo.getScaledHeight()) / 2);

        PdfGState faded = new PdfGState();
        faded.setFillOpacity(0.07f);

        PdfContentByte canvas = writer.getDirectContentUnder();
        canvas.saveState();
        canvas.setGState(faded);
        canvas.addImage(logo);
        canvas.restoreState();
    }

    /** Logo + estate name/address on the left, the navy RECEIPT banner on the right. */
    private static PdfPTable letterhead() throws DocumentException, IOException {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{3f, 2f});

        PdfPCell left = borderless();
        left.addElement(estateIdentity());
        header.addCell(left);

        PdfPCell right = borderless();
        right.addElement(receiptBanner());
        header.addCell(right);
        return header;
    }

    private static PdfPTable estateIdentity() throws DocumentException, IOException {
        PdfPTable identity = new PdfPTable(2);
        identity.setWidthPercentage(100);
        identity.setWidths(new float[]{1f, 3.2f});

        PdfPCell logoCell = borderless();
        if (LOGO != null) {
            Image logo = Image.getInstance(LOGO);
            logo.scaleToFit(54, 54);
            logoCell.addElement(logo);
        }
        identity.addCell(logoCell);

        PdfPCell text = borderless();
        text.addElement(new Paragraph(ESTATE_NAME, new Font(Font.HELVETICA, 17, Font.BOLD, NAVY)));
        Font addressFont = new Font(Font.HELVETICA, 9.5f, Font.NORMAL, NAVY);
        for (String line : ESTATE_ADDRESS) {
            Paragraph address = new Paragraph(line, addressFont);
            address.setSpacingBefore(1);
            text.addElement(address);
        }
        identity.addCell(text);
        return identity;
    }

    private static PdfPTable receiptBanner() {
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);

        PdfPCell navy = new PdfPCell(new Phrase("R E C E I P T", new Font(Font.HELVETICA, 15, Font.BOLD, Color.WHITE)));
        navy.setBackgroundColor(NAVY);
        navy.setBorder(Rectangle.NO_BORDER);
        navy.setPadding(16);
        navy.setHorizontalAlignment(Element.ALIGN_CENTER);
        banner.addCell(navy);

        PdfPCell accent = new PdfPCell(new Phrase(" "));
        accent.setBackgroundColor(LAVENDER);
        accent.setBorder(Rectangle.NO_BORDER);
        accent.setFixedHeight(9);
        banner.addCell(accent);
        return banner;
    }

    private static PdfPTable receiptMeta(String receiptNumber, Instant paidAt) throws DocumentException {
        PdfPTable meta = new PdfPTable(2);
        meta.setWidthPercentage(100);
        meta.setWidths(new float[]{1f, 3f});
        meta.setSpacingBefore(26);

        meta.addCell(borderless());

        PdfPCell values = borderless();
        values.addElement(metaLine("Receipt No:", receiptNumber));
        values.addElement(metaLine("Date:", paidAt.atZone(ESTATE_ZONE).format(RECEIPT_DATE)));
        meta.addCell(values);
        return meta;
    }

    private static Paragraph metaLine(String label, String value) {
        Paragraph line = new Paragraph();
        line.setAlignment(Element.ALIGN_RIGHT);
        line.setSpacingBefore(3);
        line.add(new Phrase(label + "  ", new Font(Font.HELVETICA, 10.5f, Font.BOLD, NAVY)));
        line.add(new Phrase(value, new Font(Font.HELVETICA, 10.5f, Font.NORMAL, INK)));
        return line;
    }

    private static PdfPTable detailRows(String residentFullName, String levyDescription, BigDecimal amount,
                                         String method, String houseNumber, String fullAddress, Instant paidAt)
            throws DocumentException {
        PdfPTable rows = new PdfPTable(2);
        rows.setWidthPercentage(100);
        rows.setWidths(new float[]{1f, 2.4f});
        rows.setSpacingBefore(30);

        addRow(rows, "Received from", residentFullName);
        if (houseNumber != null) {
            addRow(rows, "Property", fullAddress != null ? fullAddress : houseNumber);
        }
        addRow(rows, "Amount", AmountInWords.spell(amount));
        addRow(rows, "Paid by", method.replace('_', ' '));
        addRow(rows, "For", levyDescription != null ? levyDescription : "Estate dues");
        addRow(rows, "Date paid", paidAt.atZone(ESTATE_ZONE).format(PAID_AT));
        return rows;
    }

    /** The figure version of the amount, in the navy block the printed receipt uses bottom-left. */
    private static PdfPTable amountBox(BigDecimal amount) throws DocumentException {
        PdfPTable wrapper = new PdfPTable(2);
        wrapper.setWidthPercentage(100);
        wrapper.setWidths(new float[]{1.3f, 1f});
        wrapper.setSpacingBefore(34);

        PdfPCell box = new PdfPCell(new Phrase("₦ " + String.format("%,.2f", amount),
                new Font(Font.HELVETICA, 21, Font.BOLD, Color.WHITE)));
        box.setBackgroundColor(NAVY);
        box.setBorder(Rectangle.NO_BORDER);
        box.setPadding(15);
        box.setHorizontalAlignment(Element.ALIGN_CENTER);
        wrapper.addCell(box);

        wrapper.addCell(borderless());
        return wrapper;
    }

    /**
     * Deliberately leaves the signature line blank rather than stamping a signature image - the
     * receipt is issued by the system, and reproducing a real officer's signature on it would
     * misrepresent who actually signed.
     */
    private static Paragraph signatureBlock() {
        Font label = new Font(Font.HELVETICA, 10, Font.NORMAL, NAVY);
        Font name = new Font(Font.HELVETICA, 11, Font.BOLD, NAVY);

        Paragraph block = new Paragraph();
        block.setAlignment(Element.ALIGN_RIGHT);
        block.setSpacingBefore(-62);
        block.add(new Phrase("Received By,\n", label));
        block.add(new Phrase("For, ", label));
        block.add(new Phrase(ESTATE_NAME + "\n\n\n", name));
        block.add(new Phrase("______________________\n", new Font(Font.HELVETICA, 10, Font.NORMAL, MUTED)));
        block.add(new Phrase(ESTATE_NAME, name));
        return block;
    }

    private static void addRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, new Font(Font.HELVETICA, 10.5f, Font.BOLD, NAVY)));
        labelCell.setBorder(Rectangle.BOTTOM);
        labelCell.setBorderColor(BORDER);
        labelCell.setPadding(9);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, new Font(Font.HELVETICA, 11, Font.NORMAL, INK)));
        valueCell.setBorder(Rectangle.BOTTOM);
        valueCell.setBorderColor(BORDER);
        valueCell.setPadding(9);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private static PdfPCell borderless() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0);
        return cell;
    }

    private static byte[] loadLogo() {
        try (InputStream in = PaymentReceiptPdf.class.getResourceAsStream("/branding/nitel-estate-logo.png")) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }
}
