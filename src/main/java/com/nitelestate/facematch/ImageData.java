package com.nitelestate.facematch;

import com.nitelestate.common.exception.BadRequestException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/** Helpers for the base64 data URIs the app stores photos as, and for cropping faces out of a frame. */
final class ImageData {

    /** Rekognition's limit for images sent inline. */
    static final int MAX_BYTES = 5 * 1024 * 1024;

    private ImageData() {
    }

    /** Accepts "data:image/jpeg;base64,..." or bare base64. */
    static byte[] decode(String dataUri) {
        if (dataUri == null || dataUri.isBlank()) {
            return null;
        }
        int comma = dataUri.indexOf(',');
        String b64 = dataUri.startsWith("data:") && comma > 0 ? dataUri.substring(comma + 1) : dataUri;
        try {
            return Base64.getDecoder().decode(b64.trim());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("That image couldn't be read");
        }
    }

    static BufferedImage read(byte[] bytes) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) {
                throw new BadRequestException("Unsupported image format - please upload a JPEG or PNG");
            }
            return img;
        } catch (IOException e) {
            throw new BadRequestException("That image couldn't be read");
        }
    }

    /** Crops a face (box in 0..1 fractions) with some padding, so the search sees the whole head. */
    static BufferedImage crop(BufferedImage img, FaceEngine.Box box) {
        int w = img.getWidth(), h = img.getHeight();
        float pad = 0.25f;
        int x0 = clamp((int) ((box.left() - box.width() * pad) * w), 0, w - 1);
        int y0 = clamp((int) ((box.top() - box.height() * pad) * h), 0, h - 1);
        int x1 = clamp((int) ((box.left() + box.width() * (1 + pad)) * w), x0 + 1, w);
        int y1 = clamp((int) ((box.top() + box.height() * (1 + pad)) * h), y0 + 1, h);
        return img.getSubimage(x0, y0, x1 - x0, y1 - y0);
    }

    static byte[] jpeg(BufferedImage img) {
        BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        rgb.getGraphics().drawImage(img, 0, 0, null);
        try (var out = new ByteArrayOutputStream()) {
            ImageIO.write(rgb, "jpg", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static String dataUri(byte[] jpeg) {
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(jpeg);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
