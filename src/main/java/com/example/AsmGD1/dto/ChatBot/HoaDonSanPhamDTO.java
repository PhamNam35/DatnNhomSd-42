package com.example.AsmGD1.dto.ChatBot;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class HoaDonSanPhamDTO {
    private UUID idHoaDon;
    private UUID idDonHang;
    private UUID idChiTietSanPham;
    private String tenSanPham;
    private int soLuong;
    private BigDecimal gia;
    private BigDecimal thanhTien;
    private String ghiChu;
    private LocalDateTime ngayTaoHoaDon;
}
