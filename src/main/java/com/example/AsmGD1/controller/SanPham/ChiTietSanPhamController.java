package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.dto.ChiTietSanPham.ChiTietSanPhamBatchDto;
import com.example.AsmGD1.dto.ChiTietSanPham.ChiTietSanPhamUpdateDto;
import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.repository.WebKhachHang.KhachHangSanPhamRepository;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/acvstore/chi-tiet-san-pham")
public class ChiTietSanPhamController {

    private static final Logger logger = LoggerFactory.getLogger(ChiTietSanPhamController.class);

    @Autowired private ChiTietSanPhamService chiTietSanPhamService;
    @Autowired private SanPhamService sanPhamService;
    @Autowired private MauSacService mauSacService;
    @Autowired private KichCoService kichCoService;
    @Autowired private ChatLieuService chatLieuService;
    @Autowired private XuatXuService xuatXuService;
    @Autowired private TayAoService tayAoService;
    @Autowired private CoAoService coAoService;
    @Autowired private KieuDangService kieuDangService;
    @Autowired private ThuongHieuService thuongHieuService;
    @Autowired private DanhMucService danhMucService;
    @Autowired private KhachHangSanPhamRepository khachHangSanPhamRepository;
    @Autowired private NguoiDungService nguoiDungService;
    @Autowired private ChiTietSanPhamRepository chiTietSanPhamRepository;
    @Autowired private SimpMessagingTemplate messagingTemplate; // Thêm để gửi message WebSocket

    // Helper method to check if current user is admin
    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) auth.getPrincipal();
            return "admin".equalsIgnoreCase(user.getVaiTro());
        }
        return false;
    }

    // Helper method to check if current user can edit
    private boolean canCurrentUserEdit() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) auth.getPrincipal();
            String role = user.getVaiTro();
            return "admin".equalsIgnoreCase(role);
        }
        return false;
    }

    // Helper method to check if user can view (both admin and employee)
    private boolean canCurrentUserView() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) auth.getPrincipal();
            String role = user.getVaiTro();
            return "admin".equalsIgnoreCase(role) || "employee".equalsIgnoreCase(role);
        }
        return false;
    }

    // Helper method to check if user can scan QR (admin or employee)
    private boolean canCurrentUserScan() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) auth.getPrincipal();
            String role = user.getVaiTro();
            return "admin".equalsIgnoreCase(role) || "employee".equalsIgnoreCase(role);
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
            model.addAttribute("canEdit", "admin".equalsIgnoreCase(user.getVaiTro()));
            model.addAttribute("isEmployee", "employee".equalsIgnoreCase(user.getVaiTro()));
        } else {
            List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
            model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
            model.addAttribute("isAdmin", false);
            model.addAttribute("canEdit", false);
            model.addAttribute("isEmployee", false);
        }
    }

    @GetMapping
    public String xemTatCa(Model model,
                           @RequestParam(value = "productId", required = false) UUID productId,
                           @RequestParam(value = "colorId", required = false) UUID colorId,
                           @RequestParam(value = "sizeId", required = false) UUID sizeId,
                           @RequestParam(value = "originId", required = false) UUID originId,
                           @RequestParam(value = "materialId", required = false) UUID materialId,
                           @RequestParam(value = "styleId", required = false) UUID styleId,
                           @RequestParam(value = "sleeveId", required = false) UUID sleeveId,
                           @RequestParam(value = "collarId", required = false) UUID collarId,
                           @RequestParam(value = "brandId", required = false) UUID brandId,
                           @RequestParam(value = "gender", required = false) String gender,
                           @RequestParam(value = "status", required = false) Boolean status) {
        try {
            if (!canCurrentUserView()) {
                return "redirect:/acvstore/login?error=Bạn không có quyền truy cập trang này";
            }

            addUserInfoToModel(model);

            model.addAttribute("sanPham", new SanPham());
            model.addAttribute("sanPhams", sanPhamService.findAll());
            model.addAttribute("mauSacs", chiTietSanPhamService.findColorsByProductId(productId));
            model.addAttribute("kichCos", chiTietSanPhamService.findSizesByProductId(productId));
            model.addAttribute("xuatXus", xuatXuService.getAllXuatXu());
            model.addAttribute("chatLieus", chatLieuService.getAllChatLieu());
            model.addAttribute("tayAos", tayAoService.getAllTayAo());
            model.addAttribute("coAos", coAoService.getAllCoAo());
            model.addAttribute("kieuDangs", kieuDangService.getAllKieuDang());
            model.addAttribute("thuongHieus", thuongHieuService.getAllThuongHieu());

            if (productId != null) {
                SanPham sanPhamDaChon = sanPhamService.findById(productId);
                if (sanPhamDaChon != null) {
                    model.addAttribute("selectedProductId", productId);
                    model.addAttribute("sanPhamDaChon", sanPhamDaChon);
                    List<ChiTietSanPham> chiTietList = chiTietSanPhamService.findByFilters(
                            productId, colorId, sizeId, originId, materialId, styleId, sleeveId, collarId, brandId, gender, status);
                    for (ChiTietSanPham pd : chiTietList) {
                        List<HinhAnhSanPham> images = chiTietSanPhamService.findHinhAnhSanPhamByChiTietSanPhamIdOrdered(pd.getId());
                        pd.setHinhAnhSanPhams(images);
                        logger.info("Tải {} ảnh cho chi tiết sản phẩm ID: {}", images.size(), pd.getId());
                    }
                    chiTietList.forEach(pd -> logger.info("Product ID: {}, Status: {}", pd.getId(), pd.getTrangThai()));
                    model.addAttribute("chiTietSanPhamList", chiTietList);
                    model.addAttribute("selectedColorId", colorId);
                    model.addAttribute("selectedSizeId", sizeId);
                    model.addAttribute("selectedOriginId", originId);
                    model.addAttribute("selectedMaterialId", materialId);
                    model.addAttribute("selectedStyleId", styleId);
                    model.addAttribute("selectedSleeveId", sleeveId);
                    model.addAttribute("selectedCollarId", collarId);
                    model.addAttribute("selectedBrandId", brandId);
                    model.addAttribute("selectedGender", gender);
                    model.addAttribute("selectedStatus", status);
                } else {
                    List<ChiTietSanPham> allDetails = chiTietSanPhamService.findAll();
                    for (ChiTietSanPham pd : allDetails) {
                        List<HinhAnhSanPham> images = chiTietSanPhamService.findHinhAnhSanPhamByChiTietSanPhamIdOrdered(pd.getId());
                        pd.setHinhAnhSanPhams(images);
                        logger.info("Tải {} ảnh cho chi tiết sản phẩm ID: {}", images.size(), pd.getId());
                    }
                    model.addAttribute("chiTietSanPhamList", allDetails);
                }
            } else {
                List<ChiTietSanPham> allDetails = chiTietSanPhamService.findAll();
                for (ChiTietSanPham pd : allDetails) {
                    List<HinhAnhSanPham> images = chiTietSanPhamService.findHinhAnhSanPhamByChiTietSanPhamIdOrdered(pd.getId());
                    pd.setHinhAnhSanPhams(images);
                    logger.info("Tải {} ảnh cho chi tiết sản phẩm ID: {}", images.size(), pd.getId());
                }
                model.addAttribute("chiTietSanPhamList", allDetails);
            }

            return "WebQuanLy/chi-tiet-san-pham-form";
        } catch (Exception e) {
            logger.error("Lỗi khi tải trang chi tiết sản phẩm: ", e);
            model.addAttribute("error", "Lỗi khi tải trang: " + e.getMessage());
            return "/WebQuanLy/error";
        }
    }

    @GetMapping("/add")
    public String hienThiFormThem(Model model, @RequestParam(value = "productId", required = false) UUID productId) {
        try {
            if (!canCurrentUserEdit()) {
                return "redirect:/acvstore/chi-tiet-san-pham?error=Bạn không có quyền truy cập chức năng này";
            }

            addUserInfoToModel(model);

            model.addAttribute("sanPham", new SanPham());
            model.addAttribute("sanPhams", sanPhamService.findAll());
            model.addAttribute("mauSacs", mauSacService.getAllMauSac());
            model.addAttribute("kichCos", kichCoService.getAllKichCo());
            model.addAttribute("chatLieus", chatLieuService.getAllChatLieu());
            model.addAttribute("xuatXus", xuatXuService.getAllXuatXu());
            model.addAttribute("tayAos", tayAoService.getAllTayAo());
            model.addAttribute("coAos", coAoService.getAllCoAo());
            model.addAttribute("kieuDangs", kieuDangService.getAllKieuDang());
            model.addAttribute("thuongHieus", thuongHieuService.getAllThuongHieu());

            List<DanhMuc> danhMucList = danhMucService.getAllDanhMuc();
            logger.info("Số lượng danh mục: {}", danhMucList.size());
            model.addAttribute("danhMucList", danhMucList);

            if (productId != null) {
                SanPham sanPhamDaChon = sanPhamService.findById(productId);
                model.addAttribute("selectedProductId", productId);
                model.addAttribute("sanPhamDaChon", sanPhamDaChon);
            }

            return "WebQuanLy/add-chi-tiet-san-pham-form";
        } catch (Exception e) {
            logger.error("Lỗi khi tải trang thêm chi tiết sản phẩm: ", e);
            model.addAttribute("error", "Lỗi khi tải trang: " + e.getMessage());
            return "/WebQuanLy/error";
        }
    }

    @GetMapping("/api/product-details/{productId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getProductDetails(@PathVariable UUID productId) {
        try {
            List<ChiTietSanPham> details = chiTietSanPhamService.findByProductId(productId);
            Map<String, Object> response = new HashMap<>();
            response.put("hasDetails", !details.isEmpty());

            if (!details.isEmpty()) {
                ChiTietSanPham firstDetail = details.get(0);
                response.put("originId", firstDetail.getXuatXu() != null ? firstDetail.getXuatXu().getId() : null);
                response.put("materialId", firstDetail.getChatLieu() != null ? firstDetail.getChatLieu().getId() : null);
                response.put("styleId", firstDetail.getKieuDang() != null ? firstDetail.getKieuDang().getId() : null);
                response.put("sleeveId", firstDetail.getTayAo() != null ? firstDetail.getTayAo().getId() : null);
                response.put("collarId", firstDetail.getCoAo() != null ? firstDetail.getCoAo().getId() : null);
                response.put("brandId", firstDetail.getThuongHieu() != null ? firstDetail.getThuongHieu().getId() : null);
                response.put("gender", firstDetail.getGioiTinh());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy chi tiết sản phẩm cho ID {}: ", productId, e);
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi lấy thông tin sản phẩm: " + e.getMessage()));
        }
    }

    @PostMapping("/update-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateStatus(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!canCurrentUserEdit()) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền thực hiện chức năng này!");
                return ResponseEntity.badRequest().body(response);
            }

            UUID id = UUID.fromString((String) payload.get("id"));
            Boolean trangThai = (Boolean) payload.get("trangThai");

            ChiTietSanPham chiTietSanPham = chiTietSanPhamService.findById(id);
            if (chiTietSanPham == null) {
                response.put("success", false);
                response.put("message", "Chi tiết sản phẩm không tồn tại!");
                return ResponseEntity.badRequest().body(response);
            }

            if (trangThai != null && trangThai && chiTietSanPham.getSoLuongTonKho() == 0) {
                response.put("success", false);
                response.put("message", "Không thể bật trạng thái 'Đang Bán' khi số lượng tồn kho bằng 0!");
                return ResponseEntity.badRequest().body(response);
            }

            chiTietSanPham.setTrangThai(trangThai != null ? trangThai : false);
            chiTietSanPhamService.save(chiTietSanPham);

            // Gửi thông báo WebSocket đến topic của sản phẩm
            UUID sanPhamId = chiTietSanPham.getSanPham().getId();
            messagingTemplate.convertAndSend("/topic/product/" + sanPhamId, Map.of("action", "refresh"));

            response.put("success", true);
            response.put("message", "Cập nhật trạng thái thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi cập nhật trạng thái: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/save")
    public String luuChiTietSanPhamDon(@ModelAttribute ChiTietSanPhamUpdateDto dto,
                                       @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
                                       Model model) {
        try {
            if (!canCurrentUserEdit()) {
                return "redirect:/acvstore/chi-tiet-san-pham?error=Bạn không có quyền thực hiện chức năng này";
            }

            chiTietSanPhamService.saveSingleChiTietSanPham(dto, imageFiles);

            // Gửi thông báo WebSocket đến topic của sản phẩm
            messagingTemplate.convertAndSend("/topic/product/" + dto.getProductId(), Map.of("action", "refresh"));

            return "redirect:/acvstore/chi-tiet-san-pham?productId=" + dto.getProductId() + "&success=Thêm thành công";
        } catch (Exception e) {
            logger.error("Lỗi khi lưu chi tiết sản phẩm: ", e);
            model.addAttribute("error", "Lỗi khi lưu chi tiết sản phẩm: " + e.getMessage());
            return "WebQuanLy/add-chi-tiet-san-pham-form";
        }
    }

    @PostMapping("/save-batch")
    public String luuChiTietSanPhamBatch(@ModelAttribute ChiTietSanPhamBatchDto batchDto, Model model) {
        try {
            if (!canCurrentUserEdit()) {
                return "redirect:/acvstore/chi-tiet-san-pham?error=Bạn không có quyền thực hiện chức năng này";
            }

            chiTietSanPhamService.saveChiTietSanPhamVariationsDto(batchDto);

            // Gửi thông báo WebSocket đến topic của sản phẩm
            messagingTemplate.convertAndSend("/topic/product/" + batchDto.getProductId(), Map.of("action", "refresh"));

            return "redirect:/acvstore/chi-tiet-san-pham?productId=" + batchDto.getProductId() + "&success=Thêm thành công";
        } catch (Exception e) {
            logger.error("Lỗi khi lưu batch chi tiết sản phẩm: ", e);
            model.addAttribute("error", "Lỗi khi lưu chi tiết sản phẩm: " + e.getMessage());
            return "WebQuanLy/add-chi-tiet-san-pham-form";
        }
    }

    @GetMapping("/edit/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> layChiTietSanPham(@PathVariable UUID id) {
        try {
            if (!canCurrentUserScan()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Bạn không có quyền truy cập chức năng này"));
            }

            ChiTietSanPham chiTiet = chiTietSanPhamService.findById(id);
            if (chiTiet == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy chi tiết sản phẩm"));
            }

            Map<String, Object> response = new HashMap<>();
            // Thông tin từ ChiTietSanPham
            response.put("id", chiTiet.getId());
            response.put("stockQuantity", chiTiet.getSoLuongTonKho());
            response.put("status", chiTiet.getTrangThai());
            response.put("thoiGianTao", chiTiet.getThoiGianTao() != null ? chiTiet.getThoiGianTao().toString() : null);
            response.put("gender", chiTiet.getGioiTinh());
            response.put("price", chiTiet.getGia());

            // Thông tin sản phẩm
            SanPham sanPham = chiTiet.getSanPham();
            if (sanPham != null) {
                response.put("productId", sanPham.getId());
                response.put("maSanPham", sanPham.getMaSanPham());
                response.put("tenSanPham", sanPham.getTenSanPham());
                response.put("tenDanhMuc", sanPham.getDanhMuc() != null ? sanPham.getDanhMuc().getTenDanhMuc() : null);
                response.put("urlHinhAnh", sanPham.getUrlHinhAnh());
            }

            // Màu & kích cỡ
            response.put("colorId", chiTiet.getMauSac() != null ? chiTiet.getMauSac().getId() : null);
            response.put("tenMauSac", chiTiet.getMauSac() != null ? chiTiet.getMauSac().getTenMau() : null);
            response.put("sizeId", chiTiet.getKichCo() != null ? chiTiet.getKichCo().getId() : null);
            response.put("tenKichCo", chiTiet.getKichCo() != null ? chiTiet.getKichCo().getTen() : null);

            // Các thuộc tính bổ sung
            response.put("originId", chiTiet.getXuatXu() != null ? chiTiet.getXuatXu().getId() : null);
            response.put("materialId", chiTiet.getChatLieu() != null ? chiTiet.getChatLieu().getId() : null);
            response.put("styleId", chiTiet.getKieuDang() != null ? chiTiet.getKieuDang().getId() : null);
            response.put("sleeveId", chiTiet.getTayAo() != null ? chiTiet.getTayAo().getId() : null);
            response.put("collarId", chiTiet.getCoAo() != null ? chiTiet.getCoAo().getId() : null);
            response.put("brandId", chiTiet.getThuongHieu() != null ? chiTiet.getThuongHieu().getId() : null);

            // Ảnh
            List<Map<String, Object>> images = chiTiet.getHinhAnhSanPhams() != null
                    ? chiTiet.getHinhAnhSanPhams().stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(h -> h.getThuTu() != null ? h.getThuTu() : Integer.MAX_VALUE))
                    .map(img -> {
                        Map<String, Object> imageMap = new HashMap<>();
                        imageMap.put("id", img.getId());
                        imageMap.put("imageUrl", img.getUrlHinhAnh());
                        imageMap.put("thuTu", img.getThuTu() != null ? img.getThuTu() : 0);
                        return imageMap;
                    })
                    .collect(Collectors.toList())
                    : new ArrayList<>();
            response.put("images", images);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy chi tiết sản phẩm ID {}: ", id, e);
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi tải chi tiết sản phẩm: " + e.getMessage()));
        }
    }

    @PostMapping("/update/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> capNhatChiTietSanPham(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean restricted,
            @ModelAttribute ChiTietSanPhamUpdateDto updateDto,
            @RequestParam(value = "imageFiles", required = false) MultipartFile[] imageFiles,
            @RequestParam(value = "deletedImageIds", required = false) String deletedImageIdsStr) {

        Map<String, Object> body = new HashMap<>();
        try {
            if (!canCurrentUserEdit()) {
                body.put("error", "Bạn không có quyền thực hiện chức năng này");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
            }

            ChiTietSanPham existing = chiTietSanPhamRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi tiết sản phẩm"));

            UUID productIdFromDb = (existing.getSanPham() != null) ? existing.getSanPham().getId() : null;

            updateDto.setId(id);
            if (updateDto.getProductId() == null && productIdFromDb != null) {
                updateDto.setProductId(productIdFromDb);
            }

            List<UUID> deletedIds = new ArrayList<>();
            if (deletedImageIdsStr != null && !deletedImageIdsStr.isBlank()) {
                for (String s : deletedImageIdsStr.split(",")) {
                    String trimmed = s.trim();
                    if (!trimmed.isEmpty()) {
                        try {
                            deletedIds.add(UUID.fromString(trimmed));
                        } catch (IllegalArgumentException ex) {
                            logger.warn("deletedImageIds chứa UUID không hợp lệ: {}", trimmed);
                        }
                    }
                }
            }

            MultipartFile[] safeImages = imageFiles;
            if (safeImages != null && safeImages.length > 0) {
                List<MultipartFile> nonEmpty = Arrays.stream(safeImages)
                        .filter(f -> f != null && !f.isEmpty())
                        .toList();
                safeImages = nonEmpty.toArray(new MultipartFile[0]);
            }

            if (restricted) {
                chiTietSanPhamService.updateChiTietSanPhamRestricted(updateDto, safeImages, deletedIds);
            } else {
                chiTietSanPhamService.updateChiTietSanPham(updateDto, safeImages, deletedIds);
            }

            // Gửi thông báo WebSocket đến topic của sản phẩm
            UUID productId = productIdFromDb != null ? productIdFromDb : updateDto.getProductId();
            messagingTemplate.convertAndSend("/topic/product/" + productId, Map.of("action", "refresh"));

            body.put("message", "Cập nhật chi tiết sản phẩm thành công");
            if (productId != null) body.put("productId", productId.toString());

            return ResponseEntity.ok(body);

        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật chi tiết sản phẩm ID {}: ", id, e);
            body.clear();
            body.put("error", "Lỗi khi cập nhật chi tiết sản phẩm: " + (e.getMessage() == null ? "Không xác định" : e.getMessage()));
            return ResponseEntity.badRequest().body(body);
        }
    }

    @PostMapping("/delete-image/{imageId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> xoaAnh(@PathVariable UUID imageId) {
        try {
            if (!canCurrentUserEdit()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Bạn không có quyền thực hiện chức năng này"));
            }

            chiTietSanPhamService.deleteImage(imageId);
            return ResponseEntity.ok(Map.of("message", "Xóa ảnh thành công"));
        } catch (Exception e) {
            logger.error("Lỗi khi xóa ảnh ID {}: ", imageId, e);
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xóa ảnh: " + e.getMessage()));
        }
    }

    @PostMapping("/update-bulk")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> capNhatHangLoat(
            @RequestParam("ids") String idsCsv,
            @ModelAttribute ChiTietSanPhamUpdateDto updateDto) {
        try {
            if (!canCurrentUserEdit()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Bạn không có quyền thực hiện chức năng này"));
            }

            List<UUID> ids = Arrays.stream(idsCsv.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .map(UUID::fromString).collect(Collectors.toList());

            boolean newStatus = updateDto.getStatus() != null ? updateDto.getStatus() :
                    (updateDto.getStockQuantity() != null && updateDto.getStockQuantity() > 0);
            if (newStatus && (updateDto.getStockQuantity() == null || updateDto.getStockQuantity() == 0)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Không thể bật 'Đang Bán' khi tồn kho bằng 0!"));
            }

            int affected = chiTietSanPhamService.updateBulkFullAttributes(ids, updateDto);

            // Gửi thông báo WebSocket đến topic của sản phẩm (lấy từ một id bất kỳ)
            if (!ids.isEmpty()) {
                ChiTietSanPham sample = chiTietSanPhamService.findById(ids.get(0));
                if (sample != null && sample.getSanPham() != null) {
                    UUID sanPhamId = sample.getSanPham().getId();
                    messagingTemplate.convertAndSend("/topic/product/" + sanPhamId, Map.of("action", "refresh"));
                }
            }

            return ResponseEntity.ok(Map.of("message", "Đã cập nhật " + affected + " biến thể."));
        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật hàng loạt: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi cập nhật hàng loạt: " + e.getMessage()));
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
            if (!canCurrentUserEdit()) {
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

    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getChiTietSanPhamId(
            @RequestParam("sanPhamId") UUID sanPhamId,
            @RequestParam("sizeId") UUID sizeId,
            @RequestParam("colorId") UUID colorId) {
        try {
            ChiTietSanPham chiTiet = khachHangSanPhamRepository.findBySanPhamIdAndSizeIdAndColorId(sanPhamId, sizeId, colorId);
            if (chiTiet == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Sản phẩm không tồn tại"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", chiTiet.getId());
            response.put("gia", chiTiet.getGia());
            response.put("soLuongTonKho", chiTiet.getSoLuongTonKho());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy chi tiết sản phẩm với sanPhamId={}, sizeId={}, colorId={}: ", sanPhamId, sizeId, colorId, e);
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi lấy chi tiết sản phẩm: " + e.getMessage()));
        }
    }

    @PostMapping("/scan-qr/add-stock")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> scanQrAddStock(
            @RequestParam("qr") String qrContent,
            @RequestParam(value = "qty", defaultValue = "1") int qty
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (!canCurrentUserScan()) {
                res.put("success", false);
                res.put("message", "Bạn không có quyền quét QR cho chức năng này!");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(res);
            }

            if (qty <= 0) {
                res.put("success", false);
                res.put("message", "Số lượng cộng phải > 0");
                return ResponseEntity.badRequest().body(res);
            }

            ChiTietSanPham target = null;

            try {
                UUID ctspId = UUID.fromString(qrContent.trim());
                target = chiTietSanPhamService.findById(ctspId);
            } catch (IllegalArgumentException ignore) {
            }

            if (target == null) {
                String normalized = qrContent.trim().replace(":", "|");
                String[] parts = normalized.split("\\|");
                if (parts.length == 3) {
                    try {
                        UUID sanPhamId = UUID.fromString(parts[0].trim());
                        UUID sizeId = UUID.fromString(parts[1].trim());
                        UUID colorId = UUID.fromString(parts[2].trim());

                        ChiTietSanPham byTriple = khachHangSanPhamRepository
                                .findBySanPhamIdAndSizeIdAndColorId(sanPhamId, sizeId, colorId);
                        if (byTriple != null) {
                            target = byTriple;
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            if (target == null) {
                res.put("success", false);
                res.put("message", "QR không hợp lệ hoặc không tìm thấy biến thể sản phẩm.");
                return ResponseEntity.badRequest().body(res);
            }

            int oldStock = target.getSoLuongTonKho() != null ? target.getSoLuongTonKho() : 0;
            int newStock = oldStock + qty;
            target.setSoLuongTonKho(newStock);

            if (Boolean.FALSE.equals(target.getTrangThai()) && newStock > 0) {
                target.setTrangThai(true);
            }

            chiTietSanPhamService.save(target);

            // Gửi thông báo WebSocket đến topic của sản phẩm
            UUID sanPhamId = target.getSanPham().getId();
            messagingTemplate.convertAndSend("/topic/product/" + sanPhamId, Map.of("action", "refresh"));

            res.put("success", true);
            res.put("message", "Đã cộng " + qty + " vào tồn kho.");
            res.put("chiTietSanPhamId", target.getId());
            res.put("soLuongTonKhoCu", oldStock);
            res.put("soLuongTonKhoMoi", newStock);
            res.put("status", target.getTrangThai());
            if (target.getSanPham() != null) {
                res.put("productId", target.getSanPham().getId());
                res.put("tenSanPham", target.getSanPham().getTenSanPham());
            }
            if (target.getKichCo() != null) {
                res.put("sizeId", target.getKichCo().getId());
                res.put("tenKichCo", target.getKichCo().getTen());
            }
            if (target.getMauSac() != null) {
                res.put("colorId", target.getMauSac().getId());
                res.put("tenMauSac", target.getMauSac().getTenMau());
            }

            return ResponseEntity.ok(res);
        } catch (Exception e) {
            logger.error("Lỗi khi quét QR add-stock: ", e);
            res.put("success", false);
            res.put("message", "Lỗi khi quét QR: " + (e.getMessage() == null ? "Không xác định" : e.getMessage()));
            return ResponseEntity.badRequest().body(res);
        }
    }
}