package com.example.AsmGD1.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class QRCodeUtil {

    private static final String BASE_DIR;

    static {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            BASE_DIR = "C:/DATN/uploads/qr/";
        } else {
            BASE_DIR = System.getProperty("user.home") + "/DATN/uploads/qr/";
        }

        // Tạo thư mục nếu chưa tồn tại
        try {
            Path directoryPath = Paths.get(BASE_DIR);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
                System.out.println("Đã tạo thư mục QR: " + BASE_DIR);
            }
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo thư mục QR: " + BASE_DIR, e);
        }
    }

    public static void generateQRCodeImage(String text, int width, int height, String fileName)
            throws WriterException, IOException {
        String fullPath = BASE_DIR + fileName;
        Path path = Paths.get(fullPath);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);

        System.out.println("QR code đã được tạo tại: " + fullPath);
    }

    // Shortcut tạo mã QR cho 1 productDetailId
    public static void generateQRCodeForProduct(UUID productDetailId)
            throws WriterException, IOException {
        String fileName = "qr_" + productDetailId + ".png";
        generateQRCodeImage(productDetailId.toString(), 250, 250, fileName);
    }

    public static String getBaseDir() {
        return BASE_DIR;
    }
}
