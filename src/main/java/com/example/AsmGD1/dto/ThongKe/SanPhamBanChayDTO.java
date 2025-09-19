package com.example.AsmGD1.dto.ThongKe;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class SanPhamBanChayDTO {
    private UUID idChiTietSanPham;
    private UUID idSanPham;
    private String tenSanPham;
    private String mauSac;
    private String kichCo;
    private BigDecimal gia;
    private Long soLuongDaBan;
    private String imageUrl; // Gán sau trong service

    public SanPhamBanChayDTO(UUID idChiTietSanPham, UUID idSanPham,
                             String tenSanPham, String mauSac, String kichCo,
                             BigDecimal gia, Long soLuongDaBan) {
        this.idChiTietSanPham = idChiTietSanPham;
        this.idSanPham = idSanPham;
        this.tenSanPham = tenSanPham;
        this.mauSac = mauSac;
        this.kichCo = kichCo;
        this.gia = gia;
        this.soLuongDaBan = soLuongDaBan;
    }
}
