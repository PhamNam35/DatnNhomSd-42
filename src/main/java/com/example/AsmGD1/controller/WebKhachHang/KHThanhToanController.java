package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.dto.KhachHang.ThanhToan.CheckoutRequest;
import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.BanHang.ChiTietDonHangRepository;
import com.example.AsmGD1.repository.BanHang.DonHangRepository;
import com.example.AsmGD1.repository.BanHang.PhuongThucThanhToanRepository;
import com.example.AsmGD1.repository.HoaDon.HoaDonRepository;
import com.example.AsmGD1.repository.HoaDon.LichSuHoaDonRepository;
import com.example.AsmGD1.repository.NguoiDung.DiaChiNguoiDungRepository;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.service.GiamGia.PhieuGiamGiaCuaNguoiDungService;
import com.example.AsmGD1.service.GiamGia.PhieuGiamGiaService;
import com.example.AsmGD1.service.GioHang.ChiTietGioHangService;
import com.example.AsmGD1.service.GioHang.KhachHangGioHangService;
import com.example.AsmGD1.service.HoaDon.HoaDonService;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.WebKhachHang.CheckoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/thanh-toan")
public class KHThanhToanController {

    private static final Logger logger = LoggerFactory.getLogger(KHThanhToanController.class);

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private NguoiDungService nguoiDungService;

    @Autowired
    private KhachHangGioHangService khachHangGioHangService;

    @Autowired
    private ChiTietGioHangService chiTietGioHangService;

    @Autowired
    private DonHangRepository donHangRepo;

    @Autowired
    private ChiTietDonHangRepository chiTietDonHangRepo;

    @Autowired
    private HoaDonRepository hoaDonRepo;

    @Autowired
    private LichSuHoaDonRepository lichSuRepo;

    @Autowired
    private PhuongThucThanhToanRepository phuongThucRepo;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @Autowired
    private PhieuGiamGiaService phieuGiamGiaService;

    @Autowired
    private PhieuGiamGiaCuaNguoiDungService phieuGiamGiaCuaNguoiDungService;

    @Autowired
    private DiaChiNguoiDungRepository diaChiNguoiDungRepository;

    @Autowired
    private HoaDonService hoaDonService;

    @Autowired
    private CheckoutService checkoutService; // Thêm dependency

    private UUID getNguoiDungIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            NguoiDung nguoiDung = nguoiDungService.findByTenDangNhap(userDetails.getUsername());
            return nguoiDung != null ? nguoiDung.getId() : null;
        }

        if (principal instanceof OAuth2User oAuth2User) {
            String email = oAuth2User.getAttribute("email");
            NguoiDung nguoiDung = nguoiDungService.findByEmail(email);
            return nguoiDung != null ? nguoiDung.getId() : null;
        }

        return null;
    }

    private String dinhDangTien(BigDecimal soTien) {
        return String.format("%,.0f", soTien).replace(",", ".") + " VND";
    }

    @GetMapping
    public String showCheckoutPage(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            model.addAttribute("error", "Vui lòng đăng nhập để thanh toán!");
            return "WebKhachHang/thanh-toan";
        }

        UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
        if (nguoiDungId == null) {
            model.addAttribute("error", "Không thể xác định người dùng. Vui lòng đăng nhập lại!");
            return "WebKhachHang/thanh-toan";
        }

        NguoiDung nguoiDung = nguoiDungService.findById(nguoiDungId);
        if (nguoiDung == null) {
            model.addAttribute("error", "Người dùng không tồn tại!");
            return "WebKhachHang/thanh-toan";
        }

        try {
            // Thông tin người dùng + địa chỉ
            model.addAttribute("loggedInUser", nguoiDung);

            List<DiaChiNguoiDung> addresses = diaChiNguoiDungRepository.findByNguoiDung_Id(nguoiDung.getId());
            model.addAttribute("addresses", addresses);

            Optional<DiaChiNguoiDung> defaultAddress = diaChiNguoiDungRepository
                    .findByNguoiDung_IdAndMacDinhTrue(nguoiDung.getId());
            if (defaultAddress.isPresent()) {
                DiaChiNguoiDung a = defaultAddress.get();
                model.addAttribute("defaultAddress", a.getChiTietDiaChi() + ", " + a.getPhuongXa() + ", "
                        + a.getQuanHuyen() + ", " + a.getTinhThanhPho());
                model.addAttribute("nguoiNhan", a.getNguoiNhan() != null ? a.getNguoiNhan() : nguoiDung.getHoTen());
                model.addAttribute("soDienThoaiNguoiNhan",
                        a.getSoDienThoaiNguoiNhan() != null ? a.getSoDienThoaiNguoiNhan() : nguoiDung.getSoDienThoai());
                model.addAttribute("chiTietDiaChi",
                        a.getChiTietDiaChi() != null ? a.getChiTietDiaChi() : nguoiDung.getChiTietDiaChi());
            } else {
                model.addAttribute("defaultAddress", "");
                model.addAttribute("nguoiNhan", nguoiDung.getHoTen());
                model.addAttribute("soDienThoaiNguoiNhan", nguoiDung.getSoDienThoai());
            }

            // Lấy danh sách voucher (công khai còn lượt + cá nhân còn hạn), loại trùng
            List<PhieuGiamGia> publicVouchers = phieuGiamGiaService.layTatCa().stream()
                    .filter(p -> "cong_khai".equalsIgnoreCase(p.getKieuPhieu()))
                    .filter(p -> "Đang diễn ra".equals(phieuGiamGiaService.tinhTrang(p)))
                    .filter(p -> p.getSoLuong() != null && p.getSoLuong() > 0)
                    .toList();

            List<PhieuGiamGia> privateVouchers =
                    phieuGiamGiaCuaNguoiDungService.layPhieuCaNhanConHan(nguoiDung.getId());

            java.util.Map<UUID, PhieuGiamGia> merged = new java.util.LinkedHashMap<>();
            publicVouchers.forEach(v -> merged.put(v.getId(), v));
            privateVouchers.forEach(v -> merged.put(v.getId(), v));

            List<PhieuGiamGia> allVouchers = new ArrayList<>(merged.values());
            model.addAttribute("vouchers", allVouchers);

            // ===== Build map PTTT hiển thị cho từng phiếu =====
            java.util.Map<UUID, String>  voucherPtttMapById = new java.util.LinkedHashMap<>();
            java.util.Map<String, String> voucherPtttMapByMa = new java.util.LinkedHashMap<>();

            for (PhieuGiamGia v : allVouchers) {
                List<PhuongThucThanhToan> list;
                try {
                    list = phuongThucRepo.findSelectedPtttByVoucherId(v.getId());
                } catch (Exception ex) {
                    list = java.util.Collections.emptyList();
                }

                String display;
                if (list == null || list.isEmpty()) {
                    // Không cấu hình PTTT -> áp dụng tất cả
                    display = "Tất cả";
                } else {
                    display = list.stream()
                            .map(pm -> {
                                String ten = pm.getTenPhuongThuc();
                                return (ten == null || ten.isBlank())
                                        ? (pm.getId() != null ? pm.getId().toString() : null)
                                        : ten;
                            })
                            .filter(java.util.Objects::nonNull)
                            .distinct()
                            .collect(java.util.stream.Collectors.joining(", "));
                }

                voucherPtttMapById.put(v.getId(), display);
                if (v.getMa() != null) voucherPtttMapByMa.put(v.getMa(), display);
            }

            model.addAttribute("voucherPtttMapById", voucherPtttMapById);
            model.addAttribute("voucherPtttMapByMa", voucherPtttMapByMa);

            // Log kiểm tra
            logger.info("PTTT map by ID: {}", voucherPtttMapById);
            logger.info("PTTT map by MA : {}", voucherPtttMapByMa);
            logger.info("Tổng số voucher truyền ra view: {}", allVouchers.size());

        } catch (Exception e) {
            model.addAttribute("error", "Không thể tải thông tin người dùng: " + e.getMessage());
            model.addAttribute("defaultAddress", "");
            model.addAttribute("nguoiNhan", nguoiDung.getHoTen());
            model.addAttribute("soDienThoaiNguoiNhan", nguoiDung.getSoDienThoai());
        }
        return "WebKhachHang/thanh-toan";
    }



    @Transactional
    @PostMapping("/dat-hang")
    public String datHang(
            @RequestParam("ptThanhToan") String ptThanhToan,
            @RequestParam("fullName") String fullName,
            @RequestParam("phone") String phone,
            @RequestParam("addressId") UUID addressId,
            @RequestParam(value = "address", required = false) String customAddress,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "voucherOrder", required = false) String voucherOrder,
            @RequestParam(value = "voucherShipping", required = false) String voucherShipping,
            @RequestParam(value = "shippingFee", required = false, defaultValue = "0") BigDecimal shippingFee,
            Authentication authentication,
            RedirectAttributes redirect) {
        UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
        if (nguoiDungId == null) {
            redirect.addFlashAttribute("error", "Không thể xác định người dùng. Vui lòng đăng nhập lại.");
            return "redirect:/thanh-toan";
        }

        NguoiDung nguoiDung = nguoiDungService.findById(nguoiDungId);
        if (nguoiDung == null) {
            redirect.addFlashAttribute("error", "Người dùng không tồn tại.");
            return "redirect:/thanh-toan";
        }

        GioHang gioHang = khachHangGioHangService.getOrCreateGioHang(nguoiDung.getId());
        List<ChiTietGioHang> chiTietList = chiTietGioHangService.getGioHangChiTietList(gioHang.getId());
        if (chiTietList.isEmpty()) {
            redirect.addFlashAttribute("error", "Giỏ hàng của bạn hiện đang rỗng.");
            return "redirect:/thanh-toan";
        }
        logger.info("[/thanh-toan/dat-hang] vo={}, vs={}, shippingFee={}, fullName={}, addressId={}",
                voucherOrder, voucherShipping, shippingFee, fullName, addressId);

        // Tạo CheckoutRequest
        CheckoutRequest request = new CheckoutRequest();
        request.setAddressId(addressId);
        request.setFullName(fullName);
        request.setPhone(phone);
        request.setAddress(customAddress);
        request.setNotes(note);
        request.setShippingMethod("STANDARD"); // Giả định mặc định, có thể sửa
        request.setPaymentMethodId(UUID.fromString(ptThanhToan));
        request.setVoucherOrder(voucherOrder);
        request.setVoucherShipping(voucherShipping);
        request.setShippingFee(shippingFee);

        List<CheckoutRequest.OrderItem> orderItems = new ArrayList<>();
        for (ChiTietGioHang item : chiTietList) {
            CheckoutRequest.OrderItem orderItem = new CheckoutRequest.OrderItem();
            orderItem.setChiTietSanPhamId(item.getChiTietSanPham().getId());
            orderItem.setSoLuong(item.getSoLuong());
            orderItems.add(orderItem);
        }
        request.setOrderItems(orderItems);

        try {
            // Gọi CheckoutService để tạo đơn hàng
            DonHang donHang = checkoutService.createOrder(nguoiDung, request, addressId);
            redirect.addFlashAttribute("success", "Đặt hàng thành công! Mã đơn hàng: " + donHang.getMaDonHang());
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Lỗi khi đặt hàng: " + e.getMessage());
        }

        return "redirect:/thanh-toan";
    }

    @PostMapping("/api/checkout/apply-voucher")
    public ResponseEntity<?> applyVoucher(
            @RequestParam(value = "voucherOrder", required = false) String voucherOrder,
            @RequestParam(value = "voucherShipping", required = false) String voucherShipping,
            Authentication authentication) {
        try {
            UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
            if (nguoiDungId == null) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Không thể xác định người dùng."));
            }

            NguoiDung nguoiDung = nguoiDungService.findById(nguoiDungId);
            if (nguoiDung == null) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Người dùng không tồn tại."));
            }

            GioHang gioHang = khachHangGioHangService.getOrCreateGioHang(nguoiDung.getId());
            List<ChiTietGioHang> chiTietList = chiTietGioHangService.getGioHangChiTietList(gioHang.getId());
            BigDecimal tongTien = chiTietList.stream()
                    .map(item -> item.getGia().multiply(BigDecimal.valueOf(item.getSoLuong())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal giamGiaOrder = BigDecimal.ZERO;
            BigDecimal giamGiaShipping = BigDecimal.ZERO;

            // ===== ORDER voucher (preview-only, KHÔNG trừ lượt ở đây) =====
            if (voucherOrder != null && !voucherOrder.trim().isEmpty()) {
                PhieuGiamGia phieu = phieuGiamGiaService.layTatCa().stream()
                        .filter(p -> p.getMa() != null && p.getMa().equalsIgnoreCase(voucherOrder.trim()))
                        .findFirst()
                        .orElse(null);
                if (phieu == null) {
                    return ResponseEntity.badRequest().body(new ApiResponse(false, "Mã giảm giá đơn hàng không tồn tại."));
                }
                if (!"Đang diễn ra".equals(phieuGiamGiaService.tinhTrang(phieu))) {
                    return ResponseEntity.badRequest().body(new ApiResponse(false, "Mã giảm giá đơn hàng không trong thời gian hiệu lực."));
                }
                if (!"ORDER".equalsIgnoreCase(phieu.getPhamViApDung())) {
                    return ResponseEntity.badRequest().body(new ApiResponse(false, "Mã " + phieu.getMa() + " không phải mã giảm giá đơn hàng."));
                }
                if (phieu.getGiaTriGiamToiThieu() != null && tongTien.compareTo(phieu.getGiaTriGiamToiThieu()) < 0) {
                    return ResponseEntity.badRequest().body(new ApiResponse(false, "Đơn hàng chưa đạt giá trị tối thiểu cho mã " + phieu.getMa() + "."));
                }

                boolean isCaNhan = "CA_NHAN".equalsIgnoreCase(phieu.getKieuPhieu());
                boolean canUse = isCaNhan
                        ? phieuGiamGiaCuaNguoiDungService.kiemTraPhieuCaNhan(nguoiDung.getId(), phieu.getId())
                        : (phieu.getSoLuong() != null && phieu.getSoLuong() > 0);
                if (!canUse) {
                    return ResponseEntity.badRequest().body(new ApiResponse(false,
                            isCaNhan
                                    ? "Mã giảm giá cá nhân không khả dụng hoặc đã hết lượt sử dụng: " + phieu.getMa()
                                    : "Mã giảm giá công khai đã hết lượt sử dụng: " + phieu.getMa()));
                }

                giamGiaOrder = phieuGiamGiaService.tinhTienGiamGia(phieu, tongTien);
                if (giamGiaOrder == null) giamGiaOrder = BigDecimal.ZERO;
                logger.info("🎯 Preview voucherOrder - Mã: {}, Giảm: {}", phieu.getMa(), giamGiaOrder);
            }

            // ===== SHIPPING voucher (preview-only, KHÔNG trừ lượt ở đây) =====
            if (voucherShipping != null && !voucherShipping.trim().isEmpty()) {
                PhieuGiamGia phieu = phieuGiamGiaService.layTatCa().stream()
                        .filter(p -> p.getMa() != null && p.getMa().equalsIgnoreCase(voucherShipping.trim()))
                        .findFirst()
                        .orElse(null);
                if (phieu == null) {
                    return ResponseEntity.badRequest().body(new ApiResponse(false, "Mã giảm giá phí vận chuyển không tồn tại."));
                }
                if (!"Đang diễn ra".equals(phieuGiamGiaService.tinhTrang(phieu))) {
                    return ResponseEntity.badRequest().body(new ApiResponse(false, "Mã giảm giá phí vận chuyển không trong thời gian hiệu lực."));
                }
                if (!"SHIPPING".equalsIgnoreCase(phieu.getPhamViApDung())) {
                    return ResponseEntity.badRequest().body(new ApiResponse(false, "Mã " + phieu.getMa() + " không phải mã giảm giá phí vận chuyển."));
                }
                if (phieu.getGiaTriGiamToiThieu() != null && tongTien.compareTo(phieu.getGiaTriGiamToiThieu()) < 0) {
                    return ResponseEntity.badRequest().body(new ApiResponse(false, "Đơn hàng chưa đạt giá trị tối thiểu cho mã " + phieu.getMa() + "."));
                }

                boolean isCaNhan = "CA_NHAN".equalsIgnoreCase(phieu.getKieuPhieu());
                boolean canUse = isCaNhan
                        ? phieuGiamGiaCuaNguoiDungService.kiemTraPhieuCaNhan(nguoiDung.getId(), phieu.getId())
                        : (phieu.getSoLuong() != null && phieu.getSoLuong() > 0);
                if (!canUse) {
                    return ResponseEntity.badRequest().body(new ApiResponse(false,
                            isCaNhan
                                    ? "Mã giảm giá cá nhân không khả dụng hoặc đã hết lượt sử dụng: " + phieu.getMa()
                                    : "Mã giảm giá công khai đã hết lượt sử dụng: " + phieu.getMa()));
                }

                // TODO: Lấy shippingFee thực tế từ client nếu có; tạm mặc định 15.000đ
                BigDecimal shippingFee = BigDecimal.valueOf(15000);
                giamGiaShipping = phieuGiamGiaService.tinhGiamPhiShip(phieu, shippingFee, tongTien);
                if (giamGiaShipping == null) giamGiaShipping = BigDecimal.ZERO;
                if (giamGiaShipping.compareTo(shippingFee) > 0) giamGiaShipping = shippingFee;

                logger.info("🎯 Preview voucherShipping - Mã: {}, Giảm: {}", phieu.getMa(), giamGiaShipping);
            }

            ApiResponse response = new ApiResponse(true, "Áp dụng mã giảm giá thành công (preview).");
            response.setData(new VoucherResponse(giamGiaOrder, giamGiaShipping));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Lỗi khi áp dụng mã giảm giá: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi áp dụng mã giảm giá: " + e.getMessage()));
        }
    }


    @GetMapping("/api/checkout/default-address")
    public ResponseEntity<?> getDefaultAddress(Authentication authentication) {
        try {
            UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
            if (nguoiDungId == null) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Không thể xác định người dùng."));
            }

            NguoiDung nguoiDung = nguoiDungService.findById(nguoiDungId);
            if (nguoiDung == null) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Người dùng không tồn tại."));
            }

            Optional<DiaChiNguoiDung> defaultAddress = diaChiNguoiDungRepository.findByNguoiDung_IdAndMacDinhTrue(nguoiDung.getId());
            if (defaultAddress.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse(true, "Không tìm thấy địa chỉ mặc định.", null));
            }

            DiaChiNguoiDung address = defaultAddress.get();
            return ResponseEntity.ok(new ApiResponse(true, "Lấy địa chỉ mặc định thành công.", address));
        } catch (Exception e) {
            logger.error("Lỗi khi lấy địa chỉ mặc định: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi lấy địa chỉ mặc định: " + e.getMessage()));
        }
    }

    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;

        public ApiResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public ApiResponse(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }

    public static class VoucherResponse {
        private BigDecimal giamGiaOrder;
        private BigDecimal giamGiaShipping;

        public VoucherResponse(BigDecimal giamGiaOrder, BigDecimal giamGiaShipping) {
            this.giamGiaOrder = giamGiaOrder;
            this.giamGiaShipping = giamGiaShipping;
        }

        public BigDecimal getGiamGiaOrder() { return giamGiaOrder; }
        public void setGiamGiaOrder(BigDecimal giamGiaOrder) { this.giamGiaOrder = giamGiaOrder; }
        public BigDecimal getGiamGiaShipping() { return giamGiaShipping; }
        public void setGiamGiaShipping(BigDecimal giamGiaShipping) { this.giamGiaShipping = giamGiaShipping; }
    }
}