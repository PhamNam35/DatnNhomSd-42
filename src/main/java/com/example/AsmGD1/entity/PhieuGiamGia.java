package com.example.AsmGD1.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "phieu_giam_gia")
@Data
public class PhieuGiamGia {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uniqueidentifier")
    private UUID id;

    @Column(name = "ten", nullable = false, length = 100)
    private String ten;


    @Column(name = "loai", nullable = false, length = 50)
    private String loai;

    @Column(name = "gia_tri_giam", nullable = false, precision = 10, scale = 2)
    private BigDecimal giaTriGiam;

    @Column(name = "gia_tri_giam_toi_thieu", precision = 10, scale = 2)
    private BigDecimal giaTriGiamToiThieu;

    @Column(name = "so_luong")
    private Integer soLuong;

    @Column(name = "gioi_han_su_dung")
    private Integer gioiHanSuDung;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(name = "ngay_bat_dau")
    private LocalDateTime ngayBatDau;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(name = "ngay_ket_thuc")
    private LocalDateTime ngayKetThuc;

    @Column(name = "thoi_gian_tao", nullable = false)
    private LocalDateTime thoiGianTao;

    @Column(name = "kieu_phieu", length = 20)
    private String kieuPhieu;


    @Column(name = "gia_tri_giam_toi_da", precision = 10, scale = 2)
    private BigDecimal giaTriGiamToiDa;

    @Column(name = "ma", nullable = false, length = 50)
    private String ma;

    @Column(name = "pham_vi_ap_dung", length = 20) // ORDER | SHIPPING
    private String phamViApDung;

    @PrePersist
    public void prePersist() {
        if (thoiGianTao == null) thoiGianTao = LocalDateTime.now();
        if (phamViApDung == null || phamViApDung.isBlank()) phamViApDung = "ORDER"; // default an toàn
        if (giaTriGiam == null) giaTriGiam = BigDecimal.ZERO; // tránh null pointer
    }

    // Many-to-many "thông thường" qua bảng nối (Bỏ cascade để tránh merge PTTT)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "phieu_giam_gia_phuong_thuc_thanh_toan",
            joinColumns = @JoinColumn(name = "id_phieu_giam_gia"),
            inverseJoinColumns = @JoinColumn(name = "id_phuong_thuc_thanh_toan")
    )
    private Set<PhuongThucThanhToan> phuongThucThanhToans = new HashSet<>();

    // Helpers
    public boolean isOrderScope()    { return "ORDER".equalsIgnoreCase(phamViApDung); }
    public boolean isShippingScope() { return "SHIPPING".equalsIgnoreCase(phamViApDung); }

    public String getGiaTriGiamToiThieuFormatted() {
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return giaTriGiamToiThieu != null ? formatter.format(giaTriGiamToiThieu) + " VNĐ" : "0 VNĐ";
    }
}
