package com.nitelestate.qrtag;

import com.nitelestate.common.pdf.QrPdf;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * A print-ready A4 sheet of pre-generated QR tags (see QrTagService.generateBatch): a grid of
 * stickers, each with the QR and its printed code (V-000123). The code is what an admin types
 * (or scans) to link the sticker to a vehicle or property later. Thin grey borders are cut guides.
 */
public final class QrTagSheetPdf {

    /** One sticker: the code printed under the QR, and the URL the QR encodes. */
    public record Tag(String code, String scanUrl) {
    }

    private static final Color BRAND_GREEN = new Color(0x14, 0x5A, 0x32);
    private static final Color INK = new Color(0x1F, 0x2A, 0x24);
    private static final Color MUTED = new Color(0x6D, 0x7A, 0x73);
    private static final Color CUT_LINE = new Color(0xD5, 0xDB, 0xD8);

    private static final int COLUMNS = 4;
    /** ~3.5cm - comfortably scannable by a gate phone camera through a windscreen. */
    private static final float QR_SIZE = 98f;
    private static final float STICKER_HEIGHT = 150f;

    private QrTagSheetPdf() {
    }

    public static byte[] render(QrTagKind kind, int batchNo, List<Tag> tags) {
        Document document = new Document(PageSize.A4, 28, 28, 28, 28);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            String range = tags.isEmpty() ? "" : "  ·  " + tags.get(0).code() + " to " + tags.get(tags.size() - 1).code();
            Paragraph title = new Paragraph("Nitel Estate " + kind.label().toLowerCase() + " QR tags  ·  batch " + batchNo
                    + "  ·  " + tags.size() + " tag(s)" + range, new Font(Font.HELVETICA, 9, Font.NORMAL, MUTED));
            title.setSpacingAfter(8);
            document.add(title);

            document.add(grid(writer.getDirectContent(), kind, tags));
            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate QR tag sheet PDF", e);
        }
        return out.toByteArray();
    }

    private static PdfPTable grid(PdfContentByte canvas, QrTagKind kind, List<Tag> tags) {
        PdfPTable table = new PdfPTable(COLUMNS);
        table.setWidthPercentage(100);
        for (Tag tag : tags) {
            table.addCell(sticker(canvas, kind, tag));
        }
        // Pad the last row so every sticker keeps the same width and cut lines.
        int remainder = tags.size() % COLUMNS;
        if (remainder != 0) {
            for (int i = remainder; i < COLUMNS; i++) {
                PdfPCell blank = new PdfPCell();
                blank.setBorder(Rectangle.NO_BORDER);
                blank.setFixedHeight(STICKER_HEIGHT);
                table.addCell(blank);
            }
        }
        return table;
    }

    private static PdfPCell sticker(PdfContentByte canvas, QrTagKind kind, Tag tag) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(CUT_LINE);
        cell.setBorderWidth(0.5f);
        cell.setFixedHeight(STICKER_HEIGHT);
        cell.setPaddingTop(4);
        cell.setPaddingBottom(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph brand = new Paragraph("NITEL ESTATE", new Font(Font.HELVETICA, 8, Font.BOLD, BRAND_GREEN));
        brand.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(brand);

        Image qr = QrPdf.image(canvas, tag.scanUrl(), QR_SIZE);
        qr.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(qr);

        Paragraph code = new Paragraph(tag.code(), new Font(Font.COURIER, 12, Font.BOLD, INK));
        code.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(code);

        Paragraph label = new Paragraph(kind.label().toUpperCase() + " PASS", new Font(Font.HELVETICA, 6.5f, Font.NORMAL, MUTED));
        label.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(label);
        return cell;
    }
}
