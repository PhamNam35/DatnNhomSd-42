package com.example.AsmGD1.service.WebKhachHang;

import com.example.AsmGD1.dto.KhachHang.ThanhToan.CheckoutRequest;
import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.BanHang.DonHangPhieuGiamGiaRepository;
import com.example.AsmGD1.repository.BanHang.KHChiTietDonHangRepository;
import com.example.AsmGD1.repository.BanHang.KHDonHangRepository;
import com.example.AsmGD1.repository.BanHang.KHPhuongThucThanhToanRepository;
import com.example.AsmGD1.repository.GiamGia.KHPhieuGiamGiaRepository;
import com.example.AsmGD1.repository.GioHang.ChiTietGioHangRepository;
import com.example.AsmGD1.repository.GioHang.GioHangRepository;
import com.example.AsmGD1.repository.NguoiDung.DiaChiNguoiDungRepository;
import com.example.AsmGD1.repository.WebKhachHang.KhachHangChiTietSanPhamRepository;
import com.example.AsmGD1.service.GiamGia.ChienDichGiamGiaService;
import com.example.AsmGD1.service.GiamGia.PhieuGiamGiaCuaNguoiDungService;
import com.example.AsmGD1.service.GiamGia.PhieuGiamGiaService;
import com.example.AsmGD1.service.HoaDon.HoaDonService;
import com.example.AsmGD1.service.SanPham.ChiTietSanPhamService;
import com.example.AsmGD1.service.ViThanhToan.ViThanhToanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CheckoutService {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutService.class);

    @Autowired private KHDonHangRepository donHangRepository;
    @Autowired private KHChiTietDonHangRepository chiTietDonHangRepository;
    @Autowired private KhachHangChiTietSanPhamRepository chiTietSanPhamRepository;
    @Autowired private KHPhieuGiamGiaRepository phieuGiamGiaRepository;
    @Autowired private DonHangPhieuGiamGiaRepository donHangPhieuGiamGiaRepository;
    @Autowired private KHPhuongThucThanhToanRepository phuongThucThanhToanRepository;
    @Autowired private HoaDonService hoaDonService;
    @Autowired private ChiTietGioHangRepository chiTietGioHangRepository;
    @Autowired private GioHangRepository gioHangRepository;
    @Autowired private ViThanhToanService viThanhToanService;
    @Autowired private DiaChiNguoiDungRepository diaChiNguoiDungRepository;
    @Autowired private EmailService emailService;
    @Autowired private PhieuGiamGiaService phieuGiamGiaService;
    @Autowired private PhieuGiamGiaCuaNguoiDungService phieuGiamGiaCuaNguoiDungService;
    @Autowired private ChiTietSanPhamService chiTietSanPhamService;
    @Autowired private ChienDichGiamGiaService chienDichGiamGiaService;

    @Transactional
    public DonHang createOrder(NguoiDung nguoiDung, CheckoutRequest request, UUID addressId) {

        // ===== Khởi tạo đơn hàng cơ bản =====
        DonHang donHang = new DonHang();
        donHang.setNguoiDung(nguoiDung);
        donHang.setMaDonHang("DH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        donHang.setThoiGianTao(LocalDateTime.now());
        donHang.setTrangThaiThanhToan(false);
        donHang.setPhuongThucBanHang("Online");

        BigDecimal phiShip = (request.getShippingFee() != null) ? request.getShippingFee() : BigDecimal.valueOf(15000);
        donHang.setPhiVanChuyen(phiShip);

        String contact = "Người nhận: " + request.getFullName() + ", SĐT: " + request.getPhone();
        StringBuilder ghiChu = new StringBuilder();

        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            ghiChu.append(request.getNotes().trim());
        }

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            if (ghiChu.length() > 0) ghiChu.append("\n");
            ghiChu.append("Người nhận: ").append(request.getFullName().trim());
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            if (ghiChu.length() > 0) ghiChu.append("\n");
            ghiChu.append("SĐT: ").append(request.getPhone().trim());
        }

        donHang.setGhiChu(ghiChu.length() > 0 ? ghiChu.toString() : null);

        // ===== Địa chỉ giao hàng =====
        if (addressId != null) {
            DiaChiNguoiDung selected = diaChiNguoiDungRepository.findById(addressId)
                    .orElseThrow(() -> new RuntimeException("Địa chỉ không hợp lệ."));
            donHang.setDiaChi(selected);
            donHang.setDiaChiGiaoHang(selected.getChiTietDiaChi() + ", "
                    + selected.getPhuongXa() + ", "
                    + selected.getQuanHuyen() + ", "
                    + selected.getTinhThanhPho());
        } else if (request.getAddress() != null && !request.getAddress().isBlank()) {
            donHang.setDiaChiGiaoHang(request.getAddress());
        } else {
            DiaChiNguoiDung macDinh = diaChiNguoiDungRepository
                    .findByNguoiDung_IdAndMacDinhTrue(nguoiDung.getId())
                    .orElseThrow(() -> new RuntimeException("Vui lòng chọn hoặc nhập địa chỉ giao hàng."));
            donHang.setDiaChi(macDinh);
            donHang.setDiaChiGiaoHang(macDinh.getChiTietDiaChi() + ", "
                    + macDinh.getPhuongXa() + ", "
                    + macDinh.getQuanHuyen() + ", "
                    + macDinh.getTinhThanhPho());
        }

        // ===== Phương thức thanh toán =====
        PhuongThucThanhToan pttt = phuongThucThanhToanRepository
                .findById(request.getPaymentMethodId())
                .orElseThrow(() -> new RuntimeException("Phương thức thanh toán không hợp lệ."));
        donHang.setPhuongThucThanhToan(pttt);

        // ===== Chi tiết đơn & tính tổng =====
        BigDecimal tongTien = BigDecimal.ZERO;
        List<ChiTietDonHang> chiTietList = new ArrayList<>();

        for (CheckoutRequest.OrderItem item : request.getOrderItems()) {
            ChiTietSanPham ctsp = chiTietSanPhamRepository.findById(item.getChiTietSanPhamId())
                    .orElseThrow(() -> new RuntimeException("Chi tiết SP không tồn tại: " + item.getChiTietSanPhamId()));

            if (ctsp.getSoLuongTonKho() < item.getSoLuong()) {
                throw new RuntimeException("Sản phẩm " + ctsp.getSanPham().getTenSanPham()
                        + " không đủ số lượng. Còn lại: " + ctsp.getSoLuongTonKho());
            }

            BigDecimal gia = ctsp.getGia();
            int soLuong = item.getSoLuong();
            BigDecimal thanhTien = gia.multiply(BigDecimal.valueOf(soLuong));

            // Giảm theo chiến dịch (nếu có)
            Optional<ChienDichGiamGia> activeOpt =
                    chienDichGiamGiaService.getActiveCampaignForProduct(ctsp.getSanPham().getId());
            if (activeOpt.isPresent() && "ONGOING".equals(activeOpt.get().getStatus())) {
                BigDecimal percent = activeOpt.get().getPhanTramGiam();
                if (percent != null && percent.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal giamChiTiet = gia
                            .multiply(percent.divide(BigDecimal.valueOf(100)))
                            .multiply(BigDecimal.valueOf(soLuong));
                    thanhTien = thanhTien.subtract(giamChiTiet);
                }
            }

            ChiTietDonHang ctdh = new ChiTietDonHang();
            ctdh.setDonHang(donHang);
            ctdh.setChiTietSanPham(ctsp);
            ctdh.setGia(gia);
            ctdh.setSoLuong(soLuong);
            ctdh.setThanhTien(thanhTien);
            ctdh.setTenSanPham(ctsp.getSanPham().getTenSanPham());
            ctdh.setTrangThaiHoanTra(false);
            ctdh.setGhiChu(request.getNotes());

            chiTietList.add(ctdh);
            tongTien = tongTien.add(thanhTien);

            // Trừ kho
            chiTietSanPhamService.updateStockAndStatus(ctsp.getId(), -soLuong);
            logger.info("Update tồn kho {} -> còn {}", ctsp.getSanPham().getTenSanPham(), ctsp.getSoLuongTonKho());
        }

        // ===== Xử lý voucher (luôn ghi mapping; chỉ consume khi đủ điều kiện) =====
        BigDecimal tongGiam = BigDecimal.ZERO;
        List<DonHangPhieuGiamGia> mappings = new ArrayList<>();
        StringBuilder voucherDetails = new StringBuilder();

        // --- ORDER ---
        if (request.getVoucherOrder() != null && !request.getVoucherOrder().isBlank()) {
            String code = request.getVoucherOrder().trim();
            PhieuGiamGia pgOrder = phieuGiamGiaRepository.findByMa(code)
                    .orElseThrow(() -> new RuntimeException("Mã giảm giá đơn hàng không tồn tại."));

            if (!"Đang diễn ra".equals(phieuGiamGiaService.tinhTrang(pgOrder))) {
                throw new RuntimeException("Mã giảm giá đơn hàng không còn hiệu lực.");
            }
            if (!"ORDER".equalsIgnoreCase(pgOrder.getPhamViApDung())) {
                throw new RuntimeException("Mã " + pgOrder.getMa() + " không phải mã giảm giá đơn hàng.");
            }

            boolean datMin = (pgOrder.getGiaTriGiamToiThieu() == null)
                    || (tongTien.compareTo(pgOrder.getGiaTriGiamToiThieu()) >= 0);

            BigDecimal giamOrder = BigDecimal.ZERO;
            if (datMin) {
                // chỉ consume khi đủ điều kiện
                boolean isCaNhan = "CA_NHAN".equalsIgnoreCase(pgOrder.getKieuPhieu());
                boolean used = isCaNhan
                        ? phieuGiamGiaCuaNguoiDungService.suDungPhieu(nguoiDung.getId(), pgOrder.getId())
                        : phieuGiamGiaService.apDungPhieuGiamGia(pgOrder.getId());
                if (!used) {
                    throw new RuntimeException(isCaNhan
                            ? "Mã cá nhân không khả dụng hoặc đã hết lượt: " + pgOrder.getMa()
                            : "Mã công khai đã hết lượt: " + pgOrder.getMa());
                }

                giamOrder = phieuGiamGiaService.tinhTienGiamGia(pgOrder, tongTien);
                if (giamOrder == null) giamOrder = BigDecimal.ZERO;
                tongGiam = tongGiam.add(giamOrder);

                voucherDetails.append("<p>Mã giảm giá đơn hàng: ").append(pgOrder.getMa())
                        .append(" (-").append(new DecimalFormat("#,###").format(giamOrder)).append(" VNĐ)</p>");
            } else {
                logger.info("ORDER voucher hợp lệ nhưng chưa đạt min-order. Ghi mapping với giảm = 0, không trừ lượt.");
            }

            DonHangPhieuGiamGia mapOrder = new DonHangPhieuGiamGia();
            mapOrder.setDonHang(donHang);
            mapOrder.setPhieuGiamGia(pgOrder);
            mapOrder.setLoaiGiamGia("ORDER");
            mapOrder.setGiaTriGiam(giamOrder); // có thể = 0
            mapOrder.setThoiGianApDung(LocalDateTime.now());
            mappings.add(mapOrder);
        }

        // --- SHIPPING ---
        if (request.getVoucherShipping() != null && !request.getVoucherShipping().isBlank()) {
            String code = request.getVoucherShipping().trim();
            PhieuGiamGia pgShip = phieuGiamGiaRepository.findByMa(code)
                    .orElseThrow(() -> new RuntimeException("Mã giảm giá phí vận chuyển không tồn tại."));

            if (!"Đang diễn ra".equals(phieuGiamGiaService.tinhTrang(pgShip))) {
                throw new RuntimeException("Mã giảm giá phí vận chuyển không còn hiệu lực.");
            }
            if (!"SHIPPING".equalsIgnoreCase(pgShip.getPhamViApDung())) {
                throw new RuntimeException("Mã " + pgShip.getMa() + " không phải mã giảm giá phí vận chuyển.");
            }

            boolean datMin = (pgShip.getGiaTriGiamToiThieu() == null)
                    || (tongTien.compareTo(pgShip.getGiaTriGiamToiThieu()) >= 0);

            BigDecimal giamShip = BigDecimal.ZERO;
            if (datMin) {
                boolean isCaNhan = "CA_NHAN".equalsIgnoreCase(pgShip.getKieuPhieu());
                boolean used = isCaNhan
                        ? phieuGiamGiaCuaNguoiDungService.suDungPhieu(nguoiDung.getId(), pgShip.getId())
                        : phieuGiamGiaService.apDungPhieuGiamGia(pgShip.getId());
                if (!used) {
                    throw new RuntimeException(isCaNhan
                            ? "Mã cá nhân không khả dụng hoặc đã hết lượt: " + pgShip.getMa()
                            : "Mã công khai đã hết lượt: " + pgShip.getMa());
                }

                giamShip = phieuGiamGiaService.tinhGiamPhiShip(pgShip, phiShip, tongTien);
                if (giamShip == null) giamShip = BigDecimal.ZERO;
                if (giamShip.compareTo(phiShip) > 0) giamShip = phiShip;

                tongGiam = tongGiam.add(giamShip);

                voucherDetails.append("<p>Mã giảm giá phí vận chuyển: ").append(pgShip.getMa())
                        .append(" (-").append(new DecimalFormat("#,###").format(giamShip)).append(" VNĐ)</p>");
            } else {
                logger.info("SHIPPING voucher hợp lệ nhưng chưa đạt min-order. Ghi mapping với giảm = 0, không trừ lượt.");
            }

            DonHangPhieuGiamGia mapShip = new DonHangPhieuGiamGia();
            mapShip.setDonHang(donHang);
            mapShip.setPhieuGiamGia(pgShip);
            mapShip.setLoaiGiamGia("SHIPPING");
            mapShip.setGiaTriGiam(giamShip); // có thể = 0
            mapShip.setThoiGianApDung(LocalDateTime.now());
            mappings.add(mapShip);
        }

        // ===== Tổng cuối, lưu DB =====
        donHang.setTienGiam(tongGiam);
        donHang.setTongTien(tongTien.add(phiShip).subtract(tongGiam));

        donHang = donHangRepository.save(donHang);
        chiTietDonHangRepository.saveAll(chiTietList);

        if (!mappings.isEmpty()) {
            donHangPhieuGiamGiaRepository.saveAll(mappings);
            donHangPhieuGiamGiaRepository.flush(); // đảm bảo INSERT ngay
            logger.info("Đã lưu {} mapping vào don_hang_phieu_giam_gia cho đơn {}", mappings.size(), donHang.getMaDonHang());
        } else {
            logger.info("Không có voucher nào được nhập.");
        }

        // ===== Thanh toán bằng ví (nếu chọn) =====
        UUID VI_PAYMENT_METHOD_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440019");
        if (VI_PAYMENT_METHOD_ID.equals(request.getPaymentMethodId())) {
            boolean ok = viThanhToanService.thanhToanBangVi(
                    nguoiDung.getId(),
                    donHang.getId(),
                    donHang.getTongTien()
            );
            if (!ok) throw new RuntimeException("Số dư ví không đủ để thanh toán.");
        }

        // Đánh dấu đã thanh toán & thời gian
        donHang.setTrangThaiThanhToan(true);
        donHang.setThoiGianThanhToan(LocalDateTime.now());
        donHangRepository.save(donHang);

        // Xoá item khỏi giỏ hàng
        GioHang gioHang = gioHangRepository.findByNguoiDungId(nguoiDung.getId());
        if (gioHang != null && request.getOrderItems() != null) {
            for (CheckoutRequest.OrderItem it : request.getOrderItems()) {
                chiTietGioHangRepository.deleteByGioHangIdAndChiTietSanPhamId(gioHang.getId(), it.getChiTietSanPhamId());
            }
        }

        // Tạo hoá đơn
        hoaDonService.createHoaDonFromDonHang(donHang);

        // Gửi email xác nhận (an toàn: bọc try/catch để không chặn flow nếu mail lỗi)
        try {
            DecimalFormat formatter = new DecimalFormat("#,###.###");
            String formattedTongTien = formatter.format(donHang.getTongTien()) + " VNĐ";
            String paymentMethodName = donHang.getPhuongThucThanhToan().getTenPhuongThuc();

            String subject = "Xác nhận đơn hàng #" + donHang.getMaDonHang();
            String text = "<html><head><style>"
                    + "body{font-family:Inter,system-ui,-apple-system,'Segoe UI',Roboto,sans-serif;background:#f8fafc;color:#0f172a;padding:20px}"
                    + ".container{max-width:600px;margin:0 auto;background:#fff;border-radius:12px;box-shadow:0 4px 20px rgba(0,0,0,0.05);padding:30px}"
                    + "h1{color:#2563eb;text-align:center;font-size:24px;margin:0 0 16px}"
                    + "p{font-size:15px;margin:8px 0}"
                    + ".order-details{background:#f1f5f9;padding:16px;border-radius:8px;margin:16px 0;border:1px solid #e2e8f0}"
                    + ".cta-button{text-align:center;margin:20px 0}"
                    + ".cta-button a{display:inline-block;padding:10px 18px;background:#2563eb;color:#fff;text-decoration:none;border-radius:6px}"
                    + ".footer{text-align:center;font-size:13px;color:#64748b;margin-top:20px;border-top:1px solid #e2e8f0;padding-top:16px}"
                    + "</style></head><body><div class='container'>"
                    + "<h1>Cảm ơn bạn đã mua hàng tại ACV Store!</h1>"
                    + "<p>Đơn hàng của bạn đã được đặt thành công. Thông tin chi tiết:</p>"
                    + "<div class='order-details'>"
                    + "<p><strong>Mã đơn hàng:</strong> " + donHang.getMaDonHang() + "</p>"
                    + "<p><strong>Tên người nhận:</strong> " + request.getFullName() + "</p>"
                    + "<p><strong>Số điện thoại:</strong> " + request.getPhone() + "</p>"
                    + "<p><strong>Địa chỉ giao hàng:</strong> " + donHang.getDiaChiGiaoHang() + "</p>"
                    + "<p><strong>Tổng tiền:</strong> " + formattedTongTien + "</p>"
                    + "<p><strong>Phương thức thanh toán:</strong> " + paymentMethodName + "</p>"
                    + (mappings.isEmpty() ? "" : "<p><strong>Giảm giá áp dụng:</strong></p>"
                    + mappings.stream().map(m ->
                    "<p>- " + m.getLoaiGiamGia() + " (" + m.getPhieuGiamGia().getMa() + "): -"
                            + new DecimalFormat("#,###").format(
                            m.getGiaTriGiam() == null ? BigDecimal.ZERO : m.getGiaTriGiam()
                    ) + " VNĐ</p>"
            ).reduce("", String::concat))
                    + "</div>"
                    + "<div class='cta-button'><a href='https://acvstore.site/dsdon-mua/chi-tiet/" + donHang.getId() + "'>Xem trạng thái đơn hàng</a></div>"
                    + "<div class='footer'>ACV Store – Cảm ơn bạn đã tin tưởng!</div>"
                    + "</div></body></html>";

            emailService.sendEmail(nguoiDung.getEmail(), subject, text);
        } catch (Exception mailEx) {
            logger.warn("Gửi email xác nhận thất bại: {}", mailEx.getMessage());
        }

        return donHang;
    }

}
