    package com.example.AsmGD1.entity;

    import com.fasterxml.jackson.annotation.JsonIgnore;
    import lombok.Data;
    import jakarta.persistence.*;
    import org.springframework.format.annotation.DateTimeFormat;

    import java.util.UUID;
    import java.math.BigDecimal;
    import java.time.LocalDateTime;
    import java.util.List;

    @Entity
    @Table(name = "chien_dich_giam_gia")
    @Data
    public class ChienDichGiamGia {
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private UUID id;

        @Column(name = "ten")
        private String ten;

        @Column(name = "ma", nullable = false, length = 50)
        private String ma;

        @Column(name = "hinh_thuc_giam")
        private String hinhThucGiam;

        @Column(name = "so_luong")
        private Integer soLuong;

        @Column(name = "phan_tram_giam")
        private BigDecimal phanTramGiam;

        @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        @Column(name = "ngay_bat_dau")
        private LocalDateTime ngayBatDau;

        @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        @Column(name = "ngay_ket_thuc")
        private LocalDateTime ngayKetThuc;

        @Column(name = "thoi_gian_tao")
        private LocalDateTime thoiGianTao;
        @Lob
        @Column(name = "chi_tiet_snapshot_json")
        private String chiTietSnapshotJson;

        @Column(name = "snapshot_at")
        private LocalDateTime snapshotAt;
        @OneToMany(mappedBy = "chienDichGiamGia", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        @JsonIgnore
        private List<ChiTietSanPham> chiTietSanPhams;

        public String getStatus() {
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(ngayBatDau)) {
                return "UPCOMING"; // Sắp diễn ra
            } else if (now.isAfter(ngayKetThuc)) {
                return "ENDED"; // Đã kết thúc
            } else {
                return "ONGOING"; // Đang diễn ra
            }
        }
    }
