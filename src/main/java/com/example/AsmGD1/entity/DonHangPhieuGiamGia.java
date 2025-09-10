package com.example.AsmGD1.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "don_hang_phieu_giam_gia")
@Data
public class DonHangPhieuGiamGia {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "UNIQUEIDENTIFIER")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "id_don_hang", nullable = false)
    private DonHang donHang;

    @ManyToOne
    @JoinColumn(name = "id_phieu_giam_gia", nullable = false)
    private PhieuGiamGia phieuGiamGia;

    @Column(name = "loai_giam_gia", nullable = false, length = 50)
    private String loaiGiamGia; // ORDER hoặc SHIPPING

    @Column(name = "gia_tri_giam", nullable = false, precision = 10, scale = 2)
    private BigDecimal giaTriGiam;

    @Column(name = "thoi_gian_ap_dung", nullable = false)
    private LocalDateTime thoiGianApDung;
}