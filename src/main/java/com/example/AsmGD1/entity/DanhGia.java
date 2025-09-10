package com.example.AsmGD1.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "danh_gia")
public class DanhGia {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "UNIQUEIDENTIFIER")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "id_hoa_don", nullable = false)
    private HoaDon hoaDon;

    @ManyToOne
    @JoinColumn(name = "id_chi_tiet_san_pham", nullable = false)
    private ChiTietSanPham chiTietSanPham;

    @ManyToOne
    @JoinColumn(name = "id_nguoi_dung", nullable = false)
    private NguoiDung nguoiDung;

    @Column(name = "xep_hang", nullable = false)
    private Integer xepHang;

    @Column(name = "noi_dung", columnDefinition = "NVARCHAR(MAX)")
    private String noiDung;

    @Column(name = "url_hinh_anh", columnDefinition = "NVARCHAR(MAX)")
    private String urlHinhAnh;

    @Column(name = "thoi_gian_danh_gia", nullable = false)
    private LocalDateTime thoiGianDanhGia;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai;
}