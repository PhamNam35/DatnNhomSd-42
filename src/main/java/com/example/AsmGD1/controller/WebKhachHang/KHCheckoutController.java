package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.dto.KhachHang.ThanhToan.APIResponse;
import com.example.AsmGD1.dto.KhachHang.ThanhToan.CheckoutRequest;
import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.BanHang.KHDonHangRepository;
import com.example.AsmGD1.repository.GiamGia.KHPhieuGiamGiaRepository;
import com.example.AsmGD1.repository.NguoiDung.KHNguoiDungRepository;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.repository.ThongBao.ChiTietThongBaoNhomRepository;
import com.example.AsmGD1.repository.ThongBao.ThongBaoNhomRepository;
import com.example.AsmGD1.service.GiamGia.PhieuGiamGiaCuaNguoiDungService;
import com.example.AsmGD1.service.GiamGia.PhieuGiamGiaService;
import com.example.AsmGD1.service.GioHang.ChiTietGioHangService;
import com.example.AsmGD1.service.GioHang.KhachHangGioHangService;
import com.example.AsmGD1.service.WebKhachHang.CheckoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/checkout")
public class KHCheckoutController {

    private static final Logger logger = LoggerFactory.getLogger(KHCheckoutController.class);

    @Autowired private CheckoutService checkoutService;
    @Autowired private KHNguoiDungRepository nguoiDungRepository;
    @Autowired private KHPhieuGiamGiaRepository phieuGiamGiaRepository;
    @Autowired private KHDonHangRepository donHangRepository;
    @Autowired private KhachHangGioHangService khachHangGioHangService;
    @Autowired private ChiTietGioHangService chiTietGioHangService;
    @Autowired private PhieuGiamGiaService phieuGiamGiaService;
    @Autowired private PhieuGiamGiaCuaNguoiDungService phieuGiamGiaCuaNguoiDungService;
    @Autowired private ThongBaoNhomRepository thongBaoNhomRepository;
    @Autowired private ChiTietThongBaoNhomRepository chiTietThongBaoNhomRepository;

    // ===== DTOs & Helpers =====
    public static class CheckoutItem {
        private String chiTietSanPhamId;
        private BigDecimal gia;
        private int soLuong;
        public String getChiTietSanPhamId() { return chiTietSanPhamId; }
        public void setChiTietSanPhamId(String chiTietSanPhamId) { this.chiTietSanPhamId = chiTietSanPhamId; }
        public BigDecimal getGia() { return gia; }
        public void setGia(BigDecimal gia) { this.gia = gia; }
        public int getSoLuong() { return soLuong; }
        public void setSoLuong(int soLuong) { this.soLuong = soLuong; }
    }

    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;
        public ApiResponse(boolean success, String message) { this.success = success; this.message = message; }
        public ApiResponse(boolean success, String message, Object data) { this.success = success; this.message = message; this.data = data; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }

    private String formatVND(BigDecimal number) {
        return new DecimalFormat("#,##0").format(number) + " VNĐ";
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) return null;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return new BigDecimal(n.toString());
        String s = v.toString().replaceAll("[^0-9.-]", "");
        if (s.isEmpty()) return null;
        return new BigDecimal(s);
    }

    // ===== Auth endpoints =====
    @GetMapping("/check-auth")
    public ResponseEntity<APIResponse> checkAuth(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            NguoiDung nguoiDung = (NguoiDung) authentication.getPrincipal();
            logger.info("User authenticated: {}", nguoiDung.getTenDangNhap());
            return ResponseEntity.ok(new APIResponse("Authenticated", nguoiDung));
        }
        logger.warn("Unauthorized access attempt");
        return ResponseEntity.status(401).body(new APIResponse("Unauthorized"));
    }

    @GetMapping("/get-user")
    public ResponseEntity<APIResponse> getUser(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            NguoiDung nguoiDung = (NguoiDung) authentication.getPrincipal();
            logger.info("User retrieved: {}", nguoiDung.getTenDangNhap());
            return ResponseEntity.ok(new APIResponse("User found", nguoiDung));
        }
        logger.warn("Unauthorized access attempt");
        return ResponseEntity.status(401).body(new APIResponse("Unauthorized"));
    }

    // ===== Submit Order =====
    @PostMapping("/submit")
    public ResponseEntity<APIResponse> submitOrder(@RequestBody CheckoutRequest request, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Unauthenticated order submission attempt");
                return ResponseEntity.status(401).body(new APIResponse("Vui lòng đăng nhập"));
            }

            NguoiDung nguoiDung = (NguoiDung) authentication.getPrincipal();
            logger.info("Submitting order for user: {}", nguoiDung.getTenDangNhap());

            // Log tổng quan tham số
            logger.info("[/api/checkout/submit] user={}, addrId={}, vo={}, vs={}, shipFee={}, items={}",
                    nguoiDung.getTenDangNhap(),
                    request.getAddressId(),
                    request.getVoucherOrder(),
                    request.getVoucherShipping(),
                    request.getShippingFee(),
                    (request.getOrderItems() == null ? 0 : request.getOrderItems().size()));

            // =========================
            // 🔒 CHECK PTTT CHO VOUCHER
            // =========================
            UUID pmId = request.getPaymentMethodId();
            if (pmId == null) {
                return ResponseEntity.badRequest().body(new APIResponse("Vui lòng chọn phương thức thanh toán"));
            }

            // Kiểm tra voucher ORDER (nếu có)
            if (request.getVoucherOrder() != null && !request.getVoucherOrder().isBlank()) {
                var pOrderOpt = phieuGiamGiaRepository.findByMa(request.getVoucherOrder());
                if (pOrderOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body(new APIResponse("Mã ORDER không tồn tại"));
                }
                var pOrder = pOrderOpt.get();
                if (!phieuGiamGiaService.isPaymentMethodAllowed(pOrder, pmId)) {
                    return ResponseEntity.badRequest().body(
                            new APIResponse("Mã ORDER chỉ áp dụng cho: " +
                                    phieuGiamGiaService.allowedPaymentMethodNames(pOrder))
                    );
                }
            }

            // Kiểm tra voucher FREESHIP (nếu có)
            if (request.getVoucherShipping() != null && !request.getVoucherShipping().isBlank()) {
                var pShipOpt = phieuGiamGiaRepository.findByMa(request.getVoucherShipping());
                if (pShipOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body(new APIResponse("Mã FREESHIP không tồn tại"));
                }
                var pShip = pShipOpt.get();
                if (!phieuGiamGiaService.isPaymentMethodAllowed(pShip, pmId)) {
                    return ResponseEntity.badRequest().body(
                            new APIResponse("Mã FREESHIP chỉ áp dụng cho: " +
                                    phieuGiamGiaService.allowedPaymentMethodNames(pShip))
                    );
                }
            }
            // ===== HẾT KHỐI CHECK =====

            // Tạo đơn hàng
            DonHang donHang = checkoutService.createOrder(nguoiDung, request, request.getAddressId());
            logger.info("Order submitted successfully: {}", donHang.getMaDonHang());

            // ===== Tạo thông báo gốc (ghi 1 role hợp lệ để pass CHECK) =====
            ThongBaoNhom thongBao = new ThongBaoNhom();
            thongBao.setId(UUID.randomUUID());
            thongBao.setDonHang(donHang);
            thongBao.setVaiTroNhan("admin"); // ✅ không ghi "admin,employee" để tránh vi phạm CHECK
            thongBao.setTieuDe("Khách hàng đặt đơn hàng");
            thongBao.setNoiDung("Mã đơn: " + donHang.getMaDonHang());
            thongBao.setThoiGianTao(LocalDateTime.now());
            thongBao.setTrangThai("Mới");
            thongBaoNhomRepository.save(thongBao);

            // ===== Gom tất cả admin + employee, loại trùng theo ID =====
            List<NguoiDung> danhSachAdmin    = nguoiDungRepository.findByVaiTro("admin");
            List<NguoiDung> danhSachEmployee = nguoiDungRepository.findByVaiTro("employee");

            Map<UUID, NguoiDung> recipients = new LinkedHashMap<>();
            for (NguoiDung u : danhSachAdmin)    recipients.put(u.getId(), u);
            for (NguoiDung u : danhSachEmployee) recipients.put(u.getId(), u);

            // ===== Tạo chi tiết thông báo cho mọi người nhận =====
            for (NguoiDung nd : recipients.values()) {
                ChiTietThongBaoNhom chiTiet = new ChiTietThongBaoNhom();
                chiTiet.setId(UUID.randomUUID());
                chiTiet.setNguoiDung(nd);
                chiTiet.setThongBaoNhom(thongBao);
                chiTiet.setDaXem(false);
                chiTietThongBaoNhomRepository.save(chiTiet);
            }

            return ResponseEntity.ok(new APIResponse("Đặt hàng thành công", donHang.getMaDonHang()));

        } catch (RuntimeException e) {
            logger.error("Error submitting order: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new APIResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new APIResponse("Lỗi không xác định: " + e.getMessage()));
        }
    }



    @PostMapping(value = "/apply-voucher", params = "!type")
    public ResponseEntity<?> applyVoucherLegacy(@RequestParam String voucher,
                                                @RequestParam(required = false) String source,
                                                @RequestBody(required = false) List<CheckoutItem> checkoutItems,
                                                Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Unauthenticated voucher apply attempt");
                return ResponseEntity.status(401).body(new ApiResponse(false, "Vui lòng đăng nhập"));
            }

            NguoiDung nguoiDung = (NguoiDung) authentication.getPrincipal();
            logger.info("Applying voucher {} for user: {}, source: {}", voucher, nguoiDung.getTenDangNhap(), source);

            PhieuGiamGia phieuGiamGia = phieuGiamGiaRepository.findByMa(voucher)
                    .orElseThrow(() -> new RuntimeException("Mã giảm giá không hợp lệ"));

            // Nếu là mã SHIPPING thì không tính giảm đơn ở legacy
            if ("SHIPPING".equalsIgnoreCase(phieuGiamGia.getPhamViApDung())) {
                logger.info("ℹ️ Bỏ qua tinhTienGiamGia vì phiếu thuộc SHIPPING: {}", voucher);
                return ResponseEntity.ok(new ApiResponse(true, "Mã freeship – vui lòng dùng endpoint type=SHIPPING.", BigDecimal.ZERO));
            }

            if (!"Đang diễn ra".equals(phieuGiamGiaService.tinhTrang(phieuGiamGia))) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Phiếu giảm giá không trong thời gian hiệu lực"));
            }

            BigDecimal tongTien;
            if ("buy-now".equalsIgnoreCase(source) && checkoutItems != null && !checkoutItems.isEmpty()) {
                tongTien = checkoutItems.stream()
                        .map(item -> item.getGia().multiply(BigDecimal.valueOf(item.getSoLuong())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            } else {
                var gioHang = khachHangGioHangService.getOrCreateGioHang(nguoiDung.getId());
                List<ChiTietGioHang> chiTietList = chiTietGioHangService.getGioHangChiTietList(gioHang.getId());
                if (chiTietList.isEmpty()) {
                    return ResponseEntity.badRequest().body(new ApiResponse(false, "Giỏ hàng trống, không thể áp dụng mã giảm giá"));
                }
                tongTien = chiTietList.stream()
                        .map(item -> item.getGia().multiply(BigDecimal.valueOf(item.getSoLuong())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            if (phieuGiamGia.getGiaTriGiamToiThieu() != null &&
                    tongTien.compareTo(phieuGiamGia.getGiaTriGiamToiThieu()) < 0) {
                return ResponseEntity.badRequest().body(new ApiResponse(false,
                        String.format("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã (%s). Hiện tại: %s",
                                formatVND(phieuGiamGia.getGiaTriGiamToiThieu()), formatVND(tongTien))));
            }

            boolean isCaNhan = "CA_NHAN".equalsIgnoreCase(phieuGiamGia.getKieuPhieu());
            boolean valid = isCaNhan
                    ? phieuGiamGiaCuaNguoiDungService.kiemTraPhieuCaNhan(nguoiDung.getId(), phieuGiamGia.getId())
                    : phieuGiamGia.getSoLuong() > 0;
            if (!valid) {
                return ResponseEntity.badRequest().body(new ApiResponse(false,
                        isCaNhan
                                ? "Mã giảm giá cá nhân không hợp lệ hoặc đã hết lượt sử dụng."
                                : "Mã giảm giá công khai đã hết lượt sử dụng."));
            }

            BigDecimal discount = phieuGiamGiaService.tinhTienGiamGia(phieuGiamGia, tongTien);
            return ResponseEntity.ok(new ApiResponse(true, "Áp dụng mã giảm giá thành công", discount));

        } catch (RuntimeException e) {
            logger.error("Error applying voucher: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Lỗi áp dụng mã giảm giá: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(new ApiResponse(false, "Lỗi không xác định: " + e.getMessage()));
        }
    }


    @PostMapping(value = "/apply-voucher", params = "type=SHIPPING")
    public ResponseEntity<?> applyShipVoucherV2(@RequestParam("voucher") String voucherCode,
                                                @RequestParam("source") String source,                // cart | buy-now | cart-selected
                                                @RequestParam("shippingFee") BigDecimal shippingFee,  // phí ship hiện tại
                                                @RequestParam("subtotal") BigDecimal subtotal,        // tạm tính hàng
                                                @RequestParam("paymentMethodId") UUID paymentMethodId,
                                                @RequestBody(required = false) Object body,
                                                Authentication authentication) {
        try {
            // ==== Auth check ====
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Unauthenticated voucher apply attempt (SHIPPING)");
                return ResponseEntity.status(401).body(new ApiResponse(false, "Vui lòng đăng nhập"));
            }
            UUID userId = ((NguoiDung) authentication.getPrincipal()).getId();
            if (userId == null) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Không thể xác định người dùng."));
            }
            if (paymentMethodId == null) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Vui lòng chọn phương thức thanh toán."));
            }

            // ==== Tìm voucher ====
            PhieuGiamGia phieu = phieuGiamGiaService.layTatCa().stream()
                    .filter(p -> p.getMa() != null && p.getMa().equalsIgnoreCase(voucherCode.trim()))
                    .findFirst().orElse(null);
            if (phieu == null) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Mã freeship không tồn tại."));
            }
            if (!"Đang diễn ra".equals(phieuGiamGiaService.tinhTrang(phieu))) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Mã freeship không còn hiệu lực."));
            }
            if (!"SHIPPING".equalsIgnoreCase(phieu.getPhamViApDung())) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Mã này không phải freeship."));
            }

            // ==== RÀNG BUỘC PTTT ====
            if (!phieuGiamGiaService.isPaymentMethodAllowed(phieu, paymentMethodId)) {
                String names = phieuGiamGiaService.allowedPaymentMethodNames(phieu);
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Mã freeship chỉ áp dụng cho: " + names));
            }

            // ==== Nếu buy-now/cart-selected có gửi items thì tự tính lại subtotal ====
            if (body instanceof List<?> lst && !lst.isEmpty()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) lst;
                subtotal = items.stream()
                        .map(it -> {
                            BigDecimal gia = toBigDecimal(it.get("gia"));
                            int soLuong = it.get("soLuong") != null ? ((Number) it.get("soLuong")).intValue() : 0;
                            return gia.multiply(BigDecimal.valueOf(soLuong));
                        })
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            // ==== Validate input ====
            if (shippingFee == null || shippingFee.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Phí vận chuyển hiện tại bằng 0 hoặc thiếu."));
            }
            if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) < 0) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Thiếu/không hợp lệ subtotal."));
            }

            // ==== Chỉ kiểm tra khả dụng (không trừ lượt) ====
            boolean canUse = "CA_NHAN".equalsIgnoreCase(phieu.getKieuPhieu())
                    ? phieuGiamGiaCuaNguoiDungService.kiemTraPhieuCaNhan(userId, phieu.getId())
                    : (phieu.getSoLuong() != null && phieu.getSoLuong() > 0);

            if (!canUse) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Mã freeship không khả dụng hoặc đã hết lượt/số lượng."));
            }

            // ==== Tính giảm phí ship ====
            BigDecimal giamShip = phieuGiamGiaService.tinhGiamPhiShip(phieu, shippingFee, subtotal);
            if (giamShip == null) giamShip = BigDecimal.ZERO;
            if (giamShip.compareTo(shippingFee) > 0) giamShip = shippingFee;

            logger.info("FREESHIP applied preview: code={}, shippingFee={}, subtotal={}, discount={}, pmId={}",
                    voucherCode, shippingFee, subtotal, giamShip, paymentMethodId);

            return ResponseEntity.ok(new ApiResponse(true, "Áp dụng mã FREESHIP thành công.", giamShip));
        } catch (Exception e) {
            logger.error("Lỗi applyShipVoucherV2: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi áp dụng freeship: " + e.getMessage()));
        }
    }




    @PostMapping(value = "/apply-voucher", params = "type=ORDER")
    public ResponseEntity<?> applyOrderVoucherV2(@RequestParam("voucher") String voucherCode,
                                                 @RequestParam("source") String source, // cart | buy-now | cart-selected
                                                 @RequestParam(value = "subtotal", required = false) BigDecimal subtotalParam,
                                                 @RequestParam("paymentMethodId") UUID paymentMethodId,
                                                 @RequestBody(required = false) Object body,
                                                 Authentication authentication) {
        try {
            // ==== Auth check ====
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Unauthenticated voucher apply attempt (ORDER)");
                return ResponseEntity.status(401).body(new ApiResponse(false, "Vui lòng đăng nhập"));
            }
            UUID userId = ((NguoiDung) authentication.getPrincipal()).getId();
            if (userId == null) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Không thể xác định người dùng."));
            }
            if (paymentMethodId == null) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Vui lòng chọn phương thức thanh toán."));
            }

            // ==== Tìm voucher ====
            PhieuGiamGia phieu = phieuGiamGiaService.layTatCa().stream()
                    .filter(p -> p.getMa() != null && p.getMa().equalsIgnoreCase(voucherCode.trim()))
                    .findFirst().orElse(null);
            if (phieu == null) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Mã giảm giá không tồn tại."));
            }
            if (!"Đang diễn ra".equals(phieuGiamGiaService.tinhTrang(phieu))) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Phiếu giảm giá không trong thời gian hiệu lực."));
            }
            if ("SHIPPING".equalsIgnoreCase(phieu.getPhamViApDung())) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Mã freeship, vui lòng dùng mục FREESHIP."));
            }

            // ==== RÀNG BUỘC PTTT ====
            if (!phieuGiamGiaService.isPaymentMethodAllowed(phieu, paymentMethodId)) {
                String names = phieuGiamGiaService.allowedPaymentMethodNames(phieu);
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Mã giảm giá chỉ áp dụng cho: " + names));
            }

            // ==== Lấy / tính subtotal ====
            BigDecimal subtotal = subtotalParam;
            if (subtotal == null && body instanceof Map<?, ?> map && map.get("subtotal") != null) {
                subtotal = toBigDecimal(map.get("subtotal"));
            }
            if (subtotal == null) {
                GioHang gh = khachHangGioHangService.getOrCreateGioHang(userId);
                var chiTiet = chiTietGioHangService.getGioHangChiTietList(gh.getId());
                subtotal = chiTiet.stream()
                        .map(i -> i.getGia().multiply(BigDecimal.valueOf(i.getSoLuong())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            // ==== Ràng mức tối thiểu (nếu có) ====
            if (phieu.getGiaTriGiamToiThieu() != null && subtotal.compareTo(phieu.getGiaTriGiamToiThieu()) < 0) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Đơn hàng chưa đạt giá trị tối thiểu."));
            }

            // ==== Chỉ kiểm tra khả dụng (không trừ lượt) ====
            boolean canUse = "CA_NHAN".equalsIgnoreCase(phieu.getKieuPhieu())
                    ? phieuGiamGiaCuaNguoiDungService.kiemTraPhieuCaNhan(userId, phieu.getId())
                    : (phieu.getSoLuong() != null && phieu.getSoLuong() > 0);

            if (!canUse) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Mã giảm giá không khả dụng hoặc đã hết lượt/số lượng."));
            }

            // ==== Tính giảm đơn ====
            BigDecimal giamDon = phieuGiamGiaService.tinhTienGiamGia(phieu, subtotal);
            if (giamDon == null) giamDon = BigDecimal.ZERO;

            logger.info("ORDER voucher applied preview: code={}, subtotal={}, discount={}, pmId={}",
                    voucherCode, subtotal, giamDon, paymentMethodId);

            return ResponseEntity.ok(new ApiResponse(true, "Áp dụng mã giảm giá đơn thành công.", giamDon));
        } catch (Exception e) {
            logger.error("Lỗi applyOrderVoucherV2: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi áp dụng voucher: " + e.getMessage()));
        }
    }


}
