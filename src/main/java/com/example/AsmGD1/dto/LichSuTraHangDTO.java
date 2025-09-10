package com.example.AsmGD1.dto;

import com.example.AsmGD1.entity.LichSuTraHang;
import lombok.Data;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class LichSuTraHangDTO {
    private UUID id;
    private String maDonHang;
    private String hoTenKhachHang;
    private String tenSanPham;
    private int soLuong;
    private BigDecimal tongTienHoan;
    private String formattedTongHoan;
    private String lyDoTraHang;
    private String trangThai;
    private LocalDateTime thoiGianTra;
    private String tongGiamOrder;
    private String tongGiamShip;

    // Constructor, getters, setters
    public LichSuTraHangDTO(LichSuTraHang traHang, DecimalFormat formatter) {
        this.id = traHang.getId();
        this.maDonHang = traHang.getHoaDon().getDonHang().getMaDonHang();
        this.hoTenKhachHang = traHang.getHoaDon().getDonHang().getNguoiDung().getHoTen();
        this.tenSanPham = traHang.getChiTietDonHang().getTenSanPham();
        this.soLuong = traHang.getSoLuong();
        this.tongTienHoan = traHang.getTongTienHoan();
        this.formattedTongHoan = traHang.getTongTienHoan() != null ? formatter.format(traHang.getTongTienHoan()) : "0";
        this.lyDoTraHang = traHang.getLyDoTraHang();
        this.trangThai = traHang.getTrangThai();
        this.thoiGianTra = traHang.getThoiGianTra();
    }

    // Getters and setters
}
