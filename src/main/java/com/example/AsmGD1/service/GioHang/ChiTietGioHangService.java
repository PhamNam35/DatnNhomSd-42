package com.example.AsmGD1.service.GioHang;

import com.example.AsmGD1.entity.ChiTietGioHang;
import com.example.AsmGD1.entity.ChiTietSanPham;
import com.example.AsmGD1.entity.ChienDichGiamGia;
import com.example.AsmGD1.entity.GioHang;
import com.example.AsmGD1.repository.GioHang.ChiTietGioHangRepository;
import com.example.AsmGD1.repository.GioHang.GioHangRepository;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.service.GiamGia.ChienDichGiamGiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChiTietGioHangService {

    @Autowired
    private ChiTietGioHangRepository chiTietGioHangRepository;

    @Autowired
    private GioHangRepository gioHangRepository;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @Autowired
    private ChienDichGiamGiaService chienDichGiamGiaService;

    public ChiTietGioHang updateSoLuong(UUID chiTietGioHangId, Integer soLuongMoi) {
        ChiTietGioHang chiTiet = chiTietGioHangRepository.findById(chiTietGioHangId)
                .orElseThrow(() -> new RuntimeException("Chi tiết giỏ hàng không tồn tại với ID: " + chiTietGioHangId));

        ChiTietSanPham ctp = chiTiet.getChiTietSanPham();
        if (soLuongMoi > ctp.getSoLuongTonKho()) {
            throw new RuntimeException("Số lượng yêu cầu vượt quá tồn kho hiện tại: " + ctp.getSoLuongTonKho());
        }

        int soLuongCu = chiTiet.getSoLuong();
        BigDecimal giaCu = chiTiet.getGia();       // đơn giá sau giảm hiện tại
        BigDecimal giaGoc = ctp.getGia();

        // --- LẤY CAMPAIGN THEO CHI TIẾT SẢN PHẨM ---
        BigDecimal perUnitGiam = BigDecimal.ZERO;
        Optional<ChienDichGiamGia> active = chienDichGiamGiaService
                .getActiveCampaignForProductDetail(ctp.getId());
        if (active.isPresent()) {
            ChienDichGiamGia c = active.get();
            boolean unlimited = (c.getSoLuong() == null);
            boolean enough    = unlimited || c.getSoLuong() >= soLuongMoi;

            if (c.getPhanTramGiam() != null && enough) {
                perUnitGiam = giaGoc.multiply(c.getPhanTramGiam())
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                int delta = soLuongMoi - soLuongCu;
                if (!unlimited && delta > 0) {
                    chienDichGiamGiaService.truSoLuong(c.getId(), delta);
                }
            }
        }

        BigDecimal giaSauGiam   = giaGoc.subtract(perUnitGiam);
        BigDecimal thanhTienCu  = giaCu.multiply(BigDecimal.valueOf(soLuongCu));
        BigDecimal thanhTienMoi = giaSauGiam.multiply(BigDecimal.valueOf(soLuongMoi));

        chiTiet.setSoLuong(soLuongMoi);
        chiTiet.setGia(giaSauGiam);
        chiTiet.setTienGiam(perUnitGiam.multiply(BigDecimal.valueOf(soLuongMoi)));
        chiTietGioHangRepository.save(chiTiet);

        GioHang gioHang = gioHangRepository.findById(chiTiet.getGioHang().getId())
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại"));
        gioHang.setTongTien(gioHang.getTongTien().subtract(thanhTienCu).add(thanhTienMoi));
        gioHangRepository.save(gioHang);

        return chiTiet;
    }

    public ChiTietGioHang applyDiscount(UUID chiTietGioHangId, BigDecimal tienGiam) {
        ChiTietGioHang chiTiet = chiTietGioHangRepository.findById(chiTietGioHangId)
                .orElseThrow(() -> new RuntimeException("Chi tiết giỏ hàng không tồn tại với ID: " + chiTietGioHangId));

        BigDecimal maxGiam = chiTiet.getGia().multiply(BigDecimal.valueOf(chiTiet.getSoLuong()));
        if (tienGiam.compareTo(maxGiam) > 0) {
            throw new RuntimeException("Số tiền giảm giá không được vượt quá: " + maxGiam);
        }

        chiTiet.setTienGiam(tienGiam);
        chiTiet = chiTietGioHangRepository.save(chiTiet);

        GioHang gioHang = gioHangRepository.findById(chiTiet.getGioHang().getId())
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại"));
        BigDecimal giaMoi = chiTiet.getGia().multiply(BigDecimal.valueOf(chiTiet.getSoLuong())).subtract(tienGiam);
        BigDecimal giaCu = chiTiet.getGia().multiply(BigDecimal.valueOf(chiTiet.getSoLuong())).subtract(chiTiet.getTienGiam().add(tienGiam.negate()));
        gioHang.setTongTien(gioHang.getTongTien().add(giaMoi).subtract(giaCu));
        gioHangRepository.save(gioHang);

        return chiTiet;
    }

    public void removeChiTietGioHang(UUID gioHangId, UUID chiTietSanPhamId) {
        ChiTietGioHang chiTiet = chiTietGioHangRepository.findByGioHangIdAndChiTietSanPhamId(gioHangId, chiTietSanPhamId)
                .orElseThrow(() -> new RuntimeException("Chi tiết giỏ hàng không tồn tại"));
        GioHang gioHang = gioHangRepository.findById(gioHangId)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại"));

        // Cập nhật tổng tiền giỏ hàng
        BigDecimal lineTotal = chiTiet.getGia().multiply(BigDecimal.valueOf(chiTiet.getSoLuong()));
        gioHang.setTongTien(gioHang.getTongTien().subtract(lineTotal));
        gioHangRepository.save(gioHang);

        // Xóa chi tiết giỏ hàng
        chiTietGioHangRepository.delete(chiTiet);
    }

    public Optional<ChiTietGioHang> getChiTietGioHang(UUID chiTietGioHangId) {
        return chiTietGioHangRepository.findById(chiTietGioHangId);
    }

    // ✅ THÊM MỚI: Lấy danh sách chi tiết giỏ hàng theo ID giỏ hàng
    public List<ChiTietGioHang> getGioHangChiTietList(UUID gioHangId) {
        return chiTietGioHangRepository.findByGioHangId(gioHangId);
    }

    // ✅ THÊM MỚI: Xóa toàn bộ chi tiết giỏ hàng theo ID giỏ hàng
    @Transactional
    public void clearGioHang(UUID gioHangId) {
        // Xóa tất cả chi tiết giỏ hàng
        chiTietGioHangRepository.deleteByGioHang_Id(gioHangId);

        // Cập nhật tổng tiền của giỏ hàng về 0
        GioHang gioHang = gioHangRepository.findById(gioHangId)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại"));
        gioHang.setTongTien(BigDecimal.ZERO);
        gioHangRepository.save(gioHang);
    }
}
