package com.example.AsmGD1.dto.BanHang;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class DonHangDTO {
    private UUID id;
    private UUID khachHangId;
    private String soDienThoaiKhachHang;
    private String tenKhachHang;
    private String diaChiGiaoHang;
    private String phuongThucThanhToan;
    private String phuongThucBanHang;
    private BigDecimal tongTienHang;
    private BigDecimal giamGia;
    private UUID idPhieuGiamGia;
    private BigDecimal phiVanChuyen;
    private BigDecimal tong;
    private BigDecimal soTienKhachDua;
    private String tabId;
    private List<GioHangItemDTO> danhSachSanPham;
    private UUID idPhieuFreeship; // để nhận voucher SHIPPING

}
