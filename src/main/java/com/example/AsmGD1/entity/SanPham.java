package com.example.AsmGD1.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "san_pham")
@Data
public class SanPham {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "UNIQUEIDENTIFIER")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "id_danh_muc", nullable = false)
    private DanhMuc danhMuc;

    @Column(name = "ma_san_pham", nullable = false, length = 50, unique = true)
    private String maSanPham;

    @Column(name = "ten_san_pham", nullable = false, length = 100)
    private String tenSanPham;

    @Column(name = "mo_ta", columnDefinition = "NVARCHAR(MAX)")
    private String moTa;

    @Column(name = "url_hinh_anh", columnDefinition = "NVARCHAR(MAX)")
    private String urlHinhAnh;

    @Column(name = "thoi_gian_tao", nullable = false)
    private LocalDateTime thoiGianTao;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai;

    @Transient
    private long tongSoLuong;

    //long
    @OneToMany(mappedBy = "sanPham", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ChiTietSanPham> chiTietSanPhams;

    // Helper methods for frontend
    public int getTotalStockQuantity() {
        return chiTietSanPhams != null ? chiTietSanPhams.stream()
                .mapToInt(ChiTietSanPham::getSoLuongTonKho)
                .sum() : 0;
    }

    public BigDecimal getMinPrice() {
        return chiTietSanPhams != null && !chiTietSanPhams.isEmpty() ? chiTietSanPhams.stream()
                .map(ChiTietSanPham::getGia)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO) : BigDecimal.ZERO;
    }

    public BigDecimal getMaxPrice() {
        return chiTietSanPhams != null && !chiTietSanPhams.isEmpty() ? chiTietSanPhams.stream()
                .map(ChiTietSanPham::getGia)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO) : BigDecimal.ZERO;
    }

    public String getMinPriceFormatted() {
        return String.format("%,.0f VNĐ", getMinPrice().doubleValue());
    }

    public String getMaxPriceFormatted() {
        return String.format("%,.0f VNĐ", getMaxPrice().doubleValue());
    }
}