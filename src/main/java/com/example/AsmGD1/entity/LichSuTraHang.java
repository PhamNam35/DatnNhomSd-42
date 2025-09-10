package com.example.AsmGD1.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lich_su_tra_hang")
@Data
public class LichSuTraHang {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "id_chi_tiet_don_hang")
    private ChiTietDonHang chiTietDonHang;

    @ManyToOne
    @JoinColumn(name = "id_hoa_don")
    private HoaDon hoaDon;

    @Column(name = "so_luong")
    private Integer soLuong;

    @Column(name = "tong_tien_hoan")
    private BigDecimal tongTienHoan;

    @Column(name = "ly_do_tra_hang")
    private String lyDoTraHang;

    @Column(name = "thoi_gian_tra")
    private LocalDateTime thoiGianTra;

    @Column(name = "trang_thai")
    private String trangThai;

    @Column(name = "thoi_gian_xac_nhan")
    private LocalDateTime thoiGianXacNhan; // Thêm trường này
}