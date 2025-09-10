package com.example.AsmGD1.dto.BanHang;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class GioHangItemDTO {
    private UUID idChiTietSanPham;
    private String tenSanPham;
    private String mauSac;
    private String kichCo;
    private int soLuong;
    private BigDecimal gia;
    private BigDecimal thanhTien;
    private Integer stockQuantity;
    private String hinhAnh; // Thêm trường này
    private String ghiChu;
    private Integer availableStock; // Added to reflect available stock after reservations

    private Boolean trangThaiHoanTra; // Thêm trường này

    private BigDecimal originalPrice; // Giá gốc
    private BigDecimal phanTramGiam;
}