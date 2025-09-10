package com.example.AsmGD1.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Entity
@Table(name = "hinh_anh_san_pham")
@Data
public class HinhAnhSanPham {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "UNIQUEIDENTIFIER")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "id_chi_tiet_san_pham", nullable = false)
    private ChiTietSanPham chiTietSanPham;

    @Column(name = "url_hinh_anh", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String urlHinhAnh;

    @Column(name = "thu_tu")
    private Integer thuTu;
}