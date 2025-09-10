package com.example.AsmGD1.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "yeu_cau_rut_tien")
public class YeuCauRutTien {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(name = "ma_giao_dich", unique = true, nullable = false)
    private String maGiaoDich;

    @ManyToOne
    @JoinColumn(name = "id_vi_thanh_toan", nullable = false)
    private ViThanhToan viThanhToan;

    @Column(name = "so_tien", nullable = false)
    private BigDecimal soTien;

    @Column(name = "trang_thai", nullable = false)
    private String trangThai;

    @Column(name = "ghi_chu")
    private String ghiChu;

    @Column(name = "thoi_gian_yeu_cau", nullable = false)
    private LocalDateTime thoiGianYeuCau;

    @Column(name = "thoi_gian_xu_ly")
    private LocalDateTime thoiGianXuLy;

    @Column(name = "so_tai_khoan")
    private String soTaiKhoan;

    @Column(name = "nguoi_thu_huong")
    private String nguoiThuHuong;

    @Column(name = "ten_ngan_hang")
    private String tenNganHang;

    @Column(name = "anh_bang_chung")
    private String anhBangChung;

    @PrePersist
    public void prePersist() {
        if (thoiGianYeuCau == null) {
            thoiGianYeuCau = LocalDateTime.now();
        }
        if (maGiaoDich == null) {
            this.maGiaoDich = "RT" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        this.trangThai = "Đang chờ";
    }
}
