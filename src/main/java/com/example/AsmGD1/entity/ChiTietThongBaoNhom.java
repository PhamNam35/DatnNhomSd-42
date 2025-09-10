    package com.example.AsmGD1.entity;

    import jakarta.persistence.*;
    import lombok.Data;

    import java.util.UUID;

    @Data
    @Entity
    @Table(name = "chi_tiet_thong_bao_nhom")
    public class ChiTietThongBaoNhom {
        @Id
        private UUID id;

        @ManyToOne
        @JoinColumn(name = "id_thong_bao_nhom")
        private ThongBaoNhom thongBaoNhom;

        @ManyToOne
        @JoinColumn(name = "id_nguoi_dung")
        private NguoiDung nguoiDung;

        @Column(name = "da_xem")
        private boolean daXem = false;
        // Getter/Setter
    }