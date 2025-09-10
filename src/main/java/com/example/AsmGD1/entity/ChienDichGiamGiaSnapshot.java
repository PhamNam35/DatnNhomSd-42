package com.example.AsmGD1.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "cdgg_snapshot")
public class ChienDichGiamGiaSnapshot {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private ChienDichGiamGia campaign;

    @Column(nullable = false)
    private LocalDateTime capturedAt;

    // Bản chụp thông tin campaign (để xem kể cả khi campaign đổi/xóa sau này)
    @Column(length = 50)
    private String ma;

    @Column(length = 100)
    private String ten;

    @Column(precision = 5, scale = 2)
    private BigDecimal phanTramGiam;

    private LocalDateTime ngayBatDau;
    private LocalDateTime ngayKetThuc;

    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChienDichGiamGiaSnapshotDetail> details = new ArrayList<>();

    // getters/setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ChienDichGiamGia getCampaign() { return campaign; }
    public void setCampaign(ChienDichGiamGia campaign) { this.campaign = campaign; }

    public LocalDateTime getCapturedAt() { return capturedAt; }
    public void setCapturedAt(LocalDateTime capturedAt) { this.capturedAt = capturedAt; }

    public String getMa() { return ma; }
    public void setMa(String ma) { this.ma = ma; }

    public String getTen() { return ten; }
    public void setTen(String ten) { this.ten = ten; }

    public BigDecimal getPhanTramGiam() { return phanTramGiam; }
    public void setPhanTramGiam(BigDecimal phanTramGiam) { this.phanTramGiam = phanTramGiam; }

    public LocalDateTime getNgayBatDau() { return ngayBatDau; }
    public void setNgayBatDau(LocalDateTime ngayBatDau) { this.ngayBatDau = ngayBatDau; }

    public LocalDateTime getNgayKetThuc() { return ngayKetThuc; }
    public void setNgayKetThuc(LocalDateTime ngayKetThuc) { this.ngayKetThuc = ngayKetThuc; }

    public List<ChienDichGiamGiaSnapshotDetail> getDetails() { return details; }
    public void setDetails(List<ChienDichGiamGiaSnapshotDetail> details) { this.details = details; }
}
