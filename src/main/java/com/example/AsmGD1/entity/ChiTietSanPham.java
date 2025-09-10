package com.example.AsmGD1.entity;

import com.example.AsmGD1.dto.KhachHang.ChiTietSanPhamDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chi_tiet_san_pham")
@Data
public class ChiTietSanPham {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "UNIQUEIDENTIFIER")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "id_san_pham", nullable = false)
    private SanPham sanPham;

    @ManyToOne
    @JoinColumn(name = "id_kich_co", nullable = false)
    private KichCo kichCo;

    @ManyToOne
    @JoinColumn(name = "id_mau_sac", nullable = false)
    private MauSac mauSac;

    @ManyToOne
    @JoinColumn(name = "id_chat_lieu", nullable = false)
    private ChatLieu chatLieu;

    @ManyToOne
    @JoinColumn(name = "id_xuat_xu", nullable = false)
    private XuatXu xuatXu;

    @ManyToOne
    @JoinColumn(name = "id_tay_ao", nullable = false)
    private TayAo tayAo;

    @ManyToOne
    @JoinColumn(name = "id_co_ao", nullable = false)
    private CoAo coAo;

    @ManyToOne
    @JoinColumn(name = "id_kieu_dang", nullable = false)
    private KieuDang kieuDang;

    @ManyToOne
    @JoinColumn(name = "id_thuong_hieu", nullable = false)
    private ThuongHieu thuongHieu;

    @ManyToOne
    @JoinColumn(name = "id_chien_dich_giam_gia") // Thêm khóa ngoại đến chien_dich_giam_gia
    @JsonIgnore
    private ChienDichGiamGia chienDichGiamGia; // Mối quan hệ một-nhiều

    @Column(name = "gia", nullable = false, precision = 10, scale = 2)
    private BigDecimal gia;

    @Column(name = "so_luong_ton_kho", nullable = false)
    private Integer soLuongTonKho;

    @Column(name = "gioi_tinh", length = 50)
    private String gioiTinh;

    @Column(name = "thoi_gian_tao", nullable = false)
    private LocalDateTime thoiGianTao;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai;

    @OneToMany(mappedBy = "chiTietSanPham", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<HinhAnhSanPham> hinhAnhSanPhams;

    @Transient
    private ChiTietSanPhamDto chiTietSanPhamDto;
}