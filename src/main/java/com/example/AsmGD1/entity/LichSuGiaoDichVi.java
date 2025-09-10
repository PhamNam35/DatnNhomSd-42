package com.example.AsmGD1.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lich_su_giao_dich_vi")
@Data
public class LichSuGiaoDichVi {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "UNIQUEIDENTIFIER")
    private UUID id;

    @Column(name = "id_vi_thanh_toan", nullable = false)
    private UUID idViThanhToan;

    @Column(name = "id_don_hang")
    private UUID idDonHang;

    @Column(name = "loai_giao_dich", nullable = false)
    private String loaiGiaoDich;

    @Column(name = "so_tien", nullable = false)
    private BigDecimal soTien;

    @Column(name = "thoi_gian_giao_dich", nullable = false)
    private LocalDateTime thoiGianGiaoDich;

    @Column(name = "mo_ta")
    private String moTa;

    @Column(name = "ma_giao_dich")
    private String maGiaoDich;


    @Column(name = "created_at")
    private LocalDateTime createdAt;
}