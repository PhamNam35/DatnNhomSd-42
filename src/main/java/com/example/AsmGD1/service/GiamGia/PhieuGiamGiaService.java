package com.example.AsmGD1.service.GiamGia;

import com.example.AsmGD1.entity.PhieuGiamGia;
import com.example.AsmGD1.entity.PhuongThucThanhToan;
import com.example.AsmGD1.repository.GiamGia.PhieuGiamGiaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhieuGiamGiaService {

    private static final Logger logger = LoggerFactory.getLogger(PhieuGiamGiaService.class);
    private final PhieuGiamGiaRepository phieuGiamGiaRepository;

    public List<PhieuGiamGia> layTatCa() {
        return phieuGiamGiaRepository.findAll();
    }

    public PhieuGiamGia luu(PhieuGiamGia phieu) {
        if (phieu.getThoiGianTao() == null) {
            phieu.setThoiGianTao(LocalDateTime.now());
        }
        if ("ca_nhan".equalsIgnoreCase(phieu.getKieuPhieu())) {
            phieu.setSoLuong(1);
            phieu.setGioiHanSuDung(1);
        } else {
            if (phieu.getSoLuong() == null) phieu.setSoLuong(1);
            if (phieu.getGioiHanSuDung() == null) phieu.setGioiHanSuDung(phieu.getSoLuong());
        }
        return phieuGiamGiaRepository.save(phieu);
    }



    public String tinhTrang(PhieuGiamGia v) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime batDau = v.getNgayBatDau();
        LocalDateTime ketThuc = v.getNgayKetThuc();
        if (batDau != null && ketThuc != null) {
            if (now.isBefore(batDau)) return "Sắp diễn ra";
            else if (!now.isAfter(ketThuc)) return "Đang diễn ra";
            else return "Đã kết thúc";
        }
        return "Không xác định";
    }

    @Transactional
    public boolean apDungPhieuGiamGia(UUID phieuId) {
        PhieuGiamGia phieu = phieuGiamGiaRepository.findById(phieuId).orElse(null);
        if (phieu == null) {
            logger.warn("Không tìm thấy phiếu giảm giá: phieuId={}", phieuId);
            return false;
        }
        if (!"Đang diễn ra".equals(tinhTrang(phieu))) {
            logger.warn("Phiếu giảm giá không trong thời gian hiệu lực: phieuId={}", phieuId);
            return false;
        }
        Integer soLuong = phieu.getSoLuong();
        if (soLuong != null && soLuong > 0) {
            phieu.setSoLuong(soLuong - 1);
            phieuGiamGiaRepository.save(phieu);
            phieuGiamGiaRepository.flush();
            logger.info("Áp dụng phiếu công khai thành công: phieuId={}, soLuong moi={}", phieuId, phieu.getSoLuong());
            return true;
        }
        logger.warn("Phiếu công khai không khả dụng: soLuong={}", soLuong);
        return false;
    }

    public BigDecimal tinhTienGiamGia(PhieuGiamGia phieu, BigDecimal tongTien) {
        if (phieu == null || tongTien == null || tongTien.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("⚠️ Đầu vào không hợp lệ - phieu: {}, tongTien: {}", phieu, tongTien);
            return BigDecimal.ZERO;
        }
        // ⛔ BỎ QUA freeship ở hàm này
        if ("SHIPPING".equalsIgnoreCase(phieu.getPhamViApDung())) {
            logger.info("ℹ️ Bỏ qua tinhTienGiamGia vì phiếu thuộc SHIPPING: {}", phieu.getMa());
            return BigDecimal.ZERO;
        }

        BigDecimal giamGia = BigDecimal.ZERO;
        BigDecimal giaTriGiam = phieu.getGiaTriGiam();
        if (giaTriGiam == null || giaTriGiam.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("⚠️ Giá trị giảm không hợp lệ - giaTriGiam: {}", giaTriGiam);
            return BigDecimal.ZERO;
        }
        String loai = phieu.getLoai();
        if (loai == null || loai.isBlank()) {
            logger.warn("⚠️ Loại phiếu null hoặc rỗng - ma: {}", phieu.getMa());
            return BigDecimal.ZERO;
        }
        String loaiChuanHoa = loai.trim().toUpperCase().replace("_", "").replace("-", "").replace(" ", "");
        logger.info("🔎 Tính tiền giảm giá | Mã: {}, Loại gốc: {}, Loại chuẩn hóa: {}, Giá trị giảm: {}, Tổng tiền: {}",
                phieu.getMa(), loai, loaiChuanHoa, giaTriGiam, tongTien);

        if ("PERCENT".equals(loaiChuanHoa) || "PHANTRAM".equals(loaiChuanHoa)) {
            giamGia = tongTien.multiply(giaTriGiam).divide(BigDecimal.valueOf(100), 0, BigDecimal.ROUND_HALF_UP);
            if (phieu.getGiaTriGiamToiDa() != null && giamGia.compareTo(phieu.getGiaTriGiamToiDa()) > 0) {
                giamGia = phieu.getGiaTriGiamToiDa();
            }
        } else if ("FIXED".equals(loaiChuanHoa) || "TIENMAT".equals(loaiChuanHoa) || "CASH".equals(loaiChuanHoa)) {
            giamGia = giaTriGiam;
        } else {
            logger.warn("⚠️ Loại phiếu không hỗ trợ: {}", loaiChuanHoa);
            return BigDecimal.ZERO;
        }

        if (giamGia.compareTo(tongTien) > 0) giamGia = tongTien;
        logger.info("✅ Số tiền được giảm: {}", giamGia);
        return giamGia;
    }

    public boolean isVoucherValid(PhieuGiamGia phieu, BigDecimal orderTotal, UUID paymentMethodId) {
        if (phieu == null) {
            logger.warn("Voucher is null");
            return false;
        }

        // Check status
        String status = tinhTrang(phieu);
        if (!"Đang diễn ra".equals(status)) {
            logger.warn("Voucher {} is not active. Status: {}", phieu.getMa(), status);
            return false;
        }

        // Check quantity
        if (phieu.getSoLuong() != null && phieu.getSoLuong() <= 0) {
            logger.warn("Voucher {} has no remaining quantity: {}", phieu.getMa(), phieu.getSoLuong());
            return false;
        }

        // Check minimum order value
        if (phieu.getGiaTriGiamToiThieu() != null && orderTotal != null) {
            if (orderTotal.compareTo(phieu.getGiaTriGiamToiThieu()) < 0) {
                logger.warn("Order total {} is less than minimum required {} for voucher {}",
                        orderTotal, phieu.getGiaTriGiamToiThieu(), phieu.getMa());
                return false;
            }
        }

        // Check payment method
        if (!isPaymentMethodAllowed(phieu, paymentMethodId)) {
            logger.warn("Payment method {} not allowed for voucher {}", paymentMethodId, phieu.getMa());
            return false;
        }

        logger.info("Voucher {} is valid for application", phieu.getMa());
        return true;
    }

    /* =========================
       RÀNG BUỘC PHƯƠNG THỨC TT
       ========================= */

    @Transactional(Transactional.TxType.SUPPORTS)
    public boolean isPaymentMethodAllowed(PhieuGiamGia phieu, UUID paymentMethodId) {
        if (paymentMethodId == null) return false;
        // Không cấu hình danh sách PTTT => áp dụng cho tất cả
        if (phieu.getPhuongThucThanhToans() == null || phieu.getPhuongThucThanhToans().isEmpty()) {
            return true;
        }
        return phieu.getPhuongThucThanhToans().stream()
                .anyMatch(pt -> paymentMethodId.equals(pt.getId()));
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public String allowedPaymentMethodNames(PhieuGiamGia phieu) {
        if (phieu.getPhuongThucThanhToans() == null || phieu.getPhuongThucThanhToans().isEmpty()) {
            return "tất cả phương thức thanh toán";
        }
        return phieu.getPhuongThucThanhToans().stream()
                .map(PhuongThucThanhToan::getTenPhuongThuc)
                .collect(Collectors.joining(", "));
    }

    private String normalizeType(String raw) {
        if (raw == null) return "";
        // bỏ khoảng trắng, gạch dưới, gạch nối; viết hoa
        String t = raw.trim().toUpperCase().replace("_", "").replace("-", "").replace(" ", "");
        // Quy về 2 “nhãn” duy nhất
        if (t.equals("FREESHIPFULL") || t.equals("FULLFREESHIP") || t.equals("FREESHIPALL")) {
            return "FREESHIP_FULL";
        }
        if (t.equals("FREESHIPCAP") || t.equals("CAPFREESHIP") || t.equals("FREESHIPLIMIT")) {
            return "FREESHIP_CAP";
        }
        return t; // trả nguyên nếu không khớp để switch xử lý default
    }

    public BigDecimal tinhGiamPhiShip(PhieuGiamGia phieu, BigDecimal phiShip, BigDecimal tongTruocShip) {
        if (phieu == null || phiShip == null || phiShip.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("⚠️ Đầu vào không hợp lệ - phieu: {}, phiShip: {}, tongTruocShip: {}", phieu, phiShip, tongTruocShip);
            return BigDecimal.ZERO;
        }
        if (!"SHIPPING".equalsIgnoreCase(phieu.getPhamViApDung())) {
            logger.warn("⚠️ Phiếu không phải freeship - ma: {}, phamViApDung: {}", phieu.getMa(), phieu.getPhamViApDung());
            return BigDecimal.ZERO;
        }

        // Kiểm tra điều kiện đơn tối thiểu
        if (phieu.getGiaTriGiamToiThieu() != null && tongTruocShip != null &&
                tongTruocShip.compareTo(phieu.getGiaTriGiamToiThieu()) < 0) {
            logger.warn("🚫 Không đạt đơn tối thiểu freeship - yêu cầu: {}, thực tế: {}", phieu.getGiaTriGiamToiThieu(), tongTruocShip);
            return BigDecimal.ZERO;
        }

        // Dùng chuẩn normalize để hỗ trợ các biến thể cấu hình
        String loai = normalizeType(phieu.getLoai());
        BigDecimal giamShip = BigDecimal.ZERO;

        logger.info("🔎 Tính giảm phí ship - Mã: {}, Loai: {}, GiaTriGiamToiThieu: {}, GiaTriGiamToiDa: {}, PhiShip: {}, Subtotal: {}",
                phieu.getMa(), loai, phieu.getGiaTriGiamToiThieu(), phieu.getGiaTriGiamToiDa(), phiShip, tongTruocShip);

        switch (loai) { // đã normalize -> chỉ 2 trường hợp
            case "FREESHIP_FULL":
                giamShip = phiShip; // Miễn toàn bộ phí ship
                logger.info("✅ Áp dụng FREESHIP_FULL: giảm {}", giamShip);
                break;
            case "FREESHIP_CAP":
                if (phieu.getGiaTriGiamToiDa() == null || phieu.getGiaTriGiamToiDa().compareTo(BigDecimal.ZERO) <= 0) {
                    logger.warn("⚠️ FREESHIP_CAP thiếu hoặc không hợp lệ giaTriGiamToiDa: {}", phieu.getGiaTriGiamToiDa());
                    return BigDecimal.ZERO;
                }
                giamShip = phiShip.min(phieu.getGiaTriGiamToiDa()); // Miễn tối đa gia_tri_giam_toi_da
                logger.info("✅ Áp dụng FREESHIP_CAP: giảm {}, tối đa {}", giamShip, phieu.getGiaTriGiamToiDa());
                break;
            default:
                logger.warn("⚠️ Loại freeship không hỗ trợ: {}", loai);
                return BigDecimal.ZERO;
        }

        logger.info("✅ Kết quả giảm phí ship: {}", giamShip);
        return giamShip;
    }
}
