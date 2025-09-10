package com.example.AsmGD1.entity;

import com.example.AsmGD1.dto.KhachHang.ChiTietSanPhamDto;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "chi_tiet_don_hang")
@Data
public class ChiTietDonHang {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "UNIQUEIDENTIFIER")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "id_don_hang", nullable = false)
    private DonHang donHang;

    @ManyToOne
    @JoinColumn(name = "id_chi_tiet_san_pham", nullable = false)
    private ChiTietSanPham chiTietSanPham;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    @Column(name = "gia", nullable = false, precision = 10, scale = 2)
    private BigDecimal gia;

    @Column(name = "ten_san_pham", nullable = false)
    private String tenSanPham;

    @Column(name = "thanh_tien", nullable = false, precision = 10, scale = 2)
    private BigDecimal thanhTien;

    @Column(name = "ghi_chu", columnDefinition = "NVARCHAR(MAX)")
    private String ghiChu;

    @Column(name = "trang_thai_hoan_tra", nullable = false)
    private Boolean trangThaiHoanTra = false;

    @Column(name = "ly_do_tra_hang")
    private String lyDoTraHang;

    @Column(name = "trang_thai_doi_san_pham", nullable = false)
    private Boolean trangThaiDoiSanPham = false;

    @Column(name = "ly_do_doi_hang")
    private String lyDoDoiHang;

    @Transient
    private ChiTietSanPhamDto chiTietSanPhamDto;

    // Trường mới để lưu giá đã định dạng
    @Transient
    private String formattedGia;

    @Transient
    private boolean daDanhGia;

    @Transient
    private String formattedThanhTien;



}