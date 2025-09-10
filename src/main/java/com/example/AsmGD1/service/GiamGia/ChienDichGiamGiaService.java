package com.example.AsmGD1.service.GiamGia;

import com.example.AsmGD1.dto.GiamGia.SnapshotDetailItem;
import com.example.AsmGD1.entity.ChienDichGiamGia;
import com.example.AsmGD1.entity.ChiTietSanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChienDichGiamGiaService {

    void taoMoiChienDichKemChiTiet(ChienDichGiamGia chienDich, List<UUID> danhSachChiTietSanPham);

    void capNhatChienDichKemChiTiet(ChienDichGiamGia chienDich, List<UUID> danhSachChiTietSanPham);

    Page<ChienDichGiamGia> locChienDich(String keyword, LocalDateTime startDate, LocalDateTime endDate,
                                        String status, String discountLevel, Pageable pageable);

    List<ChiTietSanPham> layChiTietTheoSanPham(UUID idSanPham);

    List<ChiTietSanPham> layChiTietDaChonTheoChienDich(UUID idChienDich);

    Optional<ChienDichGiamGia> timTheoId(UUID id);

    void xoaChienDich(UUID id);

    boolean maDaTonTai(String ma, UUID excludeId);

    boolean kiemTraMaTonTai(String ma);

    boolean kiemTraTenTonTai(String ten);

    ChiTietSanPham layChiTietTheoId(UUID id);

    void truSoLuong(UUID idChienDich, int soLuongTru);

    Optional<ChienDichGiamGia> getActiveCampaignForProduct(UUID sanPhamId);

    Optional<ChienDichGiamGia> getActiveCampaignForProductDetail(UUID chiTietSanPhamId);

    // NEW
    List<ChiTietSanPham> layChiTietConTrongTheoSanPham(List<UUID> productIds, UUID excludeCampaignId);

    // Snapshot API mới
    void refreshSnapshot(UUID campaignId);

    List<SnapshotDetailItem> getSnapshot(UUID campaignId);
}