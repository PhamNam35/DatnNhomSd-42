package com.example.AsmGD1.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "cdgg_snapshot_detail")
public class ChienDichGiamGiaSnapshotDetail {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id")
    private ChienDichGiamGiaSnapshot snapshot;

    // Lưu thông tin chi tiết ngay thời điểm chụp
    private UUID sanPhamId;
    private UUID chiTietId;

    @Column(length = 50)
    private String maSanPham;

    @Column(length = 200)
    private String tenSanPham;

    @Column(length = 100)
    private String mau;

    @Column(length = 100)
    private String size;

    @Column(precision = 18, scale = 2)
    private BigDecimal gia;

    private Integer ton;

    // getters/setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ChienDichGiamGiaSnapshot getSnapshot() { return snapshot; }
    public void setSnapshot(ChienDichGiamGiaSnapshot snapshot) { this.snapshot = snapshot; }

    public UUID getSanPhamId() { return sanPhamId; }
    public void setSanPhamId(UUID sanPhamId) { this.sanPhamId = sanPhamId; }

    public UUID getChiTietId() { return chiTietId; }
    public void setChiTietId(UUID chiTietId) { this.chiTietId = chiTietId; }

    public String getMaSanPham() { return maSanPham; }
    public void setMaSanPham(String maSanPham) { this.maSanPham = maSanPham; }

    public String getTenSanPham() { return tenSanPham; }
    public void setTenSanPham(String tenSanPham) { this.tenSanPham = tenSanPham; }

    public String getMau() { return mau; }
    public void setMau(String mau) { this.mau = mau; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public BigDecimal getGia() { return gia; }
    public void setGia(BigDecimal gia) { this.gia = gia; }

    public Integer getTon() { return ton; }
    public void setTon(Integer ton) { this.ton = ton; }
}
