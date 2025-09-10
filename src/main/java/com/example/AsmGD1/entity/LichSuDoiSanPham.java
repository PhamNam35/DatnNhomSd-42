package com.example.AsmGD1.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lich_su_doi_san_pham")
@Getter
@Setter
public class LichSuDoiSanPham {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "id_chi_tiet_don_hang")
    private ChiTietDonHang chiTietDonHang;

    @ManyToOne
    @JoinColumn(name = "id_hoa_don")
    private HoaDon hoaDon;

    @ManyToOne
    @JoinColumn(name = "id_chi_tiet_san_pham_thay_the")
    private ChiTietSanPham chiTietSanPhamThayThe;

    private int soLuong;
    private BigDecimal tongTienHoan;
    private String lyDoDoiHang;
    private LocalDateTime thoiGianDoi;
    private String trangThai;
    @Column(name = "chenh_lech_gia")
    private BigDecimal chenhLechGia; // Thêm trường này

    @ManyToOne
    @JoinColumn(name = "id_phuong_thuc_thanh_toan")
    private PhuongThucThanhToan phuongThucThanhToan;

    @Column(name = "da_thanh_toan_chenh_lech")
    private Boolean daThanhToanChenhLech = false;

}