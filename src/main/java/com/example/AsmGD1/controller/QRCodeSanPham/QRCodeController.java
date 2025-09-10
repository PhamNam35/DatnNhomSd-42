package com.example.AsmGD1.controller.QRCodeSanPham;

import com.example.AsmGD1.util.QRCodeUtil;
import com.google.zxing.WriterException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@RestController
@RequestMapping("/generate-qr")
public class QRCodeController {

    // Thư mục lưu trữ QR code (tương đối so với root dự án hoặc tạm thời)
    private static final String QR_STORAGE_DIR = "src/main/resources/static/qr/";

    @GetMapping(produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<Resource> generateQRCode(@RequestParam("data") String data) {
        try {
            // Tách productDetailId từ chuỗi data (ví dụ: "productDetailId:uuid")
            String[] parts = data.split(":");
            if (parts.length != 2 || !parts[0].equals("productDetailId")) {
                return ResponseEntity.badRequest().build();
            }
            UUID productDetailId = UUID.fromString(parts[1]); // Chuyển đổi thành UUID

            // Tạo thư mục nếu chưa tồn tại
            Path directoryPath = Path.of(QR_STORAGE_DIR);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

            // Đường dẫn file
            String filePath = QR_STORAGE_DIR + "qr_" + productDetailId + ".png";
            QRCodeUtil.generateQRCodeImage(productDetailId.toString(), 250, 250, filePath);

            File qrFile = new File(filePath);
            if (!qrFile.exists()) {
                return ResponseEntity.notFound().build();
            }

            Resource fileResource = new FileSystemResource(qrFile);
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf(MediaType.IMAGE_PNG_VALUE))
                    .body(fileResource);

        } catch (IOException | WriterException e) {
            return ResponseEntity.internalServerError().build();
        } catch (IllegalArgumentException e) { // Bắt lỗi khi UUID không hợp lệ
            return ResponseEntity.badRequest().build();
        }
    }
}