package com.example.AsmGD1.dto.ThongKe;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class SanPhamTonKhoThapDTO {
    private UUID idChiTietDonHang;
    private UUID idChiTietSanPham;
    private UUID idSanPham;
    private String tenSanPham;
    private String mauSac;
    private String kichCo;
    private BigDecimal gia;
    private Integer soLuongTonKho;
    private String imageUrl;

    public SanPhamTonKhoThapDTO(UUID idChiTietDonHang, UUID idChiTietSanPham, UUID idSanPham,
                                String tenSanPham, String mauSac, String kichCo,
                                BigDecimal gia, Integer soLuongTonKho) {
        this.idChiTietDonHang = idChiTietDonHang;
        this.idChiTietSanPham = idChiTietSanPham;
        this.idSanPham = idSanPham;
        this.tenSanPham = tenSanPham;
        this.mauSac = mauSac;
        this.kichCo = kichCo;
        this.gia = gia;
        this.soLuongTonKho = soLuongTonKho;
    }

    // ✅ Constructor rút gọn không cần idChiTietDonHang
    public SanPhamTonKhoThapDTO(UUID idChiTietSanPham, UUID idSanPham,
                                String tenSanPham, String mauSac, String kichCo,
                                BigDecimal gia, Integer soLuongTonKho) {
        this.idChiTietSanPham = idChiTietSanPham;
        this.idSanPham = idSanPham;
        this.tenSanPham = tenSanPham;
        this.mauSac = mauSac;
        this.kichCo = kichCo;
        this.gia = gia;
        this.soLuongTonKho = soLuongTonKho;
    }
}
