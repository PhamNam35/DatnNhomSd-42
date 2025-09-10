package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.dto.SanPham.SanPhamDto;
import com.example.AsmGD1.entity.DanhMuc;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.SanPham;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;  // Thêm import này
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/acvstore/san-pham")
public class SanPhamController {

    private static final Logger logger = LoggerFactory.getLogger(SanPhamController.class);

    @Autowired
    private SanPhamService sanPhamService;

    @Autowired
    private DanhMucService danhMucService;

    @Autowired
    private NguoiDungService nguoiDungService;

    @Autowired private ThuongHieuService thuongHieuService;
    @Autowired private KieuDangService kieuDangService;
    @Autowired private ChatLieuService chatLieuService;
    @Autowired private XuatXuService xuatXuService;
    @Autowired private TayAoService tayAoService;
    @Autowired private CoAoService coAoService;

    // Helper method to check if current user is admin
    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) auth.getPrincipal();
            return "admin".equalsIgnoreCase(user.getVaiTro());
        }
        return false;
    }

    // Helper method to add user info to model
    private void addUserInfoToModel(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) auth.getPrincipal();
            model.addAttribute("user", user);
            model.addAttribute("isAdmin", "admin".equalsIgnoreCase(user.getVaiTro()));
        } else {
            List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
            model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
            model.addAttribute("isAdmin", false);
        }
    }

    @GetMapping
    public String viewSanPhamPage(
            Model model,
            @RequestParam(value = "searchName", required = false) String searchName,
            @RequestParam(value = "trangThai", required = false) Boolean trangThai,
            @RequestParam(value = "danhMucId", required = false) UUID danhMucId,
            @RequestParam(value = "thuongHieuId", required = false) UUID thuongHieuId,
            @RequestParam(value = "kieuDangId", required = false) UUID kieuDangId,
            @RequestParam(value = "chatLieuId", required = false) UUID chatLieuId,
            @RequestParam(value = "xuatXuId", required = false) UUID xuatXuId,
            @RequestParam(value = "tayAoId", required = false) UUID tayAoId,
            @RequestParam(value = "coAoId", required = false) UUID coAoId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "sortField", required = false, defaultValue = "thoiGianTao") String sortField,  // Mặc định sort theo ngày tạo
            @RequestParam(value = "sortDir", required = false, defaultValue = "desc") String sortDir) {  // Mặc định desc (mới nhất đầu)

        addUserInfoToModel(model);

        // Xử lý sort: Đảm bảo field hợp lệ (maSanPham, tongSoLuong, thoiGianTao)
        String validSortField = switch (sortField) {
            case "maSanPham", "tongSoLuong", "thoiGianTao" -> sortField;
            default -> "thoiGianTao";  // Mặc định nếu field không hợp lệ
        };

        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, validSortField);

        Pageable pageable = PageRequest.of(page, 5, sort);  // Áp dụng sort vào Pageable

        Page<SanPham> sanPhamPage = sanPhamService.findByAdvancedFilters(
                searchName,
                trangThai,
                danhMucId,
                thuongHieuId,
                kieuDangId,
                chatLieuId,
                xuatXuId,
                tayAoId,
                coAoId,
                pageable
        );

        List<DanhMuc> danhMucList = danhMucService.getAllDanhMuc();

        model.addAttribute("sanPhamList", sanPhamPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", sanPhamPage.getTotalPages());
        model.addAttribute("searchName", searchName);
        model.addAttribute("selectedTrangThai", trangThai);
        model.addAttribute("sanPham", new SanPham());
        model.addAttribute("danhMucList", danhMucList);
        model.addAttribute("thuongHieuList", thuongHieuService.getAllThuongHieu());
        model.addAttribute("kieuDangList", kieuDangService.getAllKieuDang());
        model.addAttribute("chatLieuList", chatLieuService.getAllChatLieu());
        model.addAttribute("xuatXuList", xuatXuService.getAllXuatXu());
        model.addAttribute("tayAoList", tayAoService.getAllTayAo());
        model.addAttribute("coAoList", coAoService.getAllCoAo());
        model.addAttribute("selectedDanhMucId", danhMucId);
        model.addAttribute("selectedThuongHieuId", thuongHieuId);
        model.addAttribute("selectedKieuDangId", kieuDangId);
        model.addAttribute("selectedChatLieuId", chatLieuId);
        model.addAttribute("selectedXuatXuId", xuatXuId);
        model.addAttribute("selectedTayAoId", tayAoId);
        model.addAttribute("selectedCoAoId", coAoId);

        // Thêm param sort vào model để giữ trạng thái sort trong HTML
        model.addAttribute("sortField", validSortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equalsIgnoreCase("asc") ? "desc" : "asc");

        return "WebQuanLy/san-pham-list-form";
    }

    @GetMapping("/edit/{id}")
    public String editSanPham(@PathVariable("id") UUID id, Model model) {
        if (!isCurrentUserAdmin()) {
            return "redirect:/acvstore/san-pham?error=Bạn không có quyền truy cập chức năng này";
        }

        addUserInfoToModel(model);

        SanPham sanPham = sanPhamService.findById(id);
        if (sanPham == null) {
            model.addAttribute("error", "Sản phẩm không tồn tại");
            return "WebQuanLy/error";
        }

        List<DanhMuc> danhMucList = danhMucService.getAllDanhMuc();
        model.addAttribute("sanPham", sanPham);
        model.addAttribute("danhMucList", danhMucList);

        return "WebQuanLy/edit-san-pham-form";
    }

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveSanPham(
            @RequestParam("id") UUID id,
            @RequestParam("maSanPham") String maSanPham,
            @RequestParam("tenSanPham") String tenSanPham,
            @RequestParam(value = "moTa", required = false) String moTa,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "trangThai", defaultValue = "false") Boolean trangThai,
            @RequestParam(value = "danhMucId", required = false) UUID danhMucId) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();

        try {
            // Kiểm tra quyền admin
            if (!isCurrentUserAdmin()) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền thực hiện chức năng này!");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // Tìm sản phẩm theo ID
            SanPham sanPham = sanPhamService.findById(id);
            if (sanPham == null) {
                response.put("success", false);
                response.put("message", "Sản phẩm không tồn tại với ID: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // Validate tên sản phẩm
            if (tenSanPham == null || tenSanPham.trim().isEmpty()) {
                errors.put("editTenSanPham", "Tên sản phẩm không được để trống!");
            } else {
                tenSanPham = tenSanPham.trim();
                if (!tenSanPham.matches("^(?!\\s).*[^\\s]$")) {
                    errors.put("editTenSanPham", "Tên sản phẩm không được bắt đầu hoặc kết thúc bằng khoảng trắng!");
                } else if (!tenSanPham.matches("^[\\p{L}0-9\\s_-]+$")) {
                    errors.put("editTenSanPham", "Tên sản phẩm chỉ cho phép chữ cái, số, khoảng trắng, _, -!");
                } else if (!tenSanPham.equals(sanPham.getTenSanPham()) && sanPhamService.existsByTenSanPham(tenSanPham)) {
                    errors.put("editTenSanPham", "Tên sản phẩm đã tồn tại!");
                }
            }

            // Validate danh mục
            if (danhMucId == null) {
                errors.put("editDanhMuc", "Vui lòng chọn danh mục!");
            } else {
                DanhMuc danhMuc = danhMucService.getDanhMucById(danhMucId);
                if (danhMuc == null) {
                    errors.put("editDanhMuc", "Danh mục không tồn tại với ID: " + danhMucId);
                }
            }

            // Validate trạng thái
            if (trangThai != null && trangThai && !sanPhamService.hasActiveChiTietSanPham(id)) {
                errors.put("editTrangThai", "Không thể bật trạng thái sản phẩm vì không có chi tiết sản phẩm nào đang hoạt động!");
            }

            // Nếu có lỗi validate, trả về danh sách lỗi
            if (!errors.isEmpty()) {
                response.put("success", false);
                response.put("message", "Dữ liệu không hợp lệ, vui lòng kiểm tra lại!");
                response.put("errors", errors);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Cập nhật sản phẩm
            sanPham.setTenSanPham(tenSanPham);
            sanPham.setMoTa(moTa);
            sanPham.setTrangThai(trangThai != null ? trangThai : false);

            if (danhMucId != null) {
                DanhMuc danhMuc = danhMucService.getDanhMucById(danhMucId);
                sanPham.setDanhMuc(danhMuc);
            }

            // Lưu trữ ảnh nếu có
            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    String UPLOAD_DIR = System.getProperty("os.name").toLowerCase().contains("win")
                            ? "C:/DATN/uploads/san_pham/"
                            : System.getProperty("user.home") + "/DATN/uploads/san_pham/";
                    String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
                    Path filePath = Paths.get(UPLOAD_DIR, fileName);
                    Files.createDirectories(filePath.getParent());
                    Files.write(filePath, imageFile.getBytes());
                    sanPham.setUrlHinhAnh("/images/" + fileName);
                } catch (IOException e) {
                    response.put("success", false);
                    response.put("message", "Lỗi khi lưu tệp ảnh: " + e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                }
            }

            // Lưu sản phẩm
            sanPhamService.save(sanPham);

            // Chuẩn bị response
            SanPhamDto dto = new SanPhamDto();
            dto.setId(sanPham.getId());
            dto.setMaSanPham(sanPham.getMaSanPham());
            dto.setTenSanPham(sanPham.getTenSanPham());
            dto.setMoTa(sanPham.getMoTa());
            dto.setUrlHinhAnh(sanPham.getUrlHinhAnh());
            dto.setTrangThai(sanPham.getTrangThai());
            dto.setThoiGianTao(sanPham.getThoiGianTao());
            dto.setTongSoLuong(sanPham.getTongSoLuong());

            if (sanPham.getDanhMuc() != null) {
                dto.setDanhMucId(sanPham.getDanhMuc().getId());
                dto.setTenDanhMuc(sanPham.getDanhMuc().getTenDanhMuc());
            }

            response.put("success", true);
            response.put("message", "Lưu sản phẩm thành công!");
            response.put("sanPham", dto);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Dữ liệu đầu vào không hợp lệ: ", e);
            response.put("success", false);
            response.put("message", "Dữ liệu đầu vào không hợp lệ: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            logger.error("Lỗi hệ thống khi lưu sản phẩm: ", e);
            response.put("success", false);
            response.put("message", "Lỗi hệ thống khi lưu sản phẩm: " + (e.getMessage() != null ? e.getMessage() : "Không xác định"));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/get/{id}")
    @ResponseBody
    public ResponseEntity<SanPhamDto> getSanPham(@PathVariable("id") UUID id) {
        SanPham sp = sanPhamService.findById(id);
        if (sp == null) return ResponseEntity.notFound().build();

        SanPhamDto dto = new SanPhamDto();
        dto.setId(sp.getId());
        dto.setMaSanPham(sp.getMaSanPham());
        dto.setTenSanPham(sp.getTenSanPham());
        dto.setMoTa(sp.getMoTa());
        dto.setUrlHinhAnh(sp.getUrlHinhAnh());
        dto.setTrangThai(sp.getTrangThai());
        dto.setThoiGianTao(sp.getThoiGianTao());
        dto.setTongSoLuong(sp.getTongSoLuong());

        if (sp.getDanhMuc() != null) {
            dto.setDanhMucId(sp.getDanhMuc().getId());
            dto.setTenDanhMuc(sp.getDanhMuc().getTenDanhMuc());
        }

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/update-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateStatus(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!isCurrentUserAdmin()) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền thực hiện chức năng này!");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            UUID id;
            try {
                id = UUID.fromString((String) payload.get("id"));
            } catch (IllegalArgumentException e) {
                response.put("success", false);
                response.put("message", "ID sản phẩm không hợp lệ: " + payload.get("id"));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            Boolean trangThai = (Boolean) payload.get("trangThai");
            if (trangThai == null) {
                response.put("success", false);
                response.put("message", "Trạng thái không được để trống!");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            SanPham sanPham = sanPhamService.findById(id);
            if (sanPham == null) {
                response.put("success", false);
                response.put("message", "Sản phẩm không tồn tại với ID: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            if (trangThai && !sanPhamService.hasActiveChiTietSanPham(id)) {
                response.put("success", false);
                response.put("message", "Không thể bật trạng thái sản phẩm vì không có chi tiết sản phẩm nào đang hoạt động!");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            sanPham.setTrangThai(trangThai);
            sanPhamService.save(sanPham);

            response.put("success", true);
            response.put("message", "Cập nhật trạng thái thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Lỗi hệ thống khi cập nhật trạng thái: ", e);
            response.put("success", false);
            response.put("message", "Lỗi hệ thống khi cập nhật trạng thái: " + (e.getMessage() != null ? e.getMessage() : "Không xác định"));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/save-auto-product")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveQuickAddProduct(
            @ModelAttribute SanPham sanPham,
            @RequestParam(value = "danhMuc.id", required = false) UUID danhMucId,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();

        try {
            // Check admin or employee permission
            if (!isCurrentUserAdmin()) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền thực hiện chức năng này!");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate maSanPham
            String maSanPham = sanPham.getMaSanPham();
            if (maSanPham == null || maSanPham.trim().isEmpty()) {
                errors.put("quickAddMaSanPham", "Mã sản phẩm không được để trống!");
            } else {
                maSanPham = maSanPham.trim();
                if (!maSanPham.matches("^[a-zA-Z0-9_-]+$")) {
                    errors.put("quickAddMaSanPham", "Mã sản phẩm không được chứa ký tự đặc biệt (chỉ cho phép chữ cái, số, _, -)!");
                } else if (sanPhamService.existsByMaSanPham(maSanPham)) {
                    errors.put("quickAddMaSanPham", "Mã sản phẩm đã tồn tại!");
                }
            }

            // Validate tenSanPham
            String tenSanPham = sanPham.getTenSanPham();
            if (tenSanPham == null || tenSanPham.trim().isEmpty()) {
                errors.put("quickAddTenSanPham", "Tên sản phẩm không được để trống!");
            } else {
                tenSanPham = tenSanPham.trim();
                if (!tenSanPham.matches("^(?!\\s).*[^\\s]$")) {
                    errors.put("quickAddTenSanPham", "Tên sản phẩm không được bắt đầu hoặc kết thúc bằng khoảng trắng!");
                } else if (!tenSanPham.matches("^[\\p{L}0-9\\s_-]+$")) {
                    errors.put("quickAddTenSanPham", "Tên sản phẩm chỉ cho phép chữ cái, số, khoảng trắng, _, -!");
                } else if (sanPhamService.existsByTenSanPham(tenSanPham)) {
                    errors.put("quickAddTenSanPham", "Tên sản phẩm đã tồn tại!");
                }
            }

            // Validate imageFile
            if (imageFile == null || imageFile.isEmpty()) {
                errors.put("quickAddImageFile", "Ảnh sản phẩm không được để trống!");
            }

            // Validate danhMuc
            if (danhMucId == null) {
                errors.put("quickAddDanhMuc", "Vui lòng chọn danh mục!");
            } else {
                DanhMuc danhMuc = danhMucService.getDanhMucById(danhMucId);
                if (danhMuc == null) {
                    errors.put("quickAddDanhMuc", "Danh mục không tồn tại!");
                }
            }

            // Nếu có lỗi validate, trả về danh sách lỗi
            if (!errors.isEmpty()) {
                response.put("success", false);
                response.put("errors", errors);
                return ResponseEntity.badRequest().body(response);
            }

            // Nếu validate thành công, lưu sản phẩm
            SanPham newSanPham = new SanPham();
            newSanPham.setMaSanPham(maSanPham);
            newSanPham.setTenSanPham(tenSanPham);
            newSanPham.setMoTa(sanPham.getMoTa());
            newSanPham.setTrangThai(true);
            newSanPham.setThoiGianTao(LocalDateTime.now());

            DanhMuc danhMuc = danhMucService.getDanhMucById(danhMucId);
            newSanPham.setDanhMuc(danhMuc);

            // Lưu trữ ảnh cục bộ
            String UPLOAD_DIR = System.getProperty("os.name").toLowerCase().contains("win")
                    ? "C:/DATN/uploads/san_pham/"
                    : System.getProperty("user.home") + "/DATN/uploads/san_pham/";
            if (imageFile != null && !imageFile.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
                Path filePath = Paths.get(UPLOAD_DIR, fileName);
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, imageFile.getBytes());
                newSanPham.setUrlHinhAnh("/images/" + fileName);
            }

            sanPhamService.save(newSanPham);

            response.put("success", true);
            response.put("id", newSanPham.getId());
            response.put("tenSanPham", newSanPham.getTenSanPham());
            response.put("message", "Thêm sản phẩm thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Lỗi khi thêm nhanh sản phẩm: ", e);
            response.put("success", false);
            response.put("message", "Lỗi khi thêm sản phẩm: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}