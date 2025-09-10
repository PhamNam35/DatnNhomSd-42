package com.example.AsmGD1.controller.HoaDon;

import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.HoaDon.HoaDonRepository;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.repository.WebKhachHang.LichSuDoiSanPhamRepository;
import com.example.AsmGD1.service.HoaDon.HoaDonService;
import com.example.AsmGD1.service.ThongBao.ThongBaoService;
import com.example.AsmGD1.service.WebKhachHang.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/acvstore/exchange-requests")
public class AdminExchangeController {

    private static final Logger logger = LoggerFactory.getLogger(AdminExchangeController.class);

    @Autowired
    private LichSuDoiSanPhamRepository lichSuDoiSanPhamRepository;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private ThongBaoService thongBaoService;

    @Autowired
    private EmailService emailService;
    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @Autowired
    private HoaDonService hoaDonService;

    @GetMapping
    public String listExchangeRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String searchMaDonHang,
            @RequestParam(required = false) String searchHoTen,
            @RequestParam(required = false) String searchTrangThai, // Thêm tham số trạng thái
            Model model) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String tenDangNhap = authentication.getName();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) auth.getPrincipal();
            model.addAttribute("user", user);
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "thoiGianDoi");
        Pageable pageable = PageRequest.of(page, 10, sort);

        Page<LichSuDoiSanPham> exchangeRequestsPage;
        if ((searchMaDonHang != null && !searchMaDonHang.isEmpty()) ||
                (searchHoTen != null && !searchHoTen.isEmpty()) ||
                (searchTrangThai != null && !searchTrangThai.isEmpty())) {
            exchangeRequestsPage = lichSuDoiSanPhamRepository.findByMaDonHangOrHoTenOrTrangThai(
                    searchMaDonHang != null ? searchMaDonHang.trim() : "",
                    searchHoTen != null ? searchHoTen.trim() : "",
                    searchTrangThai != null ? searchTrangThai.trim() : "",
                    pageable);
        } else {
            exchangeRequestsPage = lichSuDoiSanPhamRepository.findAll(pageable);
        }

        List<LichSuDoiSanPham> requests = exchangeRequestsPage.getContent();

        // Nhóm các yêu cầu theo mã đơn hàng
        List<List<LichSuDoiSanPham>> groups = new ArrayList<>();
        if (!requests.isEmpty()) {
            List<LichSuDoiSanPham> currentGroup = new ArrayList<>();
            String currentMa = requests.get(0).getHoaDon().getDonHang().getMaDonHang();
            currentGroup.add(requests.get(0));
            for (int i = 1; i < requests.size(); i++) {
                LichSuDoiSanPham r = requests.get(i);
                String ma = r.getHoaDon().getDonHang().getMaDonHang();
                if (ma.equals(currentMa)) {
                    currentGroup.add(r);
                } else {
                    groups.add(currentGroup);
                    currentGroup = new ArrayList<>();
                    currentGroup.add(r);
                    currentMa = ma;
                }
            }
            groups.add(currentGroup);
        }

        model.addAttribute("groups", groups);
        model.addAttribute("currentPage", exchangeRequestsPage.getNumber());
        model.addAttribute("totalPages", exchangeRequestsPage.getTotalPages());
        model.addAttribute("searchMaDonHang", searchMaDonHang);
        model.addAttribute("searchHoTen", searchHoTen);
        model.addAttribute("searchTrangThai", searchTrangThai); // Thêm trạng thái vào model
        return "WebQuanLy/exchange-requests";
    }

    @PostMapping("/approve/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> approveExchangeRequest(@PathVariable UUID id, Authentication authentication) {
        return approveSingle(id, authentication);
    }

    private ResponseEntity<Map<String, Object>> approveSingle(UUID id, Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Quyền
            if (authentication == null || !authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền xác nhận yêu cầu đổi hàng.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            LichSuDoiSanPham lichSu = lichSuDoiSanPhamRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Yêu cầu đổi hàng không tồn tại."));

            if (!"Chờ xử lý".equals(lichSu.getTrangThai())) {
                response.put("success", false);
                response.put("message", "Yêu cầu đổi hàng không ở trạng thái chờ xử lý.");
                return ResponseEntity.badRequest().body(response);
            }

            // Kho
            ChiTietSanPham replacementProduct = lichSu.getChiTietSanPhamThayThe();
            if (replacementProduct.getSoLuongTonKho() < lichSu.getSoLuong()) {
                response.put("success", false);
                response.put("message", "Sản phẩm thay thế không đủ tồn kho. Tồn kho hiện tại: "
                        + replacementProduct.getSoLuongTonKho() + ", Yêu cầu: " + lichSu.getSoLuong());
                return ResponseEntity.badRequest().body(response);
            }

            // Cập nhật trạng thái yêu cầu
            lichSu.setTrangThai("Đã xác nhận");
            lichSuDoiSanPhamRepository.save(lichSu);

            // Cập nhật CTĐH + kho
            ChiTietDonHang chiTietDonHang = lichSu.getChiTietDonHang();
            chiTietDonHang.setTrangThaiDoiSanPham(true);
            chiTietDonHang.setLyDoDoiHang(lichSu.getLyDoDoiHang());

            ChiTietSanPham originalProduct = chiTietDonHang.getChiTietSanPham();
            originalProduct.setSoLuongTonKho(originalProduct.getSoLuongTonKho() + lichSu.getSoLuong()); // trả hàng cũ
            replacementProduct.setSoLuongTonKho(replacementProduct.getSoLuongTonKho() - lichSu.getSoLuong()); // trừ hàng mới
            chiTietSanPhamRepository.save(originalProduct);
            chiTietSanPhamRepository.save(replacementProduct);

            // Cập nhật HĐ gốc → Đã đổi hàng (KHÓA đổi tiếp)
            HoaDon hoaDon = lichSu.getHoaDon();
            hoaDon.setTrangThai("Đã đổi hàng");
            hoaDonRepository.save(hoaDon);

            LichSuHoaDon lshd = new LichSuHoaDon();
            lshd.setHoaDon(hoaDon);
            lshd.setTrangThai("Đã đổi hàng");
            lshd.setThoiGian(LocalDateTime.now());
            lshd.setGhiChu("Admin xác nhận đổi: " + lichSu.getLyDoDoiHang());
            hoaDon.getLichSuHoaDons().add(lshd);
            hoaDonRepository.save(hoaDon);

            // Tính chênh lệch (tổng)
            BigDecimal totalOriginal = (lichSu.getTongTienHoan() != null)
                    ? lichSu.getTongTienHoan()
                    : chiTietDonHang.getGia().multiply(BigDecimal.valueOf(lichSu.getSoLuong()));
            BigDecimal totalReplacement = replacementProduct.getGia().multiply(BigDecimal.valueOf(lichSu.getSoLuong()));
            BigDecimal chenhLech = totalReplacement.subtract(totalOriginal);

            // Lấy PTTT: ưu tiên PTTT được KH chọn khi tạo yêu cầu đổi; nếu null, fallback PTTT của HĐ gốc
            PhuongThucThanhToan pttt = (lichSu.getPhuongThucThanhToan() != null)
                    ? lichSu.getPhuongThucThanhToan()
                    : hoaDon.getPhuongThucThanhToan();

            boolean daThanhToan = Boolean.TRUE.equals(lichSu.getDaThanhToanChenhLech());

            // XỬ LÝ CHÊNH LỆCH
            String moTa = "Đổi '" + chiTietDonHang.getTenSanPham() + "' → '"
                    + replacementProduct.getSanPham().getTenSanPham() + "', SL " + lichSu.getSoLuong();

            hoaDonService.xuLyChenhLechSauDuyet(
                    hoaDon,
                    chenhLech,
                    lichSu.getSoLuong(),
                    replacementProduct.getId(),
                    moTa,
                    pttt,          // ✅ đẩy PTTT
                    daThanhToan    // ✅ đẩy cờ đã thanh toán chênh lệch
            );


            // Thông báo + Email
            NguoiDung user = hoaDon.getDonHang().getNguoiDung();
            thongBaoService.taoThongBaoHeThong(
                    user.getTenDangNhap(),
                    "Yêu cầu đổi sản phẩm đã được xác nhận",
                    "Đơn " + hoaDon.getDonHang().getMaDonHang()
                            + (chenhLech.compareTo(BigDecimal.ZERO) > 0
                            ? " phát sinh phụ thu: " + String.format("%,.0f", chenhLech) + " VNĐ."
                            : (chenhLech.compareTo(BigDecimal.ZERO) < 0
                            ? " được hoàn chênh lệch: " + String.format("%,.0f", chenhLech.abs()) + " VNĐ."
                            : " không phát sinh chênh lệch.")),
                    hoaDon.getDonHang()
            );

            String emailContent = "<h2>Xác nhận yêu cầu đổi sản phẩm</h2>"
                    + "<p>Xin chào " + user.getHoTen() + ",</p>"
                    + "<p>Yêu cầu đổi cho đơn <strong>" + hoaDon.getDonHang().getMaDonHang() + "</strong> đã được xác nhận.</p>"
                    + (chenhLech.compareTo(BigDecimal.ZERO) > 0
                    ? "<p><strong>Phụ thu:</strong> " + String.format("%,.0f", chenhLech) + " VNĐ.<br>"
                    + "Hệ thống đã tạo đơn chênh lệch mới (phí ship 0đ). Vui lòng thanh toán trong mục Đơn mua.</p>"
                    : (chenhLech.compareTo(BigDecimal.ZERO) < 0
                    ? "<p><strong>Đã hoàn chênh lệch:</strong> " + String.format("%,.0f", chenhLech.abs()) + " VNĐ.</p>"
                    : "<p>Không phát sinh chênh lệch.</p>")
            );
            emailService.sendEmail(user.getEmail(), "Xác nhận đổi sản phẩm - ACV Store", emailContent);

            response.put("success", true);
            response.put("message", "Yêu cầu đổi sản phẩm đã được xác nhận.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Lỗi khi xác nhận yêu cầu đổi sản phẩm: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi xác nhận yêu cầu. Vui lòng thử lại sau.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/approve-all/{hoaDonId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> approveAllExchangeRequests(@PathVariable UUID hoaDonId, Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Quyền
            if (authentication == null || !authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền xác nhận yêu cầu đổi hàng.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            HoaDon hoaDon = hoaDonRepository.findById(hoaDonId)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại."));

            List<LichSuDoiSanPham> pendingRequests = lichSuDoiSanPhamRepository.findByHoaDonAndTrangThai(hoaDon, "Chờ xử lý");

            if (pendingRequests.isEmpty()) {
                response.put("success", false);
                response.put("message", "Không có yêu cầu đổi hàng chờ xử lý cho hóa đơn này.");
                return ResponseEntity.badRequest().body(response);
            }

            // Kiểm tra tồn kho cho tất cả
            for (LichSuDoiSanPham lichSu : pendingRequests) {
                ChiTietSanPham replacementProduct = lichSu.getChiTietSanPhamThayThe();
                if (replacementProduct.getSoLuongTonKho() < lichSu.getSoLuong()) {
                    response.put("success", false);
                    response.put("message", "Sản phẩm thay thế không đủ tồn kho cho yêu cầu: " + lichSu.getId());
                    return ResponseEntity.badRequest().body(response);
                }
            }

            BigDecimal totalChenhLech = BigDecimal.ZERO;
            StringBuilder moTaBuilder = new StringBuilder("Đổi hàng nhóm: ");

            for (LichSuDoiSanPham lichSu : pendingRequests) {
                // Xử lý từng yêu cầu giống approve single, nhưng gộp chênh lệch
                lichSu.setTrangThai("Đã xác nhận");
                lichSuDoiSanPhamRepository.save(lichSu);

                ChiTietDonHang chiTietDonHang = lichSu.getChiTietDonHang();
                chiTietDonHang.setTrangThaiDoiSanPham(true);
                chiTietDonHang.setLyDoDoiHang(lichSu.getLyDoDoiHang());

                ChiTietSanPham originalProduct = chiTietDonHang.getChiTietSanPham();
                ChiTietSanPham replacementProduct = lichSu.getChiTietSanPhamThayThe();

                originalProduct.setSoLuongTonKho(originalProduct.getSoLuongTonKho() + lichSu.getSoLuong());
                replacementProduct.setSoLuongTonKho(replacementProduct.getSoLuongTonKho() - lichSu.getSoLuong());
                chiTietSanPhamRepository.save(originalProduct);
                chiTietSanPhamRepository.save(replacementProduct);

                BigDecimal totalOriginal = (lichSu.getTongTienHoan() != null)
                        ? lichSu.getTongTienHoan()
                        : chiTietDonHang.getGia().multiply(BigDecimal.valueOf(lichSu.getSoLuong()));
                BigDecimal totalReplacement = replacementProduct.getGia().multiply(BigDecimal.valueOf(lichSu.getSoLuong()));
                BigDecimal chenhLech = totalReplacement.subtract(totalOriginal);
                totalChenhLech = totalChenhLech.add(chenhLech);

                moTaBuilder.append("\n- Đổi '").append(chiTietDonHang.getTenSanPham())
                        .append("' → '").append(replacementProduct.getSanPham().getTenSanPham())
                        .append("', SL ").append(lichSu.getSoLuong());
            }

            // Cập nhật hóa đơn
            hoaDon.setTrangThai("Đã đổi hàng");
            hoaDonRepository.save(hoaDon);

            LichSuHoaDon lshd = new LichSuHoaDon();
            lshd.setHoaDon(hoaDon);
            lshd.setTrangThai("Đã đổi hàng");
            lshd.setThoiGian(LocalDateTime.now());
            lshd.setGhiChu("Admin xác nhận đổi nhóm");
            hoaDon.getLichSuHoaDons().add(lshd);
            hoaDonRepository.save(hoaDon);

            // Xử lý chênh lệch tổng
            // Tạo 1 hoá đơn phụ thu chứa nhiều sản phẩm (mỗi lịch sử đổi là 1 dòng – chỉ lấy dòng chênh lệch dương)
            hoaDonService.taoHoaDonPhuThuNhieuDong(hoaDon, pendingRequests);


            // Thông báo + Email
            NguoiDung user = hoaDon.getDonHang().getNguoiDung();
            thongBaoService.taoThongBaoHeThong(
                    user.getTenDangNhap(),
                    "Tất cả yêu cầu đổi sản phẩm đã được xác nhận",
                    "Đơn " + hoaDon.getDonHang().getMaDonHang()
                            + (totalChenhLech.compareTo(BigDecimal.ZERO) > 0
                            ? " phát sinh phụ thu: " + String.format("%,.0f", totalChenhLech) + " VNĐ."
                            : (totalChenhLech.compareTo(BigDecimal.ZERO) < 0
                            ? " được hoàn chênh lệch: " + String.format("%,.0f", totalChenhLech.abs()) + " VNĐ."
                            : " không phát sinh chênh lệch.")),
                    hoaDon.getDonHang()
            );

            String emailContent = "<h2>Xác nhận tất cả yêu cầu đổi sản phẩm</h2>"
                    + "<p>Xin chào " + user.getHoTen() + ",</p>"
                    + "<p>Tất cả yêu cầu đổi cho đơn <strong>" + hoaDon.getDonHang().getMaDonHang() + "</strong> đã được xác nhận.</p>"
                    + (totalChenhLech.compareTo(BigDecimal.ZERO) > 0
                    ? "<p><strong>Phụ thu tổng:</strong> " + String.format("%,.0f", totalChenhLech) + " VNĐ.<br>"
                    + "Hệ thống đã tạo đơn chênh lệch mới (phí ship 0đ). Vui lòng thanh toán trong mục Đơn mua.</p>"
                    : (totalChenhLech.compareTo(BigDecimal.ZERO) < 0
                    ? "<p><strong>Đã hoàn chênh lệch tổng:</strong> " + String.format("%,.0f", totalChenhLech.abs()) + " VNĐ.</p>"
                    : "<p>Không phát sinh chênh lệch.</p>")
            );
            emailService.sendEmail(user.getEmail(), "Xác nhận đổi sản phẩm - ACV Store", emailContent);

            response.put("success", true);
            response.put("message", "Tất cả yêu cầu đổi sản phẩm đã được xác nhận.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Lỗi khi xác nhận tất cả yêu cầu đổi sản phẩm: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi xác nhận. Vui lòng thử lại sau.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/reject/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> rejectExchangeRequest(@PathVariable UUID id,
                                                                     @RequestParam(required = false) String rejectReason,
                                                                     Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Kiểm tra quyền admin
            if (authentication == null || !authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền hủy yêu cầu đổi hàng.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            LichSuDoiSanPham lichSu = lichSuDoiSanPhamRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Yêu cầu đổi hàng không tồn tại."));

            if (!"Chờ xử lý".equals(lichSu.getTrangThai())) {
                response.put("success", false);
                response.put("message", "Yêu cầu đổi hàng không ở trạng thái chờ xử lý.");
                return ResponseEntity.badRequest().body(response);
            }

            // Cập nhật trạng thái yêu cầu đổi hàng
            lichSu.setTrangThai("Đã hủy");
            lichSuDoiSanPhamRepository.save(lichSu);

            // Cập nhật trạng thái hóa đơn về trạng thái trước đó (Vận chuyển thành công)
            HoaDon hoaDon = lichSu.getHoaDon();
            hoaDon.setTrangThai("Vận chuyển thành công");
            hoaDonRepository.save(hoaDon);

            // Thêm lịch sử hóa đơn
            LichSuHoaDon lichSuHoaDon = new LichSuHoaDon();
            lichSuHoaDon.setHoaDon(hoaDon);
            lichSuHoaDon.setTrangThai("Vận chuyển thành công");
            lichSuHoaDon.setThoiGian(LocalDateTime.now());
            lichSuHoaDon.setGhiChu("Admin đã hủy yêu cầu đổi sản phẩm: " + (rejectReason != null ? rejectReason : "Không có lý do cụ thể"));
            hoaDon.getLichSuHoaDons().add(lichSuHoaDon);
            hoaDonRepository.save(hoaDon);

            // Gửi thông báo cho khách hàng
            NguoiDung user = hoaDon.getDonHang().getNguoiDung();
            String emailContent = "<h2>Hủy yêu cầu đổi sản phẩm</h2>" +
                    "<p>Xin chào " + user.getHoTen() + ",</p>" +
                    "<p>Yêu cầu đổi sản phẩm của bạn cho đơn hàng mã <strong>" + hoaDon.getDonHang().getMaDonHang() + "</strong> đã bị hủy.</p>" +
                    "<p><strong>Lý do:</strong> " + (rejectReason != null ? rejectReason : "Không có lý do cụ thể") + "</p>" +
                    "<p>Vui lòng liên hệ đội ngũ hỗ trợ nếu bạn cần thêm thông tin.</p>" +
                    "<p>Trân trọng,<br>Đội ngũ ACV Store</p>";
            emailService.sendEmail(user.getEmail(), "Hủy yêu cầu đổi sản phẩm - ACV Store", emailContent);

            response.put("success", true);
            response.put("message", "Yêu cầu đổi sản phẩm đã bị hủy.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Lỗi khi hủy yêu cầu đổi sản phẩm: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi hủy yêu cầu. Vui lòng thử lại sau.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private String formatVND(double value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        return formatter.format(value) + " VNĐ";
    }
}