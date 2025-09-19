package com.example.AsmGD1.controller.GiamGia;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.PhieuGiamGia;
import com.example.AsmGD1.entity.PhuongThucThanhToan;
import com.example.AsmGD1.repository.BanHang.PhuongThucThanhToanRepository;
import com.example.AsmGD1.repository.GiamGia.PhieuGiamGiaRepository;
import com.example.AsmGD1.repository.GiamGia.PhieuGiamGiaSpecification;
import com.example.AsmGD1.service.GiamGia.GuiMailService;
import com.example.AsmGD1.service.GiamGia.PhieuGiamGiaCuaNguoiDungService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/polyshoe/phieu-giam-gia")
public class PhieuGiamGiaController {

    @Autowired
    private PhieuGiamGiaRepository phieuGiamGiaRepository;

    @Autowired
    private PhieuGiamGiaCuaNguoiDungService phieuService;

    @Autowired
    private GuiMailService guiMailService;

    @Autowired
    private PhuongThucThanhToanRepository phuongThucThanhToanRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // ===== RBAC =====
    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung user) {
            return "ADMIN".equalsIgnoreCase(user.getVaiTro());
        }
        return false;
    }

    private boolean isCurrentUserEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung user) {
            return "EMPLOYEE".equalsIgnoreCase(user.getVaiTro());
        }
        return false;
    }

    private void addUserInfoToModel(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung user) {
            model.addAttribute("user", user);
            model.addAttribute("isAdmin", "ADMIN".equalsIgnoreCase(user.getVaiTro()));
            model.addAttribute("isEmployee", "EMPLOYEE".equalsIgnoreCase(user.getVaiTro()));
        } else {
            NguoiDung defaultUser = new NguoiDung();
            defaultUser.setTenDangNhap("Unknown");
            defaultUser.setVaiTro("GUEST");
            model.addAttribute("user", defaultUser);
            model.addAttribute("isAdmin", false);
            model.addAttribute("isEmployee", false);
        }
    }

    // ===== List =====
    @GetMapping
    public String list(@RequestParam(required = false) String search,
                       @RequestParam(required = false) String fromDate,
                       @RequestParam(required = false) String toDate,
                       @RequestParam(required = false) String type,
                       @RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "5") int size,
                       Model model) {

        if (!isCurrentUserAdmin() && !isCurrentUserEmployee()) {
            return "redirect:/login";
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "thoiGianTao")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        Pageable pageable = PageRequest.of(page, size, sort);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        LocalDateTime from = (fromDate != null && !fromDate.isEmpty()) ? LocalDateTime.parse(fromDate, formatter) : null;
        LocalDateTime to = (toDate != null && !toDate.isEmpty()) ? LocalDateTime.parse(toDate, formatter) : null;

        Page<PhieuGiamGia> pageResult = phieuGiamGiaRepository.findAll(
                PhieuGiamGiaSpecification.filter(search, type, status, from, to), pageable
        );

        List<PhieuGiamGia> vouchers = pageResult.getContent();
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        Map<UUID, Map<String, String>> formats = new HashMap<>();
        for (PhieuGiamGia v : vouchers) {
            Map<String, String> map = new HashMap<>();
            if ("SHIPPING".equalsIgnoreCase(v.getPhamViApDung())) {
                if ("FREESHIP_FULL".equalsIgnoreCase(v.getLoai())) {
                    map.put("giaTriGiam", "Freeship toàn phần");
                } else if ("FREESHIP_CAP".equalsIgnoreCase(v.getLoai())) {
                    String cap = v.getGiaTriGiamToiDa() != null ? nf.format(v.getGiaTriGiamToiDa()) + " ₫" : "0 ₫";
                    map.put("giaTriGiam", "Giảm phí ship tối đa " + cap);
                } else {
                    map.put("giaTriGiam", "-");
                }
            } else {
                String suffix = "PERCENT".equalsIgnoreCase(v.getLoai()) ? " %" : " ₫";
                map.put("giaTriGiam", (v.getGiaTriGiam() != null ? nf.format(v.getGiaTriGiam()) : "0") + suffix);
                if (v.getGiaTriGiamToiDa() != null && "PERCENT".equalsIgnoreCase(v.getLoai())) {
                    map.put("giaTriGiamToiDa", nf.format(v.getGiaTriGiamToiDa()) + " ₫");
                }
            }
            if (v.getGiaTriGiamToiThieu() != null) {
                map.put("giaTriGiamToiThieu", nf.format(v.getGiaTriGiamToiThieu()) + " ₫");
            }
            formats.put(v.getId(), map);
        }

        model.addAttribute("vouchers", vouchers);
        model.addAttribute("formats", formats);
        model.addAttribute("getStatus", (Function<PhieuGiamGia, String>) this::getTrangThai);

        addUserInfoToModel(model);

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageResult.getTotalPages());
        model.addAttribute("pageSize", size);
        model.addAttribute("search", search);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("type", type);
        model.addAttribute("status", status);
        model.addAttribute("phuongThucList", phuongThucThanhToanRepository.findAll());

        return "WebQuanLy/voucher-list";
    }

    private String getTrangThai(PhieuGiamGia v) {
        LocalDateTime now = LocalDateTime.now();

        if (v.getSoLuong() != null && v.getSoLuong() <= 0) {
            return "Đã kết thúc";
        }

        if (v.getNgayBatDau() != null && v.getNgayKetThuc() != null) {
            if (now.isBefore(v.getNgayBatDau())) {
                return "Sắp diễn ra";
            } else if (!now.isAfter(v.getNgayKetThuc())) {
                return "Đang diễn ra";
            } else {
                return "Đã kết thúc";
            }
        }
        return "Không xác định";
    }

    // ===== Create Form =====
    @GetMapping("/create")
    public String createForm(Model model, RedirectAttributes redirectAttributes) {
        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/polyshoe/phieu-giam-gia";
        }

        PhieuGiamGia voucher = new PhieuGiamGia();
        voucher.setPhamViApDung("ORDER"); // mặc định
        model.addAttribute("voucher", voucher);
        model.addAttribute("customers", phieuService.layTatCaKhachHang());

        addUserInfoToModel(model);
        return "WebQuanLy/voucher-create";
    }


    // ===== Create Submit (VALIDATES tightened) =====
    @PostMapping("/create")
    @Transactional
    public String create(@RequestParam(required = false) String giaTriGiam,
                         @RequestParam(required = false) String giaTriGiamToiDa,
                         @RequestParam(required = false) String giaTriGiamToiThieu,
                         @ModelAttribute PhieuGiamGia voucher,
                         @RequestParam(required = false) boolean sendMail,
                         @RequestParam(required = false) List<UUID> selectedCustomerIds,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/polyshoe/phieu-giam-gia";
        }

        List<String> errors = new ArrayList<>();

        // ----- MÃ: 6..50, chữ+số, viết liền (loại mọi whitespace) -----
        if (voucher.getMa() == null) {
            errors.add("Mã phiếu không được để trống.");
        } else {
            String normMa = voucher.getMa().trim().replaceAll("\\s+", "");
            if (normMa.isEmpty()) {
                errors.add("Mã phiếu không được để trống.");
            } else if (normMa.length() < 6 || normMa.length() > 50) {
                errors.add("Mã phiếu phải từ 6 đến 50 ký tự.");
            } else if (!normMa.matches("^[A-Za-z0-9]+$")) {
                errors.add("Mã phiếu chỉ được chứa chữ và số, viết liền, không ký tự đặc biệt.");
            } else if (phieuGiamGiaRepository.existsByMaIgnoreCase(normMa)) {
                errors.add("Mã phiếu đã tồn tại.");
            }
            voucher.setMa(normMa);
        }

        // ----- TÊN: cho phép space đầu/cuối; giữa chỉ 1 space; chữ+số+-+/; >=6 (không tính space); <=100 -----
        if (voucher.getTen() == null || voucher.getTen().isBlank()) {
            errors.add("Tên phiếu không được để trống.");
        } else {
            String tenRaw  = voucher.getTen();
            String tenTrim = tenRaw.trim();

            if (tenTrim.matches(".*\\s{2,}.*")) {
                errors.add("Tên phiếu: giữa các từ chỉ được 1 khoảng trắng.");
            } else if (!tenTrim.matches("^[\\p{L}\\d\\-/]+(?: [\\p{L}\\d\\-/]+)*$")) {
                errors.add("Tên chỉ gồm chữ (có dấu), số, dấu '-' và '/'; giữa các nhóm cách đúng 1 khoảng trắng.");
            } else {
                int nonSpaceLen = tenTrim.replaceAll("\\s+", "").length();
                if (nonSpaceLen < 6) {
                    errors.add("Tên phiếu phải có ít nhất 6 ký tự (không tính khoảng trắng).");
                } else if (tenTrim.length() > 100) {
                    errors.add("Tên phiếu tối đa 100 ký tự.");
                }
            }
            // Giữ nguyên khoảng trắng đầu/cuối theo yêu cầu
            voucher.setTen(voucher.getTen());
        }

        // ----- Phạm vi -----
        if (voucher.getPhamViApDung() == null ||
                !(voucher.getPhamViApDung().equalsIgnoreCase("ORDER") ||
                        voucher.getPhamViApDung().equalsIgnoreCase("SHIPPING"))) {
            errors.add("Phạm vi áp dụng không hợp lệ (chỉ ORDER hoặc SHIPPING).");
        }

        // ----- Parse số -----
        BigDecimal parsedGiaTriGiam = null;
        BigDecimal parsedGiaTriGiamToiDa = null;
        BigDecimal parsedGiaTriGiamToiThieu = null;

        try {
            if (giaTriGiam != null && !giaTriGiam.isBlank()) {
                parsedGiaTriGiam = new BigDecimal(giaTriGiam.replaceAll("[^\\d.]", "").replaceFirst("\\.(\\d+?)\\.", "$1"));
            }
        } catch (NumberFormatException e) {
            errors.add("Giá trị giảm không hợp lệ.");
        }

        try {
            if (giaTriGiamToiDa != null && !giaTriGiamToiDa.isBlank()) {
                parsedGiaTriGiamToiDa = new BigDecimal(giaTriGiamToiDa.replaceAll("[^\\d.]", "").replaceFirst("\\.(\\d+?)\\.", "$1"));
            }
        } catch (NumberFormatException e) {
            errors.add("Giá trị giảm tối đa không hợp lệ.");
        }

        try {
            if (giaTriGiamToiThieu != null && !giaTriGiamToiThieu.isBlank()) {
                parsedGiaTriGiamToiThieu = new BigDecimal(giaTriGiamToiThieu.replaceAll("[^\\d.]", "").replaceFirst("\\.(\\d+?)\\.", "$1"));
                if (parsedGiaTriGiamToiThieu.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("Đơn tối thiểu phải > 0.");
                }
            }
        } catch (NumberFormatException e) {
            errors.add("Đơn tối thiểu không hợp lệ.");
        }

        // ----- Theo phạm vi/loại -----
        if ("ORDER".equalsIgnoreCase(voucher.getPhamViApDung())) {
            if (voucher.getLoai() == null ||
                    !(voucher.getLoai().equalsIgnoreCase("PERCENT") || voucher.getLoai().equalsIgnoreCase("CASH"))) {
                errors.add("Kiểu giảm (ORDER) không hợp lệ (PERCENT hoặc CASH).");
            } else {
                // bắt buộc có đơn tối thiểu
                if (parsedGiaTriGiamToiThieu == null) {
                    errors.add("Vui lòng nhập 'Đơn tối thiểu áp dụng' (> 0) cho phiếu ORDER.");
                }

                if ("PERCENT".equalsIgnoreCase(voucher.getLoai())) {
                    // % integer 1..100
                    if (parsedGiaTriGiam == null) {
                        errors.add("Vui lòng nhập phần trăm giảm.");
                    } else {
                        BigDecimal pt = parsedGiaTriGiam.stripTrailingZeros();
                        if (pt.scale() > 0) {
                            errors.add("Phần trăm giảm phải là số nguyên từ 1 đến 100.");
                        } else if (pt.compareTo(BigDecimal.ONE) < 0 || pt.compareTo(new BigDecimal("100")) > 0) {
                            errors.add("Phần trăm giảm phải trong khoảng 1..100.");
                        }
                    }
                    // cap >0 và < đơn tối thiểu
                    if (parsedGiaTriGiamToiDa == null || parsedGiaTriGiamToiDa.compareTo(BigDecimal.ZERO) <= 0) {
                        errors.add("Phải nhập 'Giá trị giảm tối đa' (> 0) khi giảm theo %.");
                    }
                    if (parsedGiaTriGiamToiDa != null && parsedGiaTriGiamToiThieu != null) {
                        if (parsedGiaTriGiamToiDa.compareTo(parsedGiaTriGiamToiThieu) >= 0) {
                            errors.add("Giá trị giảm tối đa phải nhỏ hơn 'Đơn tối thiểu áp dụng'.");
                        }
                    }
                    if (errors.isEmpty()) {
                        voucher.setGiaTriGiam(parsedGiaTriGiam);
                        voucher.setGiaTriGiamToiDa(parsedGiaTriGiamToiDa);
                    }
                } else { // CASH
                    if (parsedGiaTriGiam == null || parsedGiaTriGiam.compareTo(BigDecimal.ZERO) <= 0) {
                        errors.add("Giá trị giảm (tiền mặt) phải > 0.");
                    }
                    if (parsedGiaTriGiamToiThieu == null) {
                        errors.add("Vui lòng nhập 'Đơn tối thiểu áp dụng' cho phiếu giảm tiền mặt.");
                    }
                    if (parsedGiaTriGiam != null && parsedGiaTriGiamToiThieu != null) {
                        if (parsedGiaTriGiam.compareTo(parsedGiaTriGiamToiThieu) >= 0) {
                            errors.add("Giá trị giảm (tiền mặt) phải nhỏ hơn 'Đơn tối thiểu áp dụng'.");
                        }
                    }
                    voucher.setGiaTriGiamToiDa(null);
                    if (errors.isEmpty()) {
                        voucher.setGiaTriGiam(parsedGiaTriGiam);
                    }
                }
            }
        } else if ("SHIPPING".equalsIgnoreCase(voucher.getPhamViApDung())) {
            if (voucher.getLoai() == null ||
                    !(voucher.getLoai().equalsIgnoreCase("FREESHIP_FULL") ||
                            voucher.getLoai().equalsIgnoreCase("FREESHIP_CAP"))) {
                errors.add("Loại freeship không hợp lệ (FREESHIP_FULL hoặc FREESHIP_CAP).");
            } else {
                voucher.setGiaTriGiam(BigDecimal.ZERO); // freeship không dùng giaTriGiam
                if ("FREESHIP_CAP".equalsIgnoreCase(voucher.getLoai())) {
                    if (parsedGiaTriGiamToiDa == null || parsedGiaTriGiamToiDa.compareTo(BigDecimal.ZERO) <= 0) {
                        errors.add("Phải nhập 'Giảm phí ship tối đa' (> 0) cho FREESHIP_CAP.");
                    }
                    if (parsedGiaTriGiamToiThieu == null) {
                        errors.add("Vui lòng nhập 'Đơn tối thiểu áp dụng' (> 0) cho FREESHIP_CAP.");
                    }
                    if (parsedGiaTriGiamToiDa != null && parsedGiaTriGiamToiThieu != null) {
                        if (parsedGiaTriGiamToiDa.compareTo(parsedGiaTriGiamToiThieu) >= 0) {
                            errors.add("'Giảm phí ship tối đa' phải nhỏ hơn 'Đơn tối thiểu áp dụng'.");
                        }
                    }
                    if (errors.isEmpty()) {
                        voucher.setGiaTriGiamToiDa(parsedGiaTriGiamToiDa);
                    }
                } else { // FREESHIP_FULL
                    voucher.setGiaTriGiamToiDa(null);
                }
            }
        }

        // Gán đơn tối thiểu dùng chung
        if (parsedGiaTriGiamToiThieu != null && parsedGiaTriGiamToiThieu.compareTo(BigDecimal.ZERO) > 0) {
            voucher.setGiaTriGiamToiThieu(parsedGiaTriGiamToiThieu);
        } else {
            voucher.setGiaTriGiamToiThieu(null);
        }

        // ----- Thời gian -----
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getNgayBatDau() == null) {
            errors.add("Ngày bắt đầu không được để trống.");
        } else if (voucher.getNgayBatDau().isBefore(now)) {
            errors.add("Ngày bắt đầu không được nằm trong quá khứ.");
        }
        if (voucher.getNgayKetThuc() == null) {
            errors.add("Ngày kết thúc không được để trống.");
        } else if (voucher.getNgayKetThuc().isBefore(now)) {
            errors.add("Ngày kết thúc không được nằm trong quá khứ.");
        }
        if (voucher.getNgayBatDau() != null && voucher.getNgayKetThuc() != null
                && voucher.getNgayBatDau().isAfter(voucher.getNgayKetThuc())) {
            errors.add("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        }

        // ----- Kiểu phiếu -----
        if ("ca_nhan".equalsIgnoreCase(voucher.getKieuPhieu())) {
            if (selectedCustomerIds == null || selectedCustomerIds.isEmpty()) {
                errors.add("Vui lòng chọn ít nhất một khách hàng khi tạo phiếu cá nhân.");
            } else {
                voucher.setGioiHanSuDung(1);
            }
        } else if ("cong_khai".equalsIgnoreCase(voucher.getKieuPhieu())) {
            Integer gioiHan = voucher.getGioiHanSuDung();
            if (gioiHan == null || gioiHan <= 0) {
                errors.add("Vui lòng nhập số lượt sử dụng hợp lệ cho phiếu công khai.");
            } else {
                voucher.setSoLuong(gioiHan);
            }
        }

        if (!errors.isEmpty()) {
            model.addAttribute("errorMessage", String.join("<br>", errors));
            model.addAttribute("voucher", voucher);
            model.addAttribute("customers", phieuService.layTatCaKhachHang());
            // (ĐÃ BỎ PHƯƠNG THỨC THANH TOÁN Ở MÀN TẠO)
            addUserInfoToModel(model);
            return "WebQuanLy/voucher-create";
        }

        if (voucher.getThoiGianTao() == null) {
            voucher.setThoiGianTao(LocalDateTime.now());
        }

        // ✅ Không ràng buộc PTTT khi tạo (áp dụng cho mọi PTTT)
        voucher.setPhuongThucThanhToans(new HashSet<>());

        try {
            PhieuGiamGia savedVoucher = phieuGiamGiaRepository.save(voucher);
            phieuGiamGiaRepository.flush();

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            broadcastVoucherUpdate("CREATED", savedVoucher, "Phiếu giảm giá mới được tạo: " + savedVoucher.getMa());

            if ("ca_nhan".equalsIgnoreCase(voucher.getKieuPhieu()) && selectedCustomerIds != null) {
                List<NguoiDung> selectedUsers = phieuService.layNguoiDungTheoIds(selectedCustomerIds);
                for (NguoiDung user : selectedUsers) {
                    phieuService.ganPhieuChoNguoiDung(user, savedVoucher);
                }

                if (sendMail) {
                    for (NguoiDung user : selectedUsers) {
                        guiMailService.guiPhieuGiamGia(user, savedVoucher);
                    }
                    redirectAttributes.addFlashAttribute("mailMessage", "Đã gửi email đến khách hàng được chọn.");
                }
            }

            redirectAttributes.addFlashAttribute("successMessage", "Tạo phiếu giảm giá thành công!");
            return "redirect:/polyshoe/phieu-giam-gia";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu phiếu giảm giá: " + e.getMessage());
            return "redirect:/polyshoe/phieu-giam-gia";
        }
    }


    // ===== View =====
    @GetMapping("/view/{id}")
    public String viewDetails(@PathVariable UUID id,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "5") int size,
                              @RequestParam(required = false) String search,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        if (!isCurrentUserAdmin() && !isCurrentUserEmployee()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/polyshoe/phieu-giam-gia";
        }

        PhieuGiamGia voucher = phieuGiamGiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiếu giảm giá không tồn tại"));

        model.addAttribute("viewMode", true);
        model.addAttribute("readOnly", true);

        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        String giaTriGiamStr;
        String giaTriGiamToiDaStr = voucher.getGiaTriGiamToiDa() != null ? nf.format(voucher.getGiaTriGiamToiDa()) : "";
        String giaTriGiamToiThieuStr = voucher.getGiaTriGiamToiThieu() != null ? nf.format(voucher.getGiaTriGiamToiThieu()) : "";
        String gioiHanSuDungStr = voucher.getGioiHanSuDung() != null ? String.valueOf(voucher.getGioiHanSuDung()) : "";

        if ("SHIPPING".equalsIgnoreCase(voucher.getPhamViApDung())) {
            if ("FREESHIP_FULL".equalsIgnoreCase(voucher.getLoai())) {
                giaTriGiamStr = "Freeship toàn phần";
            } else if ("FREESHIP_CAP".equalsIgnoreCase(voucher.getLoai())) {
                giaTriGiamStr = "Giảm phí ship tối đa " + (voucher.getGiaTriGiamToiDa() != null ? nf.format(voucher.getGiaTriGiamToiDa()) + " ₫" : "0 ₫");
            } else {
                giaTriGiamStr = "-";
            }
        } else {
            giaTriGiamStr = nf.format(voucher.getGiaTriGiam());
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<NguoiDung> customerPage = (search != null && !search.isBlank())
                ? phieuService.timKiemKhachHangPhanTrang(search, pageable)
                : phieuService.layTatCaKhachHangPhanTrang(pageable);

        List<UUID> selectedCustomerIds = new ArrayList<>();
        List<UUID> selectedPtttIds = voucher.getPhuongThucThanhToans().stream().map(PhuongThucThanhToan::getId).toList();
        List<PhuongThucThanhToan> phuongThucList = selectedPtttIds.isEmpty()
                ? new ArrayList<>()
                : phuongThucThanhToanRepository.findAllById(selectedPtttIds);

        if ("ca_nhan".equalsIgnoreCase(voucher.getKieuPhieu())) {
            List<NguoiDung> daDuocGan = phieuService.layNguoiDungTheoPhieu(voucher.getId());
            selectedCustomerIds = daDuocGan.stream().map(NguoiDung::getId).toList();
        }

        model.addAttribute("voucher", voucher);
        model.addAttribute("giaTriGiamStr", giaTriGiamStr);
        model.addAttribute("giaTriGiamToiDaStr", giaTriGiamToiDaStr);
        model.addAttribute("giaTriGiamToiThieuStr", giaTriGiamToiThieuStr);
        model.addAttribute("gioiHanSuDungStr", gioiHanSuDungStr);
        model.addAttribute("customers", customerPage.getContent());
        model.addAttribute("selectedCustomerIds", selectedCustomerIds);
        model.addAttribute("selectedPtttIds", selectedPtttIds);
        model.addAttribute("phuongThucList", phuongThucList);
        model.addAttribute("currentCustomerPage", page);
        model.addAttribute("totalCustomerPages", customerPage.getTotalPages());
        model.addAttribute("search", search);
        model.addAttribute("getStatus", (Function<PhieuGiamGia, String>) this::getTrangThai);
        addUserInfoToModel(model);

        return "WebQuanLy/voucher-detail";
    }

    // ===== Edit Form =====
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable UUID id,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "5") int size,
                               @RequestParam(required = false) String search,
                               Model model,
                               RedirectAttributes redirectAttributes) {

        PhieuGiamGia voucher = phieuGiamGiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiếu giảm giá không tồn tại"));

        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/polyshoe/phieu-giam-gia";
        }

        String status = getTrangThai(voucher);
        if (!"Sắp diễn ra".equals(status)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ có thể chỉnh sửa phiếu giảm giá ở trạng thái 'Sắp diễn ra'.");
            return "redirect:/polyshoe/phieu-giam-gia";
        }

        model.addAttribute("viewMode", false);
        model.addAttribute("readOnly", false);

        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        String giaTriGiamStr;
        String giaTriGiamToiDaStr = voucher.getGiaTriGiamToiDa() != null ? nf.format(voucher.getGiaTriGiamToiDa()) : "";
        String giaTriGiamToiThieuStr = voucher.getGiaTriGiamToiThieu() != null ? nf.format(voucher.getGiaTriGiamToiThieu()) : "";
        String gioiHanSuDungStr = voucher.getGioiHanSuDung() != null ? String.valueOf(voucher.getGioiHanSuDung()) : "";

        if ("SHIPPING".equalsIgnoreCase(voucher.getPhamViApDung())) {
            if ("FREESHIP_FULL".equalsIgnoreCase(voucher.getLoai())) {
                giaTriGiamStr = "Freeship toàn phần";
            } else if ("FREESHIP_CAP".equalsIgnoreCase(voucher.getLoai())) {
                giaTriGiamStr = "Giảm phí ship tối đa " + (voucher.getGiaTriGiamToiDa() != null ? nf.format(voucher.getGiaTriGiamToiDa()) + " ₫" : "0 ₫");
            } else {
                giaTriGiamStr = "-";
            }
        } else {
            giaTriGiamStr = nf.format(voucher.getGiaTriGiam());
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<NguoiDung> customerPage = (search != null && !search.isBlank())
                ? phieuService.timKiemKhachHangPhanTrang(search, pageable)
                : phieuService.layTatCaKhachHangPhanTrang(pageable);

        List<UUID> selectedCustomerIds = new ArrayList<>();
        if ("ca_nhan".equalsIgnoreCase(voucher.getKieuPhieu())) {
            List<NguoiDung> daDuocGan = phieuService.layNguoiDungTheoPhieu(voucher.getId());
            selectedCustomerIds = daDuocGan.stream().map(NguoiDung::getId).toList();
        }

        model.addAttribute("voucher", voucher);
        model.addAttribute("giaTriGiamStr", giaTriGiamStr);
        model.addAttribute("giaTriGiamToiDaStr", giaTriGiamToiDaStr);
        model.addAttribute("giaTriGiamToiThieuStr", giaTriGiamToiThieuStr);
        model.addAttribute("gioiHanSuDungStr", gioiHanSuDungStr);
        model.addAttribute("customers", customerPage.getContent());
        model.addAttribute("selectedCustomerIds", selectedCustomerIds);
        model.addAttribute("currentCustomerPage", page);
        model.addAttribute("totalCustomerPages", customerPage.getTotalPages());
        model.addAttribute("search", search);
        model.addAttribute("getStatus", (Function<PhieuGiamGia, String>) this::getTrangThai);
        addUserInfoToModel(model);

        return "WebQuanLy/voucher-edit";
    }


    // ===== Edit Submit (VALIDATES tightened) =====
    @PostMapping("/edit/{id}")
    @Transactional
    public String update(@PathVariable UUID id,
                         @RequestParam(required = false) String giaTriGiam,
                         @RequestParam(required = false) String giaTriGiamToiDa,
                         @RequestParam(required = false) String giaTriGiamToiThieu,
                         @ModelAttribute PhieuGiamGia voucher,
                         @RequestParam(required = false) boolean sendMail,
                         @RequestParam(required = false) List<UUID> selectedCustomerIds,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/polyshoe/phieu-giam-gia";
        }

        PhieuGiamGia existing = phieuGiamGiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiếu giảm giá không tồn tại"));

        String status = getTrangThai(existing);
        if (!"Sắp diễn ra".equals(status)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ có thể chỉnh sửa phiếu giảm giá ở trạng thái 'Sắp diễn ra'.");
            return "redirect:/polyshoe/phieu-giam-gia";
        }

        // Khóa các trường không cho đổi
        voucher.setId(id);
        voucher.setMa(existing.getMa());
        voucher.setThoiGianTao(existing.getThoiGianTao());
        voucher.setPhamViApDung(existing.getPhamViApDung());

        // 🚫 KHÓA LOẠI PHIẾU: luôn giữ nguyên như cũ
        voucher.setKieuPhieu(existing.getKieuPhieu());

        List<String> errors = new ArrayList<>();

        // ===== Validate Tên (giữ rule hiện tại) =====
        if (voucher.getTen() == null || voucher.getTen().isBlank()) {
            errors.add("Tên phiếu không được để trống.");
        } else {
            String tenRaw  = voucher.getTen();
            String tenTrim = tenRaw.trim();

            if (!tenRaw.equals(tenTrim)) {
                errors.add("Không cho phép khoảng trắng ở đầu hoặc cuối.");
            } else if (tenTrim.matches(".*\\s{2,}.*")) {
                errors.add("Tên phiếu: giữa các từ chỉ được 1 khoảng trắng.");
            } else if (!tenTrim.matches("^[\\p{L}\\d\\-/]+(?: [\\p{L}\\d\\-/]+)*$")) {
                errors.add("Tên chỉ gồm chữ (có dấu), số, dấu '-' và '/'; giữa các nhóm cách đúng 1 khoảng trắng.");
            } else {
                int nonSpaceLen = tenTrim.replaceAll("\\s+", "").length();
                if (nonSpaceLen < 6) {
                    errors.add("Tên phiếu phải có ít nhất 6 ký tự (không tính khoảng trắng).");
                } else if (tenTrim.length() > 100) {
                    errors.add("Tên phiếu tối đa 100 ký tự.");
                }
            }
            voucher.setTen(tenRaw);
        }

        // ===== Parse số =====
        BigDecimal parsedGiaTriGiam = null;
        BigDecimal parsedGiaTriGiamToiDa = null;
        BigDecimal parsedGiaTriGiamToiThieu = null;

        try {
            if (giaTriGiam != null && !giaTriGiam.isBlank()) {
                parsedGiaTriGiam = new BigDecimal(giaTriGiam.replaceAll("[^\\d.]", "").replaceFirst("\\.(\\d+?)\\.", "$1"));
            }
        } catch (NumberFormatException e) {
            errors.add("Giá trị giảm không hợp lệ.");
        }

        try {
            if (giaTriGiamToiDa != null && !giaTriGiamToiDa.isBlank()) {
                parsedGiaTriGiamToiDa = new BigDecimal(giaTriGiamToiDa.replaceAll("[^\\d.]", "").replaceFirst("\\.(\\d+?)\\.", "$1"));
            }
        } catch (NumberFormatException e) {
            errors.add("Giá trị giảm tối đa không hợp lệ.");
        }

        try {
            if (giaTriGiamToiThieu != null && !giaTriGiamToiThieu.isBlank()) {
                parsedGiaTriGiamToiThieu = new BigDecimal(giaTriGiamToiThieu.replaceAll("[^\\d.]", "").replaceFirst("\\.(\\d+?)\\.", "$1"));
                if (parsedGiaTriGiamToiThieu.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("Đơn tối thiểu phải > 0.");
                }
            }
        } catch (NumberFormatException e) {
            errors.add("Đơn tối thiểu không hợp lệ.");
        }

        // ===== Validate theo phạm vi/loại =====
        if ("ORDER".equalsIgnoreCase(voucher.getPhamViApDung())) {
            if (voucher.getLoai() == null ||
                    !(voucher.getLoai().equalsIgnoreCase("PERCENT") || voucher.getLoai().equalsIgnoreCase("CASH"))) {
                errors.add("Kiểu giảm (ORDER) không hợp lệ (PERCENT hoặc CASH).");
            } else {
                if (parsedGiaTriGiamToiThieu == null) {
                    errors.add("Vui lòng nhập 'Đơn tối thiểu áp dụng' (> 0) cho phiếu ORDER.");
                }
                if ("PERCENT".equalsIgnoreCase(voucher.getLoai())) {
                    if (parsedGiaTriGiam == null) {
                        errors.add("Vui lòng nhập phần trăm giảm.");
                    } else {
                        BigDecimal pt = parsedGiaTriGiam.stripTrailingZeros();
                        if (pt.scale() > 0) {
                            errors.add("Phần trăm giảm phải là số nguyên từ 1 đến 100.");
                        } else if (pt.compareTo(BigDecimal.ONE) < 0 || pt.compareTo(new BigDecimal("100")) > 0) {
                            errors.add("Phần trăm giảm phải trong khoảng 1..100.");
                        }
                    }
                    if (parsedGiaTriGiamToiDa == null || parsedGiaTriGiamToiDa.compareTo(BigDecimal.ZERO) <= 0) {
                        errors.add("Phải nhập 'Giá trị giảm tối đa' (> 0) khi giảm theo %.");
                    }
                    if (parsedGiaTriGiamToiDa != null && parsedGiaTriGiamToiThieu != null
                            && parsedGiaTriGiamToiDa.compareTo(parsedGiaTriGiamToiThieu) >= 0) {
                        errors.add("Giá trị giảm tối đa phải nhỏ hơn 'Đơn tối thiểu áp dụng'.");
                    }
                } else { // CASH
                    if (parsedGiaTriGiam == null || parsedGiaTriGiam.compareTo(BigDecimal.ZERO) <= 0) {
                        errors.add("Giá trị giảm (tiền mặt) phải > 0.");
                    }
                    if (parsedGiaTriGiamToiThieu == null) {
                        errors.add("Vui lòng nhập 'Đơn tối thiểu áp dụng' cho phiếu giảm tiền mặt.");
                    }
                    if (parsedGiaTriGiam != null && parsedGiaTriGiamToiThieu != null
                            && parsedGiaTriGiam.compareTo(parsedGiaTriGiamToiThieu) >= 0) {
                        errors.add("Giá trị giảm (tiền mặt) phải nhỏ hơn 'Đơn tối thiểu áp dụng'.");
                    }
                    parsedGiaTriGiamToiDa = null;
                }
            }
        } else if ("SHIPPING".equalsIgnoreCase(voucher.getPhamViApDung())) {
            if (voucher.getLoai() == null ||
                    !(voucher.getLoai().equalsIgnoreCase("FREESHIP_FULL") ||
                            voucher.getLoai().equalsIgnoreCase("FREESHIP_CAP"))) {
                errors.add("Loại freeship không hợp lệ (FREESHIP_FULL hoặc FREESHIP_CAP).");
            } else {
                parsedGiaTriGiam = BigDecimal.ZERO; // freeship không dùng giaTriGiam
                if ("FREESHIP_CAP".equalsIgnoreCase(voucher.getLoai())) {
                    if (parsedGiaTriGiamToiDa == null || parsedGiaTriGiamToiDa.compareTo(BigDecimal.ZERO) <= 0) {
                        errors.add("Phải nhập 'Giảm phí ship tối đa' (> 0) cho FREESHIP_CAP.");
                    }
                    if (parsedGiaTriGiamToiThieu == null) {
                        errors.add("Vui lòng nhập 'Đơn tối thiểu áp dụng' (> 0) cho FREESHIP_CAP.");
                    }
                    if (parsedGiaTriGiamToiDa != null && parsedGiaTriGiamToiThieu != null
                            && parsedGiaTriGiamToiDa.compareTo(parsedGiaTriGiamToiThieu) >= 0) {
                        errors.add("'Giảm phí ship tối đa' phải nhỏ hơn 'Đơn tối thiểu áp dụng'.");
                    }
                } else { // FREESHIP_FULL
                    parsedGiaTriGiamToiDa = null;
                }
            }
        } else {
            errors.add("Phạm vi áp dụng không hợp lệ (chỉ ORDER hoặc SHIPPING).");
        }

        // gán đơn tối thiểu dùng chung
        if (parsedGiaTriGiamToiThieu != null && parsedGiaTriGiamToiThieu.compareTo(BigDecimal.ZERO) > 0) {
            voucher.setGiaTriGiamToiThieu(parsedGiaTriGiamToiThieu);
        } else {
            voucher.setGiaTriGiamToiThieu(null);
        }

        // ===== Thời gian =====
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getNgayBatDau() == null) {
            errors.add("Ngày bắt đầu không được để trống.");
        } else if (voucher.getNgayBatDau().isBefore(now)) {
            errors.add("Ngày bắt đầu không được nằm trong quá khứ.");
        }
        if (voucher.getNgayKetThuc() == null) {
            errors.add("Ngày kết thúc không được để trống.");
        } else if (voucher.getNgayKetThuc().isBefore(now)) {
            errors.add("Ngày kết thúc không được nằm trong quá khứ.");
        }
        if (voucher.getNgayBatDau() != null && voucher.getNgayKetThuc() != null
                && voucher.getNgayBatDau().isAfter(voucher.getNgayKetThuc())) {
            errors.add("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        }

        // ===== Kiểu phiếu (khóa đổi): xét theo existing =====
        boolean isCaNhan = "ca_nhan".equalsIgnoreCase(existing.getKieuPhieu());
        if (isCaNhan) {
            if (selectedCustomerIds == null || selectedCustomerIds.isEmpty()) {
                errors.add("Vui lòng chọn ít nhất một khách hàng cho phiếu cá nhân.");
            }
        } else {
            Integer gioiHan = voucher.getGioiHanSuDung();
            if (gioiHan == null || gioiHan <= 0) {
                errors.add("Vui lòng nhập số lượt sử dụng hợp lệ cho phiếu công khai.");
            }
        }

        if (!errors.isEmpty()) {
            model.addAttribute("voucher", existing);
            model.addAttribute("errorMessage", String.join("<br>", errors));
            model.addAttribute("customers", phieuService.layTatCaKhachHang());
            model.addAttribute("selectedCustomerIds", selectedCustomerIds != null ? selectedCustomerIds : new ArrayList<>());
            NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
            model.addAttribute("giaTriGiamStr", existing.getGiaTriGiam() != null ? nf.format(existing.getGiaTriGiam()) : "");
            model.addAttribute("giaTriGiamToiDaStr", existing.getGiaTriGiamToiDa() != null ? nf.format(existing.getGiaTriGiamToiDa()) : "");
            model.addAttribute("giaTriGiamToiThieuStr", existing.getGiaTriGiamToiThieu() != null ? nf.format(existing.getGiaTriGiamToiThieu()) : "");
            model.addAttribute("gioiHanSuDungStr", existing.getGioiHanSuDung() != null ? String.valueOf(existing.getGioiHanSuDung()) : "");
            model.addAttribute("getStatus", (Function<PhieuGiamGia, String>) this::getTrangThai);
            model.addAttribute("currentCustomerPage", 0);
            model.addAttribute("totalCustomerPages", 1);
            addUserInfoToModel(model);
            return "WebQuanLy/voucher-edit";
        }

        // ===== Ghi dữ liệu hợp lệ =====
        existing.setTen(voucher.getTen());
        existing.setLoai(voucher.getLoai());
        existing.setGiaTriGiam(parsedGiaTriGiam);
        existing.setGiaTriGiamToiDa(parsedGiaTriGiamToiDa);
        existing.setGiaTriGiamToiThieu(parsedGiaTriGiamToiThieu);
        existing.setNgayBatDau(voucher.getNgayBatDau());
        existing.setNgayKetThuc(voucher.getNgayKetThuc());

        // KHÔNG ĐỘNG VÀO PTTT Ở EDIT NỮA

        // Kiểu phiếu cố định theo existing
        existing.setKieuPhieu(existing.getKieuPhieu());
        if (isCaNhan) {
            existing.setGioiHanSuDung(1);
            existing.setSoLuong(null);
        } else {
            Integer gioiHan = voucher.getGioiHanSuDung();
            existing.setGioiHanSuDung(gioiHan);
            existing.setSoLuong(gioiHan); // còn 'Sắp diễn ra' nên cho sync lại
        }

        try {
            PhieuGiamGia saved = phieuGiamGiaRepository.save(existing);

            // Nếu là cá nhân: gán KH + (tuỳ chọn) gửi mail
            if (isCaNhan && selectedCustomerIds != null && !selectedCustomerIds.isEmpty()) {
                List<NguoiDung> users = phieuService.layNguoiDungTheoIds(selectedCustomerIds);
                for (NguoiDung user : users) {
                    phieuService.ganPhieuChoNguoiDung(user, saved);
                }
                if (sendMail) {
                    try {
                        for (NguoiDung user : users) guiMailService.guiPhieuGiamGia(user, saved);
                        redirectAttributes.addFlashAttribute("mailMessage", "Đã gửi mail cập nhật cho khách hàng");
                    } catch (Exception e) {
                        redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi gửi mail: " + e.getMessage());
                    }
                }
            }

            broadcastVoucherUpdate("UPDATED", saved, "Phiếu giảm giá được cập nhật: " + saved.getMa());
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật phiếu giảm giá thành công!");
            return "redirect:/polyshoe/phieu-giam-gia";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật phiếu giảm giá: " + e.getMessage());
            return "redirect:/polyshoe/phieu-giam-gia";
        }
    }


    // ===== Delete (only "Sắp diễn ra") =====
    @PostMapping("/delete/{id}")
    @Transactional
    public String deleteByPath(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        return doDelete(id, redirectAttributes);
    }

    @PostMapping({"/delete", "/delete/"})
    @Transactional
    public String deleteByParam(@RequestParam("id") UUID id, RedirectAttributes redirectAttributes) {
        return doDelete(id, redirectAttributes);
    }

    private String doDelete(UUID id, RedirectAttributes redirectAttributes) {
        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/polyshoe/phieu-giam-gia";
        }

        PhieuGiamGia voucher = phieuGiamGiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiếu giảm giá không tồn tại"));

        String status = getTrangThai(voucher);
        if (!"Sắp diễn ra".equals(status)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Chỉ có thể xóa phiếu giảm giá ở trạng thái 'Sắp diễn ra'.");
            return "redirect:/polyshoe/phieu-giam-gia";
        }

        try {
            broadcastVoucherUpdate("DELETED", voucher, "Phiếu giảm giá bị xóa: " + voucher.getMa());

            phieuService.xoaTatCaGanKetTheoPhieu(id);
            phieuGiamGiaRepository.deletePhuongThucThanhToanByPhieuGiamGiaId(id);
            phieuGiamGiaRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa phiếu giảm giá thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa: " + e.getMessage());
        }

        return "redirect:/polyshoe/phieu-giam-gia";
    }

    // ===== API summary =====
    @GetMapping("/api/summary")
    @ResponseBody
    public List<Map<String, Object>> summaryByIds(@RequestParam("ids") String idsCsv) {
        if (!isCurrentUserAdmin() && !isCurrentUserEmployee()) {
            return Collections.emptyList();
        }
        if (idsCsv == null || idsCsv.isBlank()) return Collections.emptyList();

        List<UUID> ids = Arrays.stream(idsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .collect(Collectors.toList());

        List<PhieuGiamGia> list = phieuGiamGiaRepository.findAllById(ids);
        List<Map<String, Object>> result = new ArrayList<>();
        for (PhieuGiamGia v : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", v.getId().toString());
            m.put("soLuong", v.getSoLuong());
            m.put("status", getTrangThai(v));
            result.add(m);
        }
        return result;
    }

    private void broadcastVoucherUpdate(String action, PhieuGiamGia voucher, String message) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("action", action);
            update.put("voucherId", voucher.getId().toString());
            update.put("voucherCode", voucher.getMa());
            update.put("voucherName", voucher.getTen());
            update.put("status", getTrangThai(voucher));
            update.put("quantity", voucher.getSoLuong());
            update.put("message", message);
            update.put("timestamp", System.currentTimeMillis());

            System.out.println("Broadcasting voucher update: " + action + " for voucher: " + voucher.getMa());

            // Broadcast to admin voucher list
            messagingTemplate.convertAndSend("/topic/vouchers", update);

            // Broadcast to payment pages
            messagingTemplate.convertAndSend("/topic/payment/vouchers", update);

            System.out.println("Successfully broadcasted voucher update for: " + voucher.getMa());

        } catch (Exception e) {
            System.err.println("Failed to broadcast voucher update: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
