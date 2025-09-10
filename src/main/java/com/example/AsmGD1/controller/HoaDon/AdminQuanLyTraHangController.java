package com.example.AsmGD1.controller.HoaDon;

import com.example.AsmGD1.dto.HoaDonTraHangDTO;
import com.example.AsmGD1.dto.LichSuTraHangDTO;
import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.BanHang.DonHangPhieuGiamGiaRepository;
import com.example.AsmGD1.repository.BanHang.DonHangRepository;
import com.example.AsmGD1.repository.HoaDon.HoaDonRepository;
import com.example.AsmGD1.repository.HoaDon.LichSuHoaDonRepository;
import com.example.AsmGD1.repository.HoaDon.LichSuTraHangRepository;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.repository.ViThanhToan.LichSuGiaoDichViRepository;
import com.example.AsmGD1.repository.ViThanhToan.ViThanhToanRepository;
import com.example.AsmGD1.service.GiamGia.ChienDichGiamGiaService;
import com.example.AsmGD1.service.HoaDon.HoaDonService;
import com.example.AsmGD1.service.ThongBao.ThongBaoService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/acvstore/quan-ly-tra-hang")
@PreAuthorize("hasRole('ADMIN')")
public class AdminQuanLyTraHangController {

    private static final Logger logger = LoggerFactory.getLogger(AdminQuanLyTraHangController.class);

    @Autowired
    private LichSuTraHangRepository lichSuTraHangRepository;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private HoaDonService hoaDonService;

    @Autowired
    private DonHangRepository donHangRepository;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @Autowired
    private LichSuHoaDonRepository lichSuHoaDonRepository;

    @Autowired
    private ThongBaoService thongBaoService;

    @Autowired
    private ChienDichGiamGiaService chienDichGiamGiaService;
    @Autowired
    private ViThanhToanRepository viThanhToanRepository;
    @Autowired
    private LichSuGiaoDichViRepository lichSuGiaoDichViRepository;
    @Autowired
    private DonHangPhieuGiamGiaRepository donHangPhieuGiamGiaRepository;

    // Trang danh sách yêu cầu trả hàng
    @GetMapping
    public String danhSachTraHang(
            @RequestParam(name = "status", defaultValue = "cho-xac-nhan") String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/dang-nhap";
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "thoiGianTra");
        Pageable pageable = PageRequest.of(page, 10, sort);
        Page<LichSuTraHang> traHangPage;

        if ("tat-ca".equalsIgnoreCase(status)) {
            traHangPage = lichSuTraHangRepository.findAll(pageable);
        } else {
            String trangThaiDb = switch (status) {
                case "cho-xac-nhan" -> "Chờ xác nhận";
                case "da-xac-nhan" -> "Đã xác nhận";
                case "da-huy" -> "Đã hủy";
                default -> "Chờ xác nhận";
            };
            traHangPage = lichSuTraHangRepository.findByTrangThai(trangThaiDb, pageable);
        }

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);

        // Nhóm các yêu cầu trả hàng theo HoaDon
        Map<HoaDon, List<LichSuTraHang>> groupedByHoaDon = traHangPage.getContent().stream()
                .collect(Collectors.groupingBy(LichSuTraHang::getHoaDon));

        List<HoaDonTraHangDTO> formattedList = groupedByHoaDon.entrySet().stream()
                .map(entry -> {
                    HoaDon hoaDon = entry.getKey();
                    List<LichSuTraHang> traHangs = entry.getValue();

                    // Tính toán thông tin tổng hợp
                    List<String> tenSanPhams = traHangs.stream()
                            .map(traHang -> traHang.getChiTietDonHang().getChiTietSanPham().getSanPham().getTenSanPham())
                            .collect(Collectors.toList());

                    int tongSoLuong = traHangs.stream()
                            .mapToInt(LichSuTraHang::getSoLuong)
                            .sum();

                    BigDecimal tongTienHoan = BigDecimal.ZERO;
                    for (LichSuTraHang traHang : traHangs) {
                        ChiTietDonHang chiTiet = traHang.getChiTietDonHang();
                        List<DonHangPhieuGiamGia> allVouchers = donHangPhieuGiamGiaRepository.findByDonHang_IdOrderByThoiGianApDungAsc(hoaDon.getDonHang().getId());
                        List<DonHangPhieuGiamGia> voucherOrder = allVouchers.stream()
                                .filter(p -> "ORDER".equalsIgnoreCase(p.getLoaiGiamGia()))
                                .collect(Collectors.toList());

                        BigDecimal tongGiamOrder = voucherOrder.stream()
                                .map(DonHangPhieuGiamGia::getGiaTriGiam)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        BigDecimal tongTienHangTra = chiTiet.getThanhTien() != null ? chiTiet.getThanhTien() : BigDecimal.ZERO;
                        BigDecimal tongTienGocHoaDon = hoaDon.getDonHang().getChiTietDonHangs().stream()
                                .filter(item -> item.getThanhTien() != null)
                                .map(ChiTietDonHang::getThanhTien)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        BigDecimal tyLeHoanTra = tongTienGocHoaDon.compareTo(BigDecimal.ZERO) > 0
                                ? tongTienHangTra.divide(tongTienGocHoaDon, 4, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                        BigDecimal giamGiaOrderHoanTra = tongGiamOrder.multiply(tyLeHoanTra).setScale(0, RoundingMode.HALF_UP);
                        BigDecimal tienHoanItem = tongTienHangTra.subtract(giamGiaOrderHoanTra);
                        if (tienHoanItem.compareTo(BigDecimal.ZERO) < 0) {
                            tienHoanItem = BigDecimal.ZERO;
                        }
                        tongTienHoan = tongTienHoan.add(tienHoanItem);
                    }

                    String formattedTongHoan = formatter.format(tongTienHoan) + " VNĐ";
                    String lyDoTraHang = traHangs.stream()
                            .map(LichSuTraHang::getLyDoTraHang)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse("Không có lý do");

                    String trangThai = traHangs.get(0).getTrangThai(); // Lấy trạng thái từ yêu cầu đầu tiên
                    LocalDateTime thoiGianTra = traHangs.get(0).getThoiGianTra(); // Lấy thời gian từ yêu cầu đầu tiên
                    UUID id = traHangs.get(0).getId(); // Lấy ID của yêu cầu đầu tiên để liên kết tới chi tiết

                    return new HoaDonTraHangDTO(
                            hoaDon.getDonHang().getMaDonHang(),
                            hoaDon.getDonHang().getNguoiDung().getHoTen(),
                            tenSanPhams,
                            tongSoLuong,
                            tongTienHoan,
                            formattedTongHoan,
                            lyDoTraHang,
                            trangThai,
                            thoiGianTra,
                            id
                    );
                })
                .collect(Collectors.toList());

        model.addAttribute("danhSachTraHang", formattedList);
        model.addAttribute("currentPage", traHangPage.getNumber());
        model.addAttribute("totalPages", traHangPage.getTotalPages());
        model.addAttribute("status", status);
        return "WebQuanLy/quan-ly-tra-hang/danh-sach";
    }

    private String formatCurrency(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        return formatter.format(amount);
    }

    // Trang chi tiết yêu cầu trả hàng
    @GetMapping("/chi-tiet/{id}")
    public String chiTietTraHang(@PathVariable("id") UUID id, Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/dang-nhap";
        }

        try {
            Optional<LichSuTraHang> lichSuOpt = lichSuTraHangRepository.findById(id);
            if (lichSuOpt.isEmpty()) {
                model.addAttribute("danhSachLichSuTraHang", null);
                return "WebQuanLy/quan-ly-tra-hang/chi-tiet";
            }

            LichSuTraHang lichSu = lichSuOpt.get();

            // Kiểm tra null safety
            if (lichSu.getHoaDon() == null) {
                logger.warn("LichSuTraHang {} thiếu thông tin hoaDon", id);
                model.addAttribute("danhSachLichSuTraHang", null);
                return "WebQuanLy/quan-ly-tra-hang/chi-tiet";
            }

            HoaDon hoaDon = lichSu.getHoaDon();

            // Lấy tất cả yêu cầu trả hàng cho hóa đơn này (bất kể trạng thái)
            List<LichSuTraHang> danhSachLichSuTraHang = lichSuTraHangRepository.findByHoaDon(hoaDon);
            if (danhSachLichSuTraHang.isEmpty()) {
                model.addAttribute("danhSachLichSuTraHang", null);
                return "WebQuanLy/quan-ly-tra-hang/chi-tiet";
            }

            // Định dạng tiền tệ
            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setGroupingSeparator('.');
            DecimalFormat formatter = new DecimalFormat("#,###", symbols);

            // Format giá sản phẩm cho từng item
            for (LichSuTraHang item : danhSachLichSuTraHang) {
                ChiTietDonHang chiTiet = item.getChiTietDonHang();
                if (chiTiet == null) continue;

                if (chiTiet.getGia() != null) {
                    try {
                        chiTiet.setFormattedGia(formatter.format(chiTiet.getGia()));
                    } catch (Exception e) {
                        logger.error("Lỗi format giá sản phẩm: {}", e.getMessage());
                        chiTiet.setFormattedGia("0");
                    }
                } else {
                    chiTiet.setFormattedGia("0");
                }

                // Xử lý giảm giá sản phẩm
                ChiTietSanPham chiTietSanPham = chiTiet.getChiTietSanPham();
                if (chiTietSanPham != null && chiTietSanPham.getGia() != null) {
                    try {
                        Optional<ChienDichGiamGia> activeCampaign = chienDichGiamGiaService.getActiveCampaignForProductDetail(chiTietSanPham.getId());
                        activeCampaign.ifPresent(campaign -> {
                            BigDecimal discount = chiTietSanPham.getGia()
                                    .multiply(campaign.getPhanTramGiam())
                                    .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
                            BigDecimal giaSauGiam = chiTietSanPham.getGia().subtract(discount);
                            chiTiet.setFormattedGia(formatter.format(giaSauGiam));
                        });
                    } catch (Exception e) {
                        logger.error("Lỗi xử lý giảm giá: {}", e.getMessage());
                    }
                }
            }

            // Tính tổng giảm giá từ phiếu giảm giá
            List<DonHangPhieuGiamGia> allVouchers = donHangPhieuGiamGiaRepository.findByDonHang_IdOrderByThoiGianApDungAsc(hoaDon.getDonHang().getId());

            List<DonHangPhieuGiamGia> voucherOrder = allVouchers.stream()
                    .filter(p -> "ORDER".equalsIgnoreCase(p.getLoaiGiamGia()))
                    .toList();

            BigDecimal tongGiamOrder = voucherOrder.stream()
                    .map(DonHangPhieuGiamGia::getGiaTriGiam)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal tongGiamShip = BigDecimal.ZERO; // Không sử dụng phí ship

            // Tính và format tổng hoàn tiền ước tính cho tất cả items
            String formattedTongHoan = "0";
            try {
                BigDecimal tongTienHoan = BigDecimal.ZERO;

                // Kiểm tra trạng thái (lấy từ item đầu tiên vì tất cả cùng trạng thái)
                if ("Đã xác nhận".equals(danhSachLichSuTraHang.get(0).getTrangThai())) {
                    // Đã xác nhận - sum tongTienHoan từ tất cả
                    tongTienHoan = danhSachLichSuTraHang.stream()
                            .map(LichSuTraHang::getTongTienHoan)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                } else {
                    // Chưa xác nhận - tính ước tính tổng
                    BigDecimal tongTienHangTra = danhSachLichSuTraHang.stream()
                            .map(LichSuTraHang::getChiTietDonHang)
                            .filter(Objects::nonNull)
                            .map(ChiTietDonHang::getThanhTien)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal tongTienGocHoaDon = hoaDon.getDonHang().getChiTietDonHangs().stream()
                            .filter(item -> item.getThanhTien() != null)
                            .map(ChiTietDonHang::getThanhTien)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal tyLeHoanTra = tongTienGocHoaDon.compareTo(BigDecimal.ZERO) > 0
                            ? tongTienHangTra.divide(tongTienGocHoaDon, 4, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    BigDecimal giamGiaOrderHoanTra = tongGiamOrder.multiply(tyLeHoanTra).setScale(0, RoundingMode.HALF_UP);
                    tongTienHoan = tongTienHangTra.subtract(giamGiaOrderHoanTra);
                    if (tongTienHoan.compareTo(BigDecimal.ZERO) < 0) {
                        tongTienHoan = BigDecimal.ZERO;
                    }
                }

                formattedTongHoan = formatter.format(tongTienHoan) + " VNĐ";
            } catch (Exception e) {
                logger.error("Lỗi format tổng hoàn: {}", e.getMessage());
                formattedTongHoan = "0 VNĐ";
            }

            // Thêm thông tin giảm giá vào model
            model.addAttribute("tongGiamOrder", formatter.format(tongGiamOrder) + " VNĐ");
            model.addAttribute("tongGiamShip", formatter.format(tongGiamShip) + " VNĐ");
            model.addAttribute("danhSachLichSuTraHang", danhSachLichSuTraHang);
            model.addAttribute("hoaDon", hoaDon);
            model.addAttribute("formattedTongHoan", formattedTongHoan);

            return "WebQuanLy/quan-ly-tra-hang/chi-tiet";

        } catch (Exception e) {
            logger.error("Lỗi không mong muốn trong chiTietTraHang: {}", e.getMessage(), e);
            model.addAttribute("danhSachLichSuTraHang", null);
            return "WebQuanLy/quan-ly-tra-hang/chi-tiet";
        }
    }

    // API xác nhận trả hàng
    @PostMapping("/api/xac-nhan/{id}")
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> xacNhanTraHang(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "Vui lòng đăng nhập để xác nhận.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Optional<LichSuTraHang> lichSuOpt = lichSuTraHangRepository.findById(id);
            if (lichSuOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Không tìm thấy yêu cầu trả hàng.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            LichSuTraHang lichSu = lichSuOpt.get();
            if (!"Chờ xác nhận".equals(lichSu.getTrangThai())) {
                response.put("success", false);
                response.put("message", "Yêu cầu này không ở trạng thái chờ xác nhận.");
                return ResponseEntity.badRequest().body(response);
            }

            HoaDon hoaDon = lichSu.getHoaDon();

            // Lấy tất cả yêu cầu chờ xác nhận cho hóa đơn này để gộp xác nhận
            List<LichSuTraHang> pendingReturns = lichSuTraHangRepository.findByHoaDonAndTrangThai(hoaDon, "Chờ xác nhận");
            if (pendingReturns.isEmpty()) {
                response.put("success", false);
                response.put("message", "Không có yêu cầu chờ xác nhận.");
                return ResponseEntity.badRequest().body(response);
            }

            NguoiDung nguoiDung = hoaDon.getDonHang().getNguoiDung();

            // 1. Tính toán tiền hoàn thực tế cho tất cả
            BigDecimal tongTienHoanThucTe = BigDecimal.ZERO;
            try {
                BigDecimal tongTienHangTra = pendingReturns.stream()
                        .map(LichSuTraHang::getChiTietDonHang)
                        .filter(Objects::nonNull)
                        .map(ChiTietDonHang::getThanhTien)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal tongTienGocHoaDon = hoaDon.getDonHang().getChiTietDonHangs().stream()
                        .filter(item -> item.getThanhTien() != null)
                        .map(ChiTietDonHang::getThanhTien)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal tyLeHoanTra = tongTienGocHoaDon.compareTo(BigDecimal.ZERO) > 0
                        ? tongTienHangTra.divide(tongTienGocHoaDon, 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                // Lấy chi tiết phiếu giảm giá
                List<DonHangPhieuGiamGia> allVouchers = donHangPhieuGiamGiaRepository.findByDonHang_IdOrderByThoiGianApDungAsc(hoaDon.getDonHang().getId());

                List<DonHangPhieuGiamGia> voucherOrder = allVouchers.stream()
                        .filter(p -> "ORDER".equalsIgnoreCase(p.getLoaiGiamGia()))
                        .toList();

                // Tính tổng giảm giá
                BigDecimal tongGiamOrder = voucherOrder.stream()
                        .map(DonHangPhieuGiamGia::getGiaTriGiam)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Phân bổ giảm giá
                BigDecimal giamGiaOrderHoanTra = tongGiamOrder.multiply(tyLeHoanTra).setScale(0, RoundingMode.HALF_UP);

                // Tổng tiền hoàn
                tongTienHoanThucTe = tongTienHangTra.subtract(giamGiaOrderHoanTra);
                if (tongTienHoanThucTe.compareTo(BigDecimal.ZERO) < 0) {
                    tongTienHoanThucTe = BigDecimal.ZERO;
                }

                // Phân bổ tiền hoàn cho từng item (để lưu vào entity)
                for (LichSuTraHang item : pendingReturns) {
                    ChiTietDonHang chiTietDonHang = item.getChiTietDonHang();
                    if (chiTietDonHang.getThanhTien() == null) continue;

                    BigDecimal tyLeItem = tongTienHangTra.compareTo(BigDecimal.ZERO) > 0
                            ? chiTietDonHang.getThanhTien().divide(tongTienHangTra, 4, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    BigDecimal giamItem = giamGiaOrderHoanTra.multiply(tyLeItem).setScale(0, RoundingMode.HALF_UP);
                    BigDecimal hoanItem = chiTietDonHang.getThanhTien().subtract(giamItem);
                    if (hoanItem.compareTo(BigDecimal.ZERO) < 0) {
                        hoanItem = BigDecimal.ZERO;
                    }
                    item.setTongTienHoan(hoanItem);
                }
            } catch (Exception e) {
                logger.error("Lỗi tính toán tiền hoàn: {}", e.getMessage());
                response.put("success", false);
                response.put("message", "Lỗi tính toán tiền hoàn.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            // 2. Cập nhật tồn kho và trạng thái cho từng item
            for (LichSuTraHang item : pendingReturns) {
                ChiTietDonHang chiTietDonHang = item.getChiTietDonHang();
                ChiTietSanPham chiTietSanPham = chiTietDonHang.getChiTietSanPham();
                if (chiTietSanPham != null) {
                    chiTietSanPham.setSoLuongTonKho(chiTietSanPham.getSoLuongTonKho() + item.getSoLuong());
                    chiTietSanPhamRepository.save(chiTietSanPham);
                }

                // Set trạng thái hoàn tra cho chi tiết đơn hàng
                chiTietDonHang.setTrangThaiHoanTra(true);
                // Giả sử có repository cho ChiTietDonHang, save nó (thêm nếu cần)
                // chiTietDonHangRepository.save(chiTietDonHang); // Nếu có repository riêng

                item.setTrangThai("Đã xác nhận");
                item.setThoiGianXacNhan(LocalDateTime.now());
                lichSuTraHangRepository.save(item);
            }

            // Flush để đảm bảo dữ liệu được commit
            lichSuTraHangRepository.flush();

            // 3. Hoàn tiền vào ví (nếu cần)
            String pttt = hoaDon.getPhuongThucThanhToan() != null
                    ? hoaDon.getPhuongThucThanhToan().getTenPhuongThuc().trim()
                    : "";

            if (tongTienHoanThucTe.compareTo(BigDecimal.ZERO) > 0 &&
                    List.of("Ví Thanh Toán", "Ví", "Chuyển khoản", "Tiền mặt").contains(pttt)) {
                try {
                    Optional<ViThanhToan> viOpt = viThanhToanRepository.findByNguoiDung(nguoiDung);
                    if (viOpt.isPresent()) {
                        ViThanhToan viThanhToan = viOpt.get();
                        viThanhToan.setSoDu(viThanhToan.getSoDu().add(tongTienHoanThucTe));
                        viThanhToan.setThoiGianCapNhat(LocalDateTime.now());
                        viThanhToanRepository.save(viThanhToan);

                        LichSuGiaoDichVi lichSuGiaoDich = new LichSuGiaoDichVi();
                        lichSuGiaoDich.setIdViThanhToan(viThanhToan.getId());
                        lichSuGiaoDich.setLoaiGiaoDich("Hoàn tiền");
                        lichSuGiaoDich.setSoTien(tongTienHoanThucTe);
                        lichSuGiaoDich.setMoTa("Hoàn tiền trả hàng cho đơn hàng: " + hoaDon.getDonHang().getMaDonHang());
                        lichSuGiaoDich.setCreatedAt(LocalDateTime.now());
                        lichSuGiaoDich.setThoiGianGiaoDich(LocalDateTime.now());
                        lichSuGiaoDichViRepository.save(lichSuGiaoDich);
                    }
                } catch (Exception e) {
                    logger.error("Lỗi cập nhật ví: {}", e.getMessage());
                }
            }

            // 4. Cập nhật hóa đơn
            List<ChiTietDonHang> chiTietDonHangs = hoaDon.getDonHang().getChiTietDonHangs();
            long totalItems = chiTietDonHangs.size();
            long returnedItems = chiTietDonHangs.stream()
                    .filter(item -> Boolean.TRUE.equals(item.getTrangThaiHoanTra()))
                    .count();

            String trangThaiTraHang = returnedItems == totalItems ? "Đã trả hàng" : "Đã trả hàng một phần";

            BigDecimal tongTienMoi = hoaDon.getTongTien().subtract(tongTienHoanThucTe);
            if (tongTienMoi.compareTo(BigDecimal.ZERO) < 0) {
                tongTienMoi = BigDecimal.ZERO;
            }

            hoaDon.setTongTien(tongTienMoi);
            hoaDon.setTrangThai(trangThaiTraHang);
            hoaDonRepository.save(hoaDon);

            // 5. Lịch sử hóa đơn
            LichSuHoaDon lichSuHoaDon = new LichSuHoaDon();
            lichSuHoaDon.setHoaDon(hoaDon);
            lichSuHoaDon.setTrangThai(trangThaiTraHang);
            lichSuHoaDon.setThoiGian(LocalDateTime.now());
            lichSuHoaDon.setGhiChu("Admin xác nhận trả hàng: " + request.getOrDefault("ghiChu", "Không có ghi chú") +
                    ". Tổng tiền hoàn: " + formatCurrency(tongTienHoanThucTe));
            lichSuHoaDonRepository.save(lichSuHoaDon);

            // 6. Thông báo
            try {
                thongBaoService.taoThongBaoHeThong(
                        nguoiDung.getTenDangNhap(),
                        "Yêu cầu trả hàng được xác nhận",
                        "Yêu cầu trả hàng cho đơn hàng " + hoaDon.getDonHang().getMaDonHang() +
                                " đã được xác nhận. Tổng tiền hoàn: " + formatCurrency(tongTienHoanThucTe) +
                                " đã được cộng vào ví của bạn.",
                        hoaDon.getDonHang()
                );
            } catch (Exception e) {
                logger.error("Lỗi gửi thông báo: {}", e.getMessage());
            }

            response.put("success", true);
            response.put("message", "Đã xác nhận trả hàng thành công. Tồn kho và ví khách hàng đã được cập nhật.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Lỗi không mong muốn khi xác nhận trả hàng: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Lỗi hệ thống khi xác nhận: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // API hủy yêu cầu trả hàng (nếu cần)
    @PostMapping("/api/huy/{id}")
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> huyTraHang(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        // Tương tự kiểm tra authentication...

        Optional<LichSuTraHang> lichSuOpt = lichSuTraHangRepository.findById(id);
        if (lichSuOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Không tìm thấy yêu cầu trả hàng.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        LichSuTraHang lichSu = lichSuOpt.get();
        if (!"Chờ xác nhận".equals(lichSu.getTrangThai())) {
            response.put("success", false);
            response.put("message", "Yêu cầu này không ở trạng thái chờ xác nhận.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            lichSu.setTrangThai("Đã hủy");
            lichSu.setThoiGianXacNhan(LocalDateTime.now());
            lichSuTraHangRepository.save(lichSu);

            // Cập nhật trạng thái hóa đơn nếu cần (ví dụ: quay về "Hoàn thành")
            HoaDon hoaDon = lichSu.getHoaDon();
            hoaDon.setTrangThai("Hoàn thành");
            hoaDonRepository.save(hoaDon);

            // Thêm lịch sử
            LichSuHoaDon lichSuHoaDon = new LichSuHoaDon();
            lichSuHoaDon.setHoaDon(hoaDon);
            lichSuHoaDon.setTrangThai("Hoàn thành");
            lichSuHoaDon.setThoiGian(LocalDateTime.now());
            lichSuHoaDon.setGhiChu("Admin hủy yêu cầu trả hàng: " + request.getOrDefault("ghiChu", "Không có ghi chú"));
            lichSuHoaDonRepository.save(lichSuHoaDon);

            // Thông báo khách hàng
            thongBaoService.taoThongBaoHeThong(
                    hoaDon.getDonHang().getNguoiDung().getTenDangNhap(),
                    "Yêu cầu trả hàng bị hủy",
                    "Yêu cầu trả hàng cho đơn hàng " + hoaDon.getDonHang().getMaDonHang() + " đã bị hủy bởi admin.",
                    hoaDon.getDonHang()
            );

            response.put("success", true);
            response.put("message", "Đã hủy yêu cầu trả hàng thành công.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Lỗi khi hủy trả hàng: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Lỗi khi hủy: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}