package com.example.AsmGD1.service.ThongBao;

import com.example.AsmGD1.entity.ChiTietThongBaoNhom;
import com.example.AsmGD1.entity.DonHang;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.ThongBaoNhom;
import com.example.AsmGD1.repository.BanHang.DonHangRepository;
import com.example.AsmGD1.repository.HoaDon.HoaDonRepository;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.repository.ThongBao.ChiTietThongBaoNhomRepository;
import com.example.AsmGD1.repository.ThongBao.ThongBaoNhomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ThongBaoService {
    private static final Logger log = LoggerFactory.getLogger(ThongBaoService.class);

    @Autowired
    private ThongBaoNhomRepository thongBaoNhomRepository;

    @Autowired
    private ChiTietThongBaoNhomRepository chiTietThongBaoNhomRepository;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;
    @Autowired
    private DonHangRepository donHangRepository;
    private List<String> parseRoles(String input) {
        if (input == null) return List.of();
        return Arrays.stream(input.split("[,;]"))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
    }

    private String pickHeaderRole(List<String> roles) {
        if (roles.contains("admin")) return "admin";
        if (roles.contains("employee")) return "employee";
        return "admin"; // fallback an toàn
    }

    private java.util.Map<UUID, NguoiDung> loadRecipients(List<String> roles) {
        java.util.Map<UUID, NguoiDung> map = new java.util.LinkedHashMap<>();
        for (String r : roles) {
            for (NguoiDung u : nguoiDungRepository.findByVaiTroAndTrangThai(r, true)) {
                map.put(u.getId(), u);
            }
        }
        return map;
    }

    public void taoThongBaoHeThong(String vaiTroNhan, String tieuDe, String noiDung) {
        List<String> roles = parseRoles(vaiTroNhan);
        if (roles.isEmpty()) roles = List.of("admin");
        String headerRole = pickHeaderRole(roles);

        ThongBaoNhom thongBao = new ThongBaoNhom();
        thongBao.setId(UUID.randomUUID());
        thongBao.setDonHang(null);
        thongBao.setVaiTroNhan(headerRole); // ✅ chỉ 1 giá trị hợp lệ với CHECK
        thongBao.setTieuDe(tieuDe);
        thongBao.setNoiDung(noiDung);
        thongBao.setThoiGianTao(LocalDateTime.now());
        thongBao.setTrangThai("Mới");
        thongBaoNhomRepository.save(thongBao);

        var recipients = loadRecipients(roles); // admin + employee
        for (NguoiDung nd : recipients.values()) {
            ChiTietThongBaoNhom ct = new ChiTietThongBaoNhom();
            ct.setId(UUID.randomUUID());
            ct.setThongBaoNhom(thongBao);
            ct.setNguoiDung(nd);
            ct.setDaXem(false);
            chiTietThongBaoNhomRepository.save(ct);

            long unread = chiTietThongBaoNhomRepository.countByNguoiDungIdAndDaXemFalse(nd.getId());
            com.example.AsmGD1.controller.ThongBao.ThongBaoController.pushUnread(nd.getId(), unread);
        }
    }


    public void taoThongBaoHeThong(String vaiTroNhan, String tieuDe, String noiDung, DonHang donHang) {
        log.info("📌 [THÔNG BÁO] BẮT ĐẦU tạo thông báo hệ thống");

        List<String> roles = parseRoles(vaiTroNhan);
        if (roles.isEmpty()) roles = List.of("admin");
        String headerRole = pickHeaderRole(roles);

        ThongBaoNhom thongBao = new ThongBaoNhom();
        thongBao.setId(UUID.randomUUID());
        thongBao.setDonHang(donHang);
        thongBao.setVaiTroNhan(headerRole); // ✅ chỉ 1 giá trị hợp lệ với CHECK
        thongBao.setTieuDe(tieuDe);
        thongBao.setNoiDung(noiDung);
        thongBao.setThoiGianTao(LocalDateTime.now());
        thongBao.setTrangThai("Mới");

        try {
            thongBaoNhomRepository.save(thongBao);
            thongBaoNhomRepository.flush();
            log.info("✅ [THÔNG BÁO] Lưu thong_bao_nhom thành công (vai_tro_nhan={})", headerRole);
        } catch (Exception e) {
            log.error("❌ [THÔNG BÁO] Lỗi khi lưu thong_bao_nhom: {}", e.getMessage(), e);
            return;
        }

        var recipients = loadRecipients(roles); // admin + employee
        log.info(">>> [THÔNG BÁO] Gửi đến {} người thuộc roles: {}", recipients.size(), roles);

        for (NguoiDung nd : recipients.values()) {
            try {
                ChiTietThongBaoNhom chiTiet = new ChiTietThongBaoNhom();
                chiTiet.setId(UUID.randomUUID());
                chiTiet.setNguoiDung(nd);
                chiTiet.setThongBaoNhom(thongBao);
                chiTiet.setDaXem(false);
                chiTietThongBaoNhomRepository.save(chiTiet);

                long unread = chiTietThongBaoNhomRepository.countByNguoiDungIdAndDaXemFalse(nd.getId());
                com.example.AsmGD1.controller.ThongBao.ThongBaoController.pushUnread(nd.getId(), unread);
                log.info("✅ Gửi thông báo cho: {}", nd.getEmail());
            } catch (Exception ex) {
                log.error("❌ Lỗi khi gửi thông báo cho {}: {}", nd.getEmail(), ex.getMessage(), ex);
            }
        }

        log.info("📨 Đã xong tạo thông báo hệ thống cho đơn hàng: {}", donHang.getMaDonHang());
    }


    public List<ChiTietThongBaoNhom> layThongBaoTheoNguoiDungVaTrangThai(UUID idNguoiDung, int page, int size, String status) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("thongBaoNhom.thoiGianTao").descending());
            if ("unread".equalsIgnoreCase(status)) {
                return chiTietThongBaoNhomRepository.findByNguoiDungIdAndDaXemFalse(idNguoiDung, pageable).getContent();
            } else if ("read".equalsIgnoreCase(status)) {
                return chiTietThongBaoNhomRepository.findByNguoiDungIdAndDaXemTrue(idNguoiDung, pageable).getContent();
            } else {
                return chiTietThongBaoNhomRepository.findByNguoiDungId(idNguoiDung, pageable).getContent();
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy thông báo theo trạng thái: " + e.getMessage());
            return List.of();
        }
    }

    public long demSoThongBaoChuaXem(UUID idNguoiDung) {
        try {
            return chiTietThongBaoNhomRepository.countByNguoiDungIdAndDaXemFalse(idNguoiDung);
        } catch (Exception e) {
            System.err.println("Lỗi khi đếm thông báo chưa xem: " + e.getMessage());
            return 0;
        }
    }

    public long demTongSoThongBao(UUID idNguoiDung) {
        try {
            return chiTietThongBaoNhomRepository.countByNguoiDungId(idNguoiDung);
        } catch (Exception e) {
            System.err.println("Lỗi khi đếm tổng số thông báo: " + e.getMessage());
            return 0;
        }
    }

    public long demTongSoThongBaoTheoTrangThai(UUID idNguoiDung, String status) {
        try {
            if ("unread".equalsIgnoreCase(status)) {
                return chiTietThongBaoNhomRepository.countByNguoiDungIdAndDaXemFalse(idNguoiDung);
            } else if ("read".equalsIgnoreCase(status)) {
                return chiTietThongBaoNhomRepository.countByNguoiDungIdAndDaXemTrue(idNguoiDung);
            } else {
                return chiTietThongBaoNhomRepository.countByNguoiDungId(idNguoiDung);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi đếm thông báo theo trạng thái: " + e.getMessage());
            return 0;
        }
    }

    public void danhDauDaXem(UUID idChiTietThongBao, UUID idNguoiDung) {
        Optional<ChiTietThongBaoNhom> optionalThongBao = chiTietThongBaoNhomRepository.findById(idChiTietThongBao);
        if (optionalThongBao.isPresent()) {
            ChiTietThongBaoNhom thongBao = optionalThongBao.get();
            if (!thongBao.getNguoiDung().getId().equals(idNguoiDung)) {
                throw new IllegalArgumentException("Thông báo không thuộc về người dùng này.");
            }
            thongBao.setDaXem(true);
            chiTietThongBaoNhomRepository.save(thongBao);
        } else {
            throw new IllegalArgumentException("Không tìm thấy thông báo.");
        }
    }

    public void danhDauTatCaDaXem(UUID idNguoiDung) {
        List<ChiTietThongBaoNhom> danhSach = chiTietThongBaoNhomRepository
                .findByNguoiDungIdAndDaXemFalseOrderByThongBaoNhom_ThoiGianTaoDesc(idNguoiDung);
        for (ChiTietThongBaoNhom tb : danhSach) {
            tb.setDaXem(true);
        }
        chiTietThongBaoNhomRepository.saveAll(danhSach);
    }

    public List<ChiTietThongBaoNhom> lay5ThongBaoMoiNhat(UUID idNguoiDung) {
        try {
            Pageable top5 = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "thongBaoNhom.thoiGianTao"));
            return chiTietThongBaoNhomRepository.findByNguoiDungId(idNguoiDung, top5).getContent();
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy 5 thông báo mới nhất: " + e.getMessage());
            return List.of();
        }
    }

    public List<ChiTietThongBaoNhom> lay5ThongBaoChuaXem(UUID idNguoiDung) {
        try {
            return chiTietThongBaoNhomRepository
                    .findTop5ByNguoiDungIdAndDaXemFalseOrderByThongBaoNhom_ThoiGianTaoDesc(idNguoiDung);
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy 5 thông báo chưa đọc: " + e.getMessage());
            return List.of();
        }
    }

    public void danhDauChuaXem(UUID idChiTietThongBao, UUID idNguoiDung) {
        Optional<ChiTietThongBaoNhom> optionalThongBao = chiTietThongBaoNhomRepository.findById(idChiTietThongBao);
        if (optionalThongBao.isPresent()) {
            ChiTietThongBaoNhom thongBao = optionalThongBao.get();
            if (!thongBao.getNguoiDung().getId().equals(idNguoiDung)) {
                throw new IllegalArgumentException("Thông báo không thuộc về người dùng này.");
            }
            thongBao.setDaXem(false);
            chiTietThongBaoNhomRepository.save(thongBao);
        } else {
            throw new IllegalArgumentException("Không tìm thấy thông báo.");
        }
    }

    public void xoaThongBao(UUID idThongBao, UUID idNguoiDung) {
        Optional<ChiTietThongBaoNhom> optional = chiTietThongBaoNhomRepository.findById(idThongBao);
        if (optional.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy thông báo.");
        }
        ChiTietThongBaoNhom thongBao = optional.get();
        if (!thongBao.getNguoiDung().getId().equals(idNguoiDung)) {
            throw new IllegalArgumentException("Thông báo không thuộc về người dùng này.");
        }
        chiTietThongBaoNhomRepository.delete(thongBao);
    }

    @Transactional
    public void xoaTatCaThongBao(UUID idNguoiDung) {
        chiTietThongBaoNhomRepository.deleteByNguoiDungId(idNguoiDung);
    }
    @Transactional
    public UUID thongBaoCapNhatTrangThai(UUID donHangId, String tieuDe, String noiDung) {
        DonHang dh = donHangRepository.findById(donHangId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        NguoiDung kh = dh.getNguoiDung();
        if (kh == null) throw new IllegalStateException("Đơn hàng không gắn khách hàng");

        // ThongBaoNhom (lưu ý: entity của bạn chưa @GeneratedValue => setId thủ công)
        ThongBaoNhom tbn = new ThongBaoNhom();
        tbn.setId(UUID.randomUUID());
        tbn.setDonHang(dh);
        tbn.setVaiTroNhan("customer");
        tbn.setTieuDe(tieuDe);
        tbn.setNoiDung(noiDung);
        tbn.setTrangThai("ACTIVE");
        tbn.setThoiGianTao(LocalDateTime.now());
        thongBaoNhomRepository.save(tbn);

        // ChiTietThongBaoNhom
        ChiTietThongBaoNhom ct = new ChiTietThongBaoNhom();
        ct.setId(UUID.randomUUID());
        ct.setThongBaoNhom(tbn);
        ct.setNguoiDung(kh);
        ct.setDaXem(false);
        chiTietThongBaoNhomRepository.save(ct);

        // 🔔 Cập nhật badge realtime qua SSE
        long unread = chiTietThongBaoNhomRepository.countByNguoiDungIdAndDaXemFalse(kh.getId());
        com.example.AsmGD1.controller.ThongBao.ThongBaoController.pushUnread(kh.getId(), unread);

        return tbn.getId();
    }
}
