package com.example.AsmGD1.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "thong_bao_nhom")
public class ThongBaoNhom {
    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "id_don_hang", nullable = true)
    private DonHang donHang;

    @Column(name = "vai_tro_nhan")
    private String vaiTroNhan;

    @Column(name = "tieu_de")
    private String tieuDe;

    @Column(name = "noi_dung")
    private String noiDung;

    @Column(name = "thoi_gian_tao")
    private LocalDateTime thoiGianTao;

    @Column(name = "trang_thai")
    private String trangThai;
    // Getter/Setter
}
