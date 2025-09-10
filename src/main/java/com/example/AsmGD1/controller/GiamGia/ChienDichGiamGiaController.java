package com.example.AsmGD1.controller.GiamGia;

import com.example.AsmGD1.dto.GiamGia.SnapshotDetailItem;
import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.service.GiamGia.ChienDichGiamGiaService;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.SanPhamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/acvstore/chien-dich-giam-gia")
public class ChienDichGiamGiaController {

    @Autowired
    private ChienDichGiamGiaService chienDichService;

    @Autowired
    private SanPhamService sanPhamService;

    @Autowired
    private NguoiDungService nguoiDungService;

    // ===== RBAC helpers =====
    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung user) {
            return "admin".equalsIgnoreCase(user.getVaiTro());
        }
        return false;
    }

    private boolean isCurrentUserEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung user) {
            return "employee".equalsIgnoreCase(user.getVaiTro());
        }
        return false;
    }

    private void addUserInfoToModel(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung user) {
            model.addAttribute("user", user);
            model.addAttribute("isAdmin", "admin".equalsIgnoreCase(user.getVaiTro()));
            model.addAttribute("isEmployee", "employee".equalsIgnoreCase(user.getVaiTro()));
        } else {
            NguoiDung defaultUser = new NguoiDung();
            defaultUser.setTenDangNhap("Unknown");
            defaultUser.setVaiTro("GUEST");
            model.addAttribute("user", defaultUser);
            model.addAttribute("isAdmin", false);
            model.addAttribute("isEmployee", false);
        }
    }

    // ===== Utils normalize =====
    private String normalizeName(String s) {
        if (s == null) return null;
        return s.trim().replaceAll("\\s+", " ");
    }
    private String normalizeCode(String s) {
        if (s == null) return null;
        return s.trim().replaceAll("\\s+", "");
    }
    private String normalizeSearchKeyword(String s) {
        if (s == null) return null;
        return s.trim().replaceAll("\\s+", " ");
    }
    // ❌ XÓA 2 method trùng mapping trước đó
// ✅ GIỮ DUY NHẤT method này
    @GetMapping(value = "/search-products", produces = "application/json")
    @ResponseBody
    public Page<SanPham> searchProductsAjax(
            @RequestParam(name="keyword", required=false, defaultValue="") String keyword,
            @RequestParam(name="page", defaultValue="0") int page,
            @RequestParam(name="size", defaultValue="5") int size,
            @RequestParam(name="selectedIds", required=false) String selectedIdsCsv
    ){
        // chuẩn hoá keyword (tuỳ chọn)
        keyword = normalizeSearchKeyword(keyword);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "thoiGianTao"));

        // parse selectedIds CSV -> Set<UUID>
        Set<UUID> selectedIds = new LinkedHashSet<>();
        if (selectedIdsCsv != null && !selectedIdsCsv.isBlank()){
            for (String s : selectedIdsCsv.split(",")){
                s = s.trim();
                if (!s.isEmpty()){
                    try { selectedIds.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
                }
            }
        }

        // Gộp “available” + “đã chọn” để các SP đã tick luôn xuất hiện
        return sanPhamService.searchAvailableOrSelectedByTenOrMa(keyword, pageable, selectedIds);
    }

    // ===== List =====
    @GetMapping
    public String danhSach(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(name = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "discountLevel", required = false) String discountLevel,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            Model model
    ) {
        if (!isCurrentUserAdmin() && !isCurrentUserEmployee()) {
            return "redirect:/login";
        }

        keyword = normalizeSearchKeyword(keyword);

        Pageable pageable = PageRequest.of(
                page, size,
                Sort.by(Sort.Direction.DESC, "thoiGianTao", "id")
        );

        Page<ChienDichGiamGia> pageResult =
                chienDichService.locChienDich(keyword, startDate, endDate, status, discountLevel, pageable);

        model.addAttribute("campaignList", pageResult.getContent());
        model.addAttribute("currentPage", pageResult.getNumber());
        model.addAttribute("totalPages", pageResult.getTotalPages());
        model.addAttribute("totalItems", pageResult.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("status", status);
        model.addAttribute("discountLevel", discountLevel);
        model.addAttribute("pageSize", size);

        addUserInfoToModel(model);
        return "WebQuanLy/discount-campaign-list";
    }

    // ===== Create form =====
    @GetMapping("/create")
    public String hienFormTaoMoi(Model model,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "5") int size,
                                 RedirectAttributes redirectAttributes) {
        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/acvstore/chien-dich-giam-gia";
        }

        if (!model.containsAttribute("chienDich")) {
            model.addAttribute("chienDich", new ChienDichGiamGia());
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<SanPham> productPage = sanPhamService.getPagedAvailableProducts(pageable);

        model.addAttribute("productPage", productPage);
        model.addAttribute("currentProductPage", productPage.getNumber());
        model.addAttribute("totalProductPages", productPage.getTotalPages());

        addUserInfoToModel(model);
        return "WebQuanLy/discount-campaign-form";
    }

    // ===== Create submit =====
    @PostMapping("/create")
    public String taoMoi(@ModelAttribute("chienDich") ChienDichGiamGia chienDich,
                         @RequestParam("selectedProductDetailIds") String selectedIdsRaw,
                         Model model, RedirectAttributes redirectAttributes) {
        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/acvstore/chien-dich-giam-gia";
        }

        chienDich.setMa(normalizeCode(chienDich.getMa()));
        chienDich.setTen(normalizeName(chienDich.getTen()));

        List<UUID> danhSachChiTietId = parseCsvUuids(selectedIdsRaw);

        String error = validateChienDich(chienDich, danhSachChiTietId, true, null);
        if (error != null) {
            model.addAttribute("errorMessage", error);
            model.addAttribute("chienDich", chienDich);

            Pageable pageable = PageRequest.of(0, 5);
            Page<SanPham> productPage = sanPhamService.getPagedAvailableProducts(pageable);

            model.addAttribute("productPage", productPage);
            model.addAttribute("currentProductPage", productPage.getNumber());
            model.addAttribute("totalProductPages", productPage.getTotalPages());
            addUserInfoToModel(model);
            return "WebQuanLy/discount-campaign-form";
        }

        chienDich.setId(null);
        chienDich.setThoiGianTao(LocalDateTime.now());
        try {
            chienDichService.taoMoiChienDichKemChiTiet(chienDich, danhSachChiTietId);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo chiến dịch thành công!");
            return "redirect:/acvstore/chien-dich-giam-gia";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("chienDich", chienDich);

            Pageable pageable = PageRequest.of(0, 5);
            Page<SanPham> productPage = sanPhamService.getPagedAvailableProducts(pageable);

            model.addAttribute("productPage", productPage);
            model.addAttribute("currentProductPage", productPage.getNumber());
            model.addAttribute("totalProductPages", productPage.getTotalPages());
            addUserInfoToModel(model);
            return "WebQuanLy/discount-campaign-form";
        }
    }

    // ===== View (đã sửa: ưu tiên snapshot khi ENDED) =====
    @GetMapping("/view/{id}")
    public String xemChiTiet(@PathVariable("id") UUID id,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (!isCurrentUserAdmin() && !isCurrentUserEmployee()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/acvstore/chien-dich-giam-gia";
        }

        try {
            ChienDichGiamGia chienDich = chienDichService.timTheoId(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch"));
            String status = getStatus(chienDich);

            // Nếu đã kết thúc, ưu tiên lấy snapshot để luôn hiển thị
            if ("ENDED".equals(status)) {
                List<SnapshotDetailItem> snap = chienDichService.getSnapshot(id);
                if (snap != null && !snap.isEmpty()) {
                    model.addAttribute("isReadOnly", true);
                    model.addAttribute("chienDich", chienDich);
                    model.addAttribute("status", status);

                    // Bật chế độ hiển thị snapshot (view cần đọc useSnapshot + snapshotDetails)
                    model.addAttribute("useSnapshot", true);
                    model.addAttribute("snapshotDetails", snap);

                    addUserInfoToModel(model);
                    return "WebQuanLy/discount-campaign-view";
                }
                // Nếu chưa có snapshot (legacy), fallback xuống luồng “live” phía dưới
            }

            // Luồng mặc định (UPCOMING/ONGOING hoặc ENDED nhưng chưa có snapshot legacy)
            List<ChiTietSanPham> chiTietDaChon = chienDichService.layChiTietDaChonTheoChienDich(id);

            List<SanPham> selectedProducts = chiTietDaChon.stream()
                    .map(ChiTietSanPham::getSanPham)
                    .filter(Objects::nonNull)
                    .collect(Collectors.collectingAndThen(
                            Collectors.toMap(SanPham::getId, sp -> sp, (a, b) -> a, LinkedHashMap::new),
                            m -> m.values().stream()
                                    .sorted(Comparator.comparing(sp -> Optional.ofNullable(sp.getTenSanPham()).orElse("")))
                                    .toList()
                    ));

            model.addAttribute("isReadOnly", true);
            model.addAttribute("chienDich", chienDich);
            model.addAttribute("status", status);
            model.addAttribute("selectedProducts", selectedProducts);
            model.addAttribute("selectedDetails", chiTietDaChon);

            // Không đặt thông báo nào — chỉ hiển thị dữ liệu
            model.addAttribute("useSnapshot", false);

            addUserInfoToModel(model);
            return "WebQuanLy/discount-campaign-view";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tải thông tin chiến dịch: " + e.getMessage());
            return "redirect:/acvstore/chien-dich-giam-gia";
        }
    }

    // ===== Edit form =====
    @GetMapping("/edit/{id}")
    public String hienFormChinhSua(@PathVariable("id") UUID id,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "5") int size,
                                   @RequestParam(name = "selectedProductIds", required = false) String selectedProductIdsStr,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        boolean isAdmin = isCurrentUserAdmin();
        boolean isEmployee = isCurrentUserEmployee();

        if (!isAdmin && !isEmployee) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/acvstore/chien-dich-giam-gia";
        }

        if (isEmployee) {
            return "redirect:/acvstore/chien-dich-giam-gia/view/" + id;
        }

        try {
            ChienDichGiamGia chienDich = chienDichService.timTheoId(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch"));

            if (chienDich.getPhanTramGiam() != null) {
                chienDich.setPhanTramGiam(chienDich.getPhanTramGiam().stripTrailingZeros());
            }

            String status = getStatus(chienDich);
            boolean isReadOnly = !"UPCOMING".equals(status);

            if (isReadOnly) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Chỉ có thể chỉnh sửa chiến dịch sắp diễn ra.");
                return "redirect:/acvstore/chien-dich-giam-gia/view/" + id;
            }

            List<ChiTietSanPham> chiTietDaChon = chienDichService.layChiTietDaChonTheoChienDich(id);

            Set<UUID> selectedProductIds = chiTietDaChon.stream()
                    .map(ChiTietSanPham::getSanPham)
                    .filter(Objects::nonNull)
                    .map(SanPham::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (selectedProductIdsStr != null && !selectedProductIdsStr.isEmpty()) {
                selectedProductIds.addAll(Arrays.stream(selectedProductIdsStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(UUID::fromString)
                        .collect(Collectors.toSet()));
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<SanPham> productPage = sanPhamService.getPagedAvailableOrSelectedProducts(pageable, selectedProductIds);

            model.addAttribute("chienDich", chienDich);
            model.addAttribute("status", status);
            model.addAttribute("isReadOnly", false);

            model.addAttribute("selectedDetails", chiTietDaChon);
            model.addAttribute("selectedProductIds", selectedProductIds);
            model.addAttribute("productPage", productPage);
            model.addAttribute("currentProductPage", productPage.getNumber());
            model.addAttribute("totalProductPages", productPage.getTotalPages());

            addUserInfoToModel(model);
            return "WebQuanLy/discount-campaign-edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tải form chỉnh sửa: " + e.getMessage());
            return "redirect:/acvstore/chien-dich-giam-gia";
        }
    }

    // ===== Update submit =====
    @PostMapping("/update")
    public String capNhatChienDich(@ModelAttribute("chienDich") ChienDichGiamGia chienDich,
                                   @RequestParam("selectedProductDetailIds") String selectedIdsRaw,
                                   Model model, RedirectAttributes redirectAttributes) {
        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/acvstore/chien-dich-giam-gia";
        }

        try {
            ChienDichGiamGia chienDichCu = chienDichService.timTheoId(chienDich.getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch"));

            String currentStatus = getStatus(chienDichCu);
            if (!"UPCOMING".equals(currentStatus)) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Chỉ có thể chỉnh sửa chiến dịch sắp diễn ra. Chiến dịch đang diễn ra hoặc đã kết thúc không thể chỉnh sửa.");
                return "redirect:/acvstore/chien-dich-giam-gia";
            }

            String incomingMa  = normalizeCode(chienDich.getMa());
            String incomingTen = normalizeName(chienDich.getTen());

            if (!chienDichCu.getMa().equalsIgnoreCase(incomingMa)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không được sửa mã chiến dịch.");
                return "redirect:/acvstore/chien-dich-giam-gia/edit/" + chienDich.getId();
            }
            chienDich.setMa(chienDichCu.getMa());
            chienDich.setTen(incomingTen);

            List<UUID> danhSachChiTietId = parseCsvUuids(selectedIdsRaw);
            String error = validateChienDich(chienDich, danhSachChiTietId, false, chienDichCu);
            if (error != null) {
                if (chienDich.getPhanTramGiam() != null) {
                    chienDich.setPhanTramGiam(chienDich.getPhanTramGiam().stripTrailingZeros());
                }

                model.addAttribute("errorMessage", error);

                Pageable pageable = PageRequest.of(0, 5);
                Page<SanPham> productPage = sanPhamService.getPagedProducts(pageable);
                model.addAttribute("productPage", productPage);
                model.addAttribute("currentProductPage", productPage.getNumber());
                model.addAttribute("totalProductPages", productPage.getTotalPages());
                model.addAttribute("selectedDetails", chienDichService.layChiTietDaChonTheoChienDich(chienDich.getId()));
                model.addAttribute("selectedProductIds", danhSachChiTietId.stream()
                        .map(id -> {
                            try {
                                return chienDichService.layChiTietTheoId(id).getSanPham().getId();
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()));
                model.addAttribute("isReadOnly", false);
                model.addAttribute("viewMode", false);
                model.addAttribute("status", currentStatus);
                addUserInfoToModel(model);
                return "WebQuanLy/discount-campaign-edit";
            }

            chienDich.setThoiGianTao(chienDichCu.getThoiGianTao());

            chienDichService.capNhatChienDichKemChiTiet(chienDich, danhSachChiTietId);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật chiến dịch thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật: " + e.getMessage());
        }
        return "redirect:/acvstore/chien-dich-giam-gia";
    }

    // ===== APIs phụ trợ =====
    @GetMapping("/chi-tiet-san-pham")
    @ResponseBody
    public List<ChiTietSanPham> layChiTietSanPhamTheoSanPham(@RequestParam("idSanPham") UUID idSanPham) {
        return chienDichService.layChiTietTheoSanPham(idSanPham);
    }

    @GetMapping("/chi-tiet-da-chon")
    @ResponseBody
    public List<ChiTietSanPham> layChiTietDaChon(@RequestParam("idChienDich") UUID idChienDich) {
        return chienDichService.layChiTietDaChonTheoChienDich(idChienDich);
    }

    @GetMapping("/multiple-product-details")
    @ResponseBody
    public List<ChiTietSanPham> getMultipleProductDetails(
            @RequestParam("productIds") List<UUID> productIds,
            @RequestParam(name = "currentCampaignId", required = false) UUID currentCampaignId
    ) {
        if (productIds == null || productIds.isEmpty()) return List.of();

        if (currentCampaignId != null) {
            return chienDichService.layChiTietConTrongTheoSanPham(productIds, currentCampaignId);
        }

        return productIds.stream()
                .flatMap(id -> chienDichService.layChiTietTheoSanPham(id).stream())
                .toList();
    }

    @PostMapping("/delete/{id}")
    public String xoaChienDich(@PathVariable("id") UUID id, RedirectAttributes redirectAttributes) {
        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/acvstore/chien-dich-giam-gia";
        }

        try {
            chienDichService.xoaChienDich(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa chiến dịch thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa: " + e.getMessage());
        }
        return "redirect:/acvstore/chien-dich-giam-gia";
    }

    @GetMapping("/search-products")
    @ResponseBody
    public Page<SanPham> timKiemSanPham(
            @RequestParam(name = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            @RequestParam(name = "selectedProductIds", required = false) List<UUID> selectedProductIds
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("tenSanPham").ascending());

        if (selectedProductIds != null && !selectedProductIds.isEmpty()) {
            if (keyword.isBlank()) {
                return sanPhamService.getPagedAvailableOrSelectedProducts(pageable, selectedProductIds);
            }
            return sanPhamService.searchAvailableOrSelectedByTenOrMa(keyword, pageable, selectedProductIds);
        }

        if (keyword.isBlank()) {
            return sanPhamService.getPagedAvailableProducts(pageable);
        }
        return sanPhamService.searchAvailableByTenOrMa(keyword, pageable);
    }

    // ===== Helpers =====
    private String getStatus(ChienDichGiamGia chienDich) {
        LocalDateTime now = LocalDateTime.now();
        if (chienDich.getNgayBatDau().isAfter(now)) return "UPCOMING";
        else if (chienDich.getNgayKetThuc().isBefore(now)) return "ENDED";
        return "ONGOING";
    }

    private List<UUID> parseCsvUuids(String csv) {
        if (csv == null || csv.isBlank()) return Collections.emptyList();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .collect(Collectors.toList());
    }

    private String validateChienDich(ChienDichGiamGia chienDich,
                                     List<UUID> chiTietIds,
                                     boolean isCreate,
                                     ChienDichGiamGia chienDichCu) {
        // ===== MÃ =====
        String ma = normalizeCode(chienDich.getMa());
        if (ma == null || ma.isEmpty()) {
            return "Mã chiến dịch không được để trống.";
        }
        if (ma.length() < 6 || ma.length() > 50) {
            return "Mã chiến dịch phải từ 6 đến 50 ký tự.";
        }
        if (!ma.matches("^[A-Za-z0-9]+$")) {
            return "Mã chiến dịch chỉ được chứa chữ và số, viết liền, không ký tự đặc biệt.";
        }
        if (isCreate) {
            if (chienDichService.kiemTraMaTonTai(ma)) {
                return "Mã chiến dịch đã tồn tại.";
            }
        } else {
            if (chienDichCu != null && !chienDichCu.getMa().equalsIgnoreCase(ma)) {
                return "Không được sửa mã chiến dịch.";
            }
        }

        // ===== TÊN (>= 6 ký tự, không tính khoảng trắng) =====
        String tenRaw = chienDich.getTen();
        if (tenRaw == null || tenRaw.isBlank()) {
            return "Tên chiến dịch không được để trống.";
        }

        // Chuẩn hoá: gộp nhiều khoảng trắng thành 1
        String tenTrim = tenRaw.trim().replaceAll("\\s+", " ");

        // Cho phép: chữ (có dấu), số, '-', '/'; các nhóm ngăn cách bởi đúng 1 khoảng trắng
        if (!tenTrim.matches("^[\\p{L}\\d\\-/]+(?: [\\p{L}\\d\\-/]+)*$")) {
            return "Tên chỉ được dùng chữ (có dấu), số, dấu gạch ngang (-) và dấu gạch chéo (/), giữa các nhóm cách 1 khoảng trắng.";
        }

        // Độ dài tối thiểu: 6 ký tự (không tính khoảng trắng)
        int tenNonSpaceLen = tenTrim.replaceAll("\\s+", "").length();
        if (tenNonSpaceLen < 6) {
            return "Tên chiến dịch phải có ít nhất 6 ký tự (không tính khoảng trắng).";
        }

        if (tenTrim.length() > 100) {
            return "Tên chiến dịch tối đa 100 ký tự.";
        }

        // Check trùng tên (nếu đổi tên)
        String tenCuTrim = (chienDichCu == null || chienDichCu.getTen() == null) ? "" : chienDichCu.getTen().trim();
        boolean tenChanged = (chienDichCu == null) || !tenTrim.equalsIgnoreCase(tenCuTrim);
        if (tenChanged && chienDichService.kiemTraTenTonTai(tenTrim)) {
            return "Tên chiến dịch đã tồn tại.";
        }

        // ===== PHẦN TRĂM GIẢM =====
        if (chienDich.getPhanTramGiam() == null) {
            return "Phần trăm giảm không được để trống.";
        }
        try {
            BigDecimal pt = chienDich.getPhanTramGiam().stripTrailingZeros();
            if (pt.scale() > 0) {
                return "Phần trăm giảm phải là số nguyên từ 1 đến 99.";
            }
            if (pt.compareTo(BigDecimal.ONE) < 0 || pt.compareTo(BigDecimal.valueOf(99)) > 0) {
                return "Phần trăm giảm phải là số nguyên từ 1 đến 99.";
            }
        } catch (Exception e) {
            return "Phần trăm giảm phải là số nguyên từ 1 đến 99.";
        }

        // ===== THỜI GIAN =====
        if (chienDich.getNgayBatDau() == null || chienDich.getNgayKetThuc() == null) {
            return "Ngày bắt đầu và ngày kết thúc không được để trống.";
        }
        if (chienDich.getNgayKetThuc().isBefore(chienDich.getNgayBatDau())) {
            return "Ngày kết thúc không được trước ngày bắt đầu.";
        }
        if (isCreate && chienDich.getNgayBatDau().isBefore(LocalDateTime.now())) {
            return "Ngày bắt đầu phải là hiện tại hoặc tương lai.";
        }

        // ===== SẢN PHẨM/CHI TIẾT =====
        if (chiTietIds == null || chiTietIds.isEmpty()) {
            return "Vui lòng chọn ít nhất một chi tiết sản phẩm.";
        }

        // Ghi lại giá trị đã chuẩn hoá
        chienDich.setMa(ma);
        chienDich.setTen(tenTrim);

        return null;
    }

}