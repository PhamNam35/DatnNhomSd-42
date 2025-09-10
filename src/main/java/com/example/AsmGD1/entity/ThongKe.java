package com.example.AsmGD1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "thong_ke_doanh_thu_chi_tiet")
@Data
public class ThongKe {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "ngay_thanh_toan")
    private LocalDate ngayThanhToan;

    @Column(name = "id_chi_tiet_don_hang")
    private UUID idChiTietDonHang;

    @Column(name = "id_chi_tiet_san_pham")
    private UUID idChiTietSanPham;

    @Column(name = "id_san_pham")
    private UUID idSanPham;

    @Column(name = "ten_san_pham")
    private String tenSanPham;

    @Column(name = "kich_co")
    private String kichCo;

    @Column(name = "mau_sac")
    private String mauSac;

    @Column(name = "so_luong_da_ban")
    private int soLuongDaBan;

    @Column(name = "doanh_thu")
    private BigDecimal doanhThu;

    @Column(name = "so_luong_ton_kho")
    private int soLuongTonKho;

    @Column(name = "image_url")
    private String imageUrl;
}
