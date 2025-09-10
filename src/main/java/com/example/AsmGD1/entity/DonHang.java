package com.example.AsmGD1.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "don_hang")
@Data
public class DonHang {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "UNIQUEIDENTIFIER")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "id_nguoi_dung", nullable = false)
    private NguoiDung nguoiDung;

    @Column(name = "ma_don_hang", nullable = false, length = 50)
    private String maDonHang;

    @Column(name = "trang_thai_thanh_toan", nullable = false)
    private Boolean trangThaiThanhToan;

    @Column(name = "phi_van_chuyen", precision = 10, scale = 2)
    private BigDecimal phiVanChuyen;

    @ManyToOne
    @JoinColumn(name = "id_phuong_thuc_thanh_toan")
    private PhuongThucThanhToan phuongThucThanhToan;

    @Column(name = "so_tien_khach_dua", precision = 10, scale = 2)
    private BigDecimal soTienKhachDua;

    @Column(name = "thoi_gian_thanh_toan")
    private LocalDateTime thoiGianThanhToan;

    @Column(name = "thoi_gian_tao", nullable = false)
    private LocalDateTime thoiGianTao;

    @Column(name = "tien_giam", precision = 10, scale = 2)
    private BigDecimal tienGiam;

    @Column(name = "tong_tien", nullable = false, precision = 10, scale = 2)
    private BigDecimal tongTien;

    @Column(name = "phuong_thuc_ban_hang", nullable = false, length = 50)
    private String phuongThucBanHang;

    @Column(name = "dia_chi_giao_hang", columnDefinition = "NVARCHAR(MAX)")
    private String diaChiGiaoHang;

    @Column(name = "ghi_chu", columnDefinition = "NVARCHAR(MAX)")
    private String ghiChu;

    @Column(name = "trang_thai", nullable = false)
    private String trangThai = "CHO_XAC_NHAN";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dia_chi")
    private DiaChiNguoiDung diaChi;

    @OneToMany(mappedBy = "donHang", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChiTietDonHang> chiTietDonHangs = new ArrayList<>();

    @OneToMany(mappedBy = "donHang", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DonHangPhieuGiamGia> dsPhieu = new ArrayList<>();
    public void addChiTietDonHang(ChiTietDonHang chiTiet) {
        chiTietDonHangs.add(chiTiet);
        chiTiet.setDonHang(this);
    }

    @Transient
    private String formattedPhiVanChuyen;

    @Transient
    private String formattedTienGiam;
}