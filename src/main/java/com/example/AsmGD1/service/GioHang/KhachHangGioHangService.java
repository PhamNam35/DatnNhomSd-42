package com.example.AsmGD1.service.GioHang;

import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.GioHang.GioHangRepository;
import com.example.AsmGD1.repository.GioHang.ChiTietGioHangRepository;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.service.GiamGia.ChienDichGiamGiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service("webKhachHangGioHangService")
public class KhachHangGioHangService {

    @Autowired
    private GioHangRepository gioHangRepository;

    @Autowired
    private ChiTietGioHangRepository chiTietGioHangRepository;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private ChienDichGiamGiaService chienDichGiamGiaService;

    public GioHang getOrCreateGioHang(UUID nguoiDungId) {
        GioHang gioHang = gioHangRepository.findByNguoiDungId(nguoiDungId);
        if (gioHang == null) {
            gioHang = new GioHang();
//            gioHang.setId(UUID.randomUUID());
            gioHang.setMaGioHang("GH" + UUID.randomUUID().toString().substring(0, 8));
            gioHang.setTongTien(BigDecimal.ZERO);
            gioHang.setThoiGianTao(LocalDateTime.now());
            gioHang.setTrangThai(true);

            // Thay vì tạo đối tượng giả
            NguoiDung nguoiDung = nguoiDungRepository.findById(nguoiDungId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
            gioHang.setNguoiDung(nguoiDung);

            gioHang = gioHangRepository.save(gioHang);
        }
        return gioHang;
    }


    @Transactional
    public ChiTietGioHang addToGioHang(UUID gioHangId, UUID chiTietSanPhamId, Integer soLuong) {
        if (soLuong == null || soLuong < 1) {
            throw new IllegalArgumentException("Số lượng phải >= 1");
        }

        // 1) Lấy giỏ hàng & chi tiết sản phẩm
        GioHang gioHang = gioHangRepository.findById(gioHangId)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại với ID: " + gioHangId));

        ChiTietSanPham chiTietSanPham = chiTietSanPhamRepository.findById(chiTietSanPhamId)
                .orElseThrow(() -> new RuntimeException("Chi tiết sản phẩm không tồn tại với ID: " + chiTietSanPhamId));

        if (chiTietSanPham.getSoLuongTonKho() < soLuong) {
            throw new RuntimeException("Số lượng tồn kho không đủ: " + chiTietSanPham.getSoLuongTonKho());
        }

        // 2) Tính giảm giá theo CHI TIẾT SẢN PHẨM (quota null = không giới hạn)
        BigDecimal giaGoc = chiTietSanPham.getGia();
        BigDecimal perUnitGiam = BigDecimal.ZERO;

        Optional<ChienDichGiamGia> active = chienDichGiamGiaService
                .getActiveCampaignForProductDetail(chiTietSanPhamId);

        if (active.isPresent()) {
            ChienDichGiamGia c = active.get();
            boolean unlimited = (c.getSoLuong() == null);
            boolean enough    = unlimited || c.getSoLuong() >= soLuong;

            if (c.getPhanTramGiam() != null && enough) {
                perUnitGiam = giaGoc.multiply(c.getPhanTramGiam())
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP); // làm tròn VND
                if (!unlimited) {
                    // chỉ trừ quota khi có giới hạn
                    chienDichGiamGiaService.truSoLuong(c.getId(), soLuong);
                }
            }
        }

        BigDecimal giaSauGiam = giaGoc.subtract(perUnitGiam);

        // 3) Nếu dòng đã tồn tại trong giỏ: cộng dồn số lượng & tính lại đơn giá theo campaign hiện tại
        Optional<ChiTietGioHang> existingOpt = chiTietGioHangRepository
                .findByGioHangIdAndChiTietSanPhamId(gioHangId, chiTietSanPhamId);

        if (existingOpt.isPresent()) {
            ChiTietGioHang existing = existingOpt.get();

            int oldQty = existing.getSoLuong();
            int newQty = oldQty + soLuong;

            if (chiTietSanPham.getSoLuongTonKho() < newQty) {
                throw new RuntimeException("Số lượng tồn kho không đủ sau khi cộng dồn: " + chiTietSanPham.getSoLuongTonKho());
            }

            // tổng cũ & mới (lưu ý: existing.getGia() là đơn giá sau giảm trước đó)
            BigDecimal oldLine = existing.getGia().multiply(BigDecimal.valueOf(oldQty));
            BigDecimal newLine = giaSauGiam.multiply(BigDecimal.valueOf(newQty));

            existing.setSoLuong(newQty);
            existing.setGia(giaSauGiam); // đơn giá sau giảm hiện tại
            existing.setTienGiam(perUnitGiam.multiply(BigDecimal.valueOf(newQty)));
            chiTietGioHangRepository.save(existing);

            gioHang.setTongTien(gioHang.getTongTien().subtract(oldLine).add(newLine));
            gioHangRepository.save(gioHang);

            return existing;
        }

        // 4) Tạo mới dòng chi tiết giỏ hàng
        ChiTietGioHang chiTiet = new ChiTietGioHang();
        chiTiet.setGioHang(gioHang);
        chiTiet.setChiTietSanPham(chiTietSanPham);
        chiTiet.setSoLuong(soLuong);
        chiTiet.setGia(giaSauGiam); // lưu đơn giá sau giảm
        chiTiet.setTienGiam(perUnitGiam.multiply(BigDecimal.valueOf(soLuong))); // tổng tiền giảm của dòng
        chiTiet.setThoiGianThem(LocalDateTime.now());
        chiTiet.setTrangThai(true);

        ChiTietGioHang saved = chiTietGioHangRepository.save(chiTiet);

        BigDecimal lineTotal = giaSauGiam.multiply(BigDecimal.valueOf(soLuong));
        gioHang.setTongTien(gioHang.getTongTien().add(lineTotal));
        gioHangRepository.save(gioHang);

        return saved;
    }

    public List<ChiTietGioHang> getGioHangChiTiets(UUID gioHangId) {
        List<ChiTietGioHang> chiTiets = chiTietGioHangRepository.findByGioHangIdWithHinhAnh(gioHangId);
        return chiTiets != null ? chiTiets : java.util.Collections.emptyList();
    }
}