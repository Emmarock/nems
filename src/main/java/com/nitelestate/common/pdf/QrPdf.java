package com.nitelestate.common.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.openpdf.text.BadElementException;
import org.openpdf.text.Image;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfTemplate;

import java.awt.Color;
import java.util.Map;

/**
 * Draws a QR code into a PDF as vector squares rather than an embedded bitmap, so it stays sharp at
 * whatever size it's printed. Shared by every PDF that carries gate QR codes.
 */
public final class QrPdf {

    private QrPdf() {
    }

    /** A QR image of exactly {@code size} points square, ready to add to a cell. */
    public static Image image(PdfContentByte canvas, String content, float size) {
        BitMatrix matrix;
        try {
            matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 2));
        } catch (WriterException e) {
            throw new IllegalStateException("Failed to encode QR code", e);
        }
        int modules = matrix.getWidth();
        float module = size / modules;
        PdfTemplate template = canvas.createTemplate(size, size);
        template.setColorFill(Color.BLACK);
        for (int y = 0; y < modules; y++) {
            for (int x = 0; x < modules; x++) {
                if (matrix.get(x, y)) {
                    // PDF's origin is bottom-left; the matrix's is top-left.
                    template.rectangle(x * module, size - (y + 1) * module, module, module);
                }
            }
        }
        template.fill();
        try {
            Image image = Image.getInstance(template);
            // A cell otherwise stretches an image to its full width; keep the QR at its intended print size.
            image.setWidthPercentage(0);
            image.scaleAbsolute(size, size);
            return image;
        } catch (BadElementException e) {
            throw new IllegalStateException(e);
        }
    }
}
