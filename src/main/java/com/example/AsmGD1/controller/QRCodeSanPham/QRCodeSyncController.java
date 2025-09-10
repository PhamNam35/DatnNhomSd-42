package com.example.AsmGD1.controller.QRCodeSanPham;

import com.example.AsmGD1.service.SanPham.ChiTietSanPhamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/qrcode")
public class QRCodeSyncController {

    @Autowired
    private ChiTietSanPhamService chiTietSanPhamService;

    @GetMapping("/sync")
    public ResponseEntity<String> syncAllQRCodes() {
        chiTietSanPhamService.syncAllProductDetailsQRCode();
        return ResponseEntity.ok("Đã đồng bộ tất cả QR code cho sản phẩm chi tiết.");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteChiTiet(@PathVariable UUID id) {
        chiTietSanPhamService.deleteChiTietSanPham(id);
        return ResponseEntity.ok("Đã xóa sản phẩm chi tiết cùng với QR Code.");
    }
}
