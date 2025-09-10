package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.dto.SanPham.SanPhamDto;
import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.dto.KhachHang.ChiTietSanPhamDto;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.repository.SanPham.DanhMucRepository;
import com.example.AsmGD1.repository.WebKhachHang.DanhGiaRepository;
import com.example.AsmGD1.repository.WebKhachHang.KhachHangSanPhamRepository;
import com.example.AsmGD1.repository.WebKhachHang.LichSuTimKiemRepository;
import com.example.AsmGD1.service.GioHang.KhachHangGioHangService;
import com.example.AsmGD1.service.WebKhachHang.KhachhangSanPhamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class HomeController {
    private static final Logger logger = LoggerFactory.getLogger(HomeController.class); // Sửa logger để dùng đúng class

    private final KhachhangSanPhamService khachhangSanPhamService;
    private final KhachHangGioHangService khachHangGioHangService;
    @Autowired
    private KhachHangSanPhamRepository khachHangSanPhamRepository;
    @Autowired
    private NguoiDungRepository nguoiDungRepository; // Thêm repository

    @Autowired
    private LichSuTimKiemRepository lichSuTimKiemRepository;

    @Autowired
    private DanhGiaRepository danhGiaRepository;
    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    public HomeController(KhachhangSanPhamService khachhangSanPhamService, KhachHangGioHangService khachHangGioHangService) {
        this.khachhangSanPhamService = khachhangSanPhamService;
        this.khachHangGioHangService = khachHangGioHangService;
    }


    @GetMapping("/api/products/prices")
    @ResponseBody
    public ResponseEntity<Map<UUID, Map<String, Object>>> getUpdatedPrices() {
        try {
            List<SanPhamDto> allProducts = khachhangSanPhamService.getAllActiveProductsDtos(); // Hoặc lấy từ cache nếu có
            Map<UUID, Map<String, Object>> prices = new HashMap<>();
            for (SanPhamDto product : allProducts) {
                Map<String, Object> info = new HashMap<>();
                // Giá sau khi giảm (nếu có flash sale hoặc campaign)
                info.put("giaSauGiam", product.getDiscountedPrice() != null ? product.getDiscountedPrice() : product.getPrice());
                // Giá gốc
                info.put("oldPrice", product.getOldPrice());
                // Phần trăm giảm giá
                info.put("discountPercentage", product.getDiscountPercentage() != null ? product.getDiscountPercentage() : BigDecimal.ZERO);
                // Tên chiến dịch giảm giá
                info.put("discountCampaignName", product.getDiscountCampaignName() != null ? product.getDiscountCampaignName() : "");
                // Tổng số lượng tồn kho
                info.put("tongSoLuong", product.getTongSoLuong());
                // Số lượng đã bán (nếu có)
                info.put("sold", product.getSold() != null ? product.getSold() : "0");
                // Tiến độ flash sale (nếu có)
                info.put("progress", product.getProgress() != null ? product.getProgress() : 0);

                prices.put(product.getId(), info);
            }
            return ResponseEntity.ok(prices);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy giá cập nhật: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/")
    public String home(Model model, Authentication authentication) {

        try {
            // ====== Data cho 3 section trên trang chủ ======
            // Sản phẩm mới (full + limited)
            List<SanPhamDto> newProductsAll = khachhangSanPhamService.getNewProducts();
            List<SanPhamDto> newProducts = newProductsAll.stream().limit(5).collect(Collectors.toList());

            // Tất cả sản phẩm (full + limited)
            List<SanPhamDto> allProductsAll = khachhangSanPhamService.getAllActiveProductsDtos();
            List<SanPhamDto> allProducts = allProductsAll.stream().limit(5).collect(Collectors.toList());

            // Bán chạy (full + limited)
            List<SanPhamDto> bestsellerProductsAll = khachhangSanPhamService.getBestSellingProducts();
            List<SanPhamDto> bestsellerProducts = bestsellerProductsAll.stream().limit(5).collect(Collectors.toList());

            // Gắn vào model cho index
            model.addAttribute("newProducts", newProducts);
            model.addAttribute("allProducts", allProducts);
            model.addAttribute("bestsellerProducts", bestsellerProducts);
            model.addAttribute("categories", khachhangSanPhamService.getActiveCategories());

            // (optional) nếu cần dùng ở nơi khác:
            model.addAttribute("newProductsAll", newProductsAll);
            model.addAttribute("allProductsAll", allProductsAll);
            model.addAttribute("bestsellerProductsAll", bestsellerProductsAll);

            // ====== Auth + Cart (giữ nguyên style của bạn) ======
            if (authentication != null && authentication.isAuthenticated()) {
                try {
                    String email = extractEmailFromAuthentication(authentication);
                    if (email != null) {
                        NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("Người dùng với email " + email + " không tồn tại"));

                        model.addAttribute("loggedInUser", nguoiDung);
                        model.addAttribute("user", nguoiDung);

                        if ("customer".equals(nguoiDung.getVaiTro())) {
                            GioHang gioHang = khachHangGioHangService.getOrCreateGioHang(nguoiDung.getId());
                            model.addAttribute("gioHangId", gioHang.getId());
                            model.addAttribute("chiTietGioHang",
                                    khachHangGioHangService.getGioHangChiTiets(gioHang.getId()) != null
                                            ? khachHangGioHangService.getGioHangChiTiets(gioHang.getId())
                                            : Collections.emptyList());
                            model.addAttribute("tongTien",
                                    gioHang.getTongTien() != null ? gioHang.getTongTien() : BigDecimal.ZERO);
                        }
                    }
                } catch (Exception e) {
                    model.addAttribute("cartError", "Không thể tải giỏ hàng hoặc thông tin người dùng: " + e.getMessage());
                }
            } else {
                model.addAttribute("loggedInUser", null);
                model.addAttribute("user", null);
            }

        } catch (Exception ex) {
            model.addAttribute("newProducts", Collections.emptyList());
            model.addAttribute("allProducts", Collections.emptyList());
            model.addAttribute("bestsellerProducts", Collections.emptyList());
            model.addAttribute("homeError", "Lỗi tải dữ liệu trang chủ: " + ex.getMessage());
        }

        return "WebKhachHang/index";
    }

    @GetMapping("/new")
    public String pageNewProducts(Model model, Authentication authentication) {
        model.addAttribute("products", khachhangSanPhamService.getNewProducts());
        commonUserCart(model, authentication); // optional: tách dùng chung
        return "WebKhachHang/list-new";
    }

    @GetMapping("/all")
    public String pageAllProducts(Model model, Authentication authentication) {
        model.addAttribute("products", khachhangSanPhamService.getAllActiveProductsDtos());
        commonUserCart(model, authentication);
        return "WebKhachHang/list-all";
    }

    @GetMapping("/bestsellers")
    public String pageBestSellers(Model model, Authentication authentication) {
        model.addAttribute("products", khachhangSanPhamService.getBestSellingProducts());
        commonUserCart(model, authentication);
        return "WebKhachHang/list-bestsellers";
    }
    @GetMapping("/chinh-sach")
    public String chinhSach(Model model, Authentication authentication) {
        commonUserCart(model, authentication);
        return "WebKhachHang/chinh-sach";
    }

    @GetMapping("/category/{id}")
    public String categoryPage(@PathVariable UUID id, Model model, Authentication authentication){
        model.addAttribute("products", khachhangSanPhamService.getProductsByCategory(id)); // id phải là id danh mục
        DanhMuc dm = danhMucRepository.findById(id).orElse(null);
        model.addAttribute("category", dm);
        model.addAttribute("currentCategoryId", id);
        commonUserCart(model, authentication);
        return "WebKhachHang/list-category";
    }


    /* Optional helper để không lặp lại */
    private void commonUserCart(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                String email = extractEmailFromAuthentication(authentication);
                if (email != null) {
                    NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email).orElse(null);
                    model.addAttribute("loggedInUser", nguoiDung);
                    model.addAttribute("user", nguoiDung);
                    if (nguoiDung != null && "customer".equals(nguoiDung.getVaiTro())) {
                        GioHang gioHang = khachHangGioHangService.getOrCreateGioHang(nguoiDung.getId());
                        model.addAttribute("gioHangId", gioHang.getId());
                        model.addAttribute("chiTietGioHang",
                                khachHangGioHangService.getGioHangChiTiets(gioHang.getId()) != null
                                        ? khachHangGioHangService.getGioHangChiTiets(gioHang.getId())
                                        : Collections.emptyList());
                        model.addAttribute("tongTien",
                                gioHang.getTongTien() != null ? gioHang.getTongTien() : BigDecimal.ZERO);
                    }
                }
            } catch (Exception ignored) {}
        } else {
            model.addAttribute("loggedInUser", null);
            model.addAttribute("user", null);
        }
    }


    @GetMapping("/chitietsanpham")
    public String productDetail(@RequestParam("id") UUID sanPhamId, Model model) {
        ChiTietSanPhamDto productDetail = khachhangSanPhamService.getProductDetail(sanPhamId);
        if (productDetail == null) {
            model.addAttribute("error", "Sản phẩm không tồn tại hoặc đã bị xóa!");
            return "WebKhachHang/error";
        }

        // Lấy min/max giá của tất cả biến thể thuộc sản phẩm
        Map<String, BigDecimal> range = khachhangSanPhamService.getPriceRangeBySanPhamId(sanPhamId);
        BigDecimal minPrice = range.getOrDefault("minPrice", productDetail.getGia());
        BigDecimal maxPrice = range.getOrDefault("maxPrice", productDetail.getGia());
        BigDecimal oldMinPrice = range.getOrDefault("oldMinPrice", productDetail.getOldPrice());
        BigDecimal oldMaxPrice = range.getOrDefault("oldMaxPrice", productDetail.getOldPrice());

        List<SanPhamDto> sanPhamLienQuan = khachhangSanPhamService.getSanPhamLienQuan(sanPhamId, 6);

        model.addAttribute("productDetail", productDetail);
        model.addAttribute("sanPhamLienQuan", sanPhamLienQuan);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("oldMinPrice", oldMinPrice);
        model.addAttribute("oldMaxPrice", oldMaxPrice);

        return "WebKhachHang/chitietsanpham";
    }

    @GetMapping("/don-mua")
    public String donMuaPage(Model model, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof NguoiDung user) {
            model.addAttribute("user", user);
            // model.addAttribute("orders", donHangService.findByCustomerId(user.getId()));
            return "WebKhachHang/don-mua"; // trang hiển thị đơn hàng đã mua
        }
        return "redirect:/customers/login";
    }
    // Quick View – trả JSON tóm tắt sản phẩm theo ID để modal hiển thị nhanh
    @GetMapping("/api/product/{id}")
    @ResponseBody
    public ResponseEntity<?> getProductSummary(@PathVariable UUID id) {
        try {
            // Lấy thông tin sản phẩm theo ID (bạn đã dùng ở trang chi tiết)
            ChiTietSanPhamDto detail = khachhangSanPhamService.getProductDetail(id);
            if (detail == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Not found", "id", id.toString()));
            }

            // Lấy min price cho đẹp (nếu có range), fallback = detail.getGia()
            Map<String, BigDecimal> range = khachhangSanPhamService.getPriceRangeBySanPhamId(id);
            BigDecimal minPrice = range != null
                    ? range.getOrDefault("minPrice", detail.getGia())
                    : detail.getGia();

            // Ảnh đại diện: ưu tiên ảnh list, không có thì dùng urlHinhAnh đơn
            String img = (detail.getHinhAnhList() != null && !detail.getHinhAnhList().isEmpty())
                    ? detail.getHinhAnhList().get(0)
                    : detail.getUrlHinhAnh();

            Map<String, Object> resp = new HashMap<>();
            resp.put("id", id);
            resp.put("tenSanPham", detail.getTenSanPham());
            resp.put("urlHinhAnh", img);
            resp.put("price", minPrice);
            resp.put("tongSoLuong", detail.getSoLuongTonKho());
            resp.put("moTa", detail.getMoTa());

            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            logger.error("Lỗi lấy product {} cho Quick View", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Server error"));
        }
    }
    @GetMapping("/api/product/{sanPhamId}/options")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getProductOptions(@PathVariable UUID sanPhamId) {
        List<ChiTietSanPham> list =
                khachHangSanPhamRepository.findActiveProductDetailsBySanPhamId(sanPhamId);

        if (list == null || list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("sizes", List.of(), "colors", List.of()));
        }

        // DISTINCT theo id & giữ thứ tự
        Map<UUID, Map<String, Object>> sizeMap = new LinkedHashMap<>();
        Map<UUID, Map<String, Object>> colorMap = new LinkedHashMap<>();

        for (ChiTietSanPham ct : list) {
            sizeMap.putIfAbsent(ct.getKichCo().getId(),
                    Map.of("id", ct.getKichCo().getId(), "ten", ct.getKichCo().getTen()));

            colorMap.putIfAbsent(ct.getMauSac().getId(),
                    Map.of("id", ct.getMauSac().getId(),
                            "tenMau", ct.getMauSac().getTenMau()));
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("sizes", new ArrayList<>(sizeMap.values()));
        resp.put("colors", new ArrayList<>(colorMap.values()));
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/cart")
    public String cart(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            model.addAttribute("error", "Vui lòng đăng nhập để xem giỏ hàng!");
            return "WebKhachHang/cart";
        }

        try {
            String email = extractEmailFromAuthentication(authentication);
            if (email == null) {
                model.addAttribute("error", "Không thể xác định người dùng. Vui lòng thử lại!");
                return "WebKhachHang/cart";
            }

            NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Người dùng với email " + email + " không tồn tại"));
            UUID nguoiDungId = nguoiDung.getId();
            GioHang gioHang = khachHangGioHangService.getOrCreateGioHang(nguoiDungId);
            model.addAttribute("gioHangId", gioHang.getId());
            model.addAttribute("chiTietGioHang", khachHangGioHangService.getGioHangChiTiets(gioHang.getId()) != null
                    ? khachHangGioHangService.getGioHangChiTiets(gioHang.getId()) : Collections.emptyList());
            model.addAttribute("tongTien", gioHang.getTongTien() != null ? gioHang.getTongTien() : BigDecimal.ZERO);
            model.addAttribute("loggedInUser", nguoiDung);
        } catch (Exception e) {
            model.addAttribute("error", "Không thể tải giỏ hàng: " + e.getMessage());
        }
        return "WebKhachHang/cart";
    }

    @GetMapping("/api/getChiTietSanPham")
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
            ChiTietSanPhamDto dto = khachhangSanPhamService.convertToChiTietSanPhamDto(chiTiet); // Sử dụng DTO để lấy cả oldPrice
            Map<String, Object> response = new HashMap<>();
            response.put("id", dto.getId());
            response.put("gia", dto.getGia());
            response.put("oldPrice", dto.getOldPrice()); // Thêm giá gốc
            response.put("soLuongTonKho", dto.getSoLuongTonKho());
            response.put("images", dto.getHinhAnhList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy chi tiết sản phẩm với sanPhamId={}, sizeId={}, colorId={}: ", sanPhamId, sizeId, colorId, e);
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi lấy chi tiết sản phẩm: " + e.getMessage()));
        }
    }

    private String extractEmailFromAuthentication(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            return (String) oauthToken.getPrincipal().getAttributes().get("email");
        } else if (authentication.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) authentication.getPrincipal();
            return user.getEmail();
        }
        return null; // Trường hợp không xác định được email
    }

    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<List<SanPhamDto>> searchProducts(@RequestParam("keyword") String keyword) {
        try {
            List<SanPhamDto> results = khachhangSanPhamService.searchProducts(keyword);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("Lỗi khi tìm kiếm sản phẩm với từ khóa {}: {}", keyword, e.getMessage());
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }
    }

    @GetMapping("/api/search-with-history")
    @ResponseBody
    public ResponseEntity<List<SanPhamDto>> searchProductsWithHistory(@RequestParam("keyword") String keyword, Authentication authentication) {
        try {
            NguoiDung nguoiDung = null;
            if (authentication != null && authentication.isAuthenticated()) {
                String email = extractEmailFromAuthentication(authentication);
                if (email != null) {
                    nguoiDung = nguoiDungRepository.findByEmail(email)
                            .orElse(null);
                }
            }
            List<SanPhamDto> results = khachhangSanPhamService.searchProductsWithHistory(keyword, nguoiDung);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("Lỗi khi tìm kiếm sản phẩm với từ khóa {}: {}", keyword, e.getMessage());
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }
    }

    // Phương thức mới: Lấy lịch sử tìm kiếm
    @GetMapping("/api/search-history")
    @ResponseBody
    public ResponseEntity<List<String>> getSearchHistory(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            String email = extractEmailFromAuthentication(authentication);
            if (email == null) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
            List<String> history = khachhangSanPhamService.getSearchHistory(nguoiDung.getId());
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy lịch sử tìm kiếm: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }
    }

    @GetMapping("/search")
    public String searchProducts(@RequestParam("keyword") String keyword, Model model, Authentication authentication) {
        try {
            NguoiDung nguoiDung = null;
            if (authentication != null && authentication.isAuthenticated()) {
                String email = extractEmailFromAuthentication(authentication);
                if (email != null) {
                    nguoiDung = nguoiDungRepository.findByEmail(email)
                            .orElse(null);
                    model.addAttribute("loggedInUser", nguoiDung);
                    model.addAttribute("user", nguoiDung);
                }
            } else {
                model.addAttribute("loggedInUser", null);
                model.addAttribute("user", null);
            }

            List<SanPhamDto> products = khachhangSanPhamService.searchProductsWithHistory(keyword, nguoiDung);
            model.addAttribute("products", products);
            model.addAttribute("keyword", keyword);
            return "WebKhachHang/search-results";
        } catch (Exception e) {
            logger.error("Lỗi khi tìm kiếm sản phẩm với từ khóa {}: {}", keyword, e.getMessage());
            model.addAttribute("error", "Lỗi khi tìm kiếm sản phẩm: " + e.getMessage());
            return "WebKhachHang/error";
        }
    }

    @DeleteMapping("/api/search-history")
    @ResponseBody
    public ResponseEntity<String> clearSearchHistory(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập!");
            }
            String email = extractEmailFromAuthentication(authentication);
            if (email == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Không thể xác định người dùng!");
            }
            NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
            lichSuTimKiemRepository.deleteByNguoiDungId(nguoiDung.getId());
            return ResponseEntity.ok("Đã xóa lịch sử tìm kiếm!");
        } catch (Exception e) {
            logger.error("Lỗi khi xóa lịch sử tìm kiếm: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi xóa lịch sử tìm kiếm!");
        }
    }

    // Trong HomeController.java
    @DeleteMapping("/api/search-history/delete")
    @ResponseBody
    public ResponseEntity<String> deleteSearchHistoryItem(@RequestParam("keyword") String keyword, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập để xóa lịch sử tìm kiếm!");
            }
            String email = extractEmailFromAuthentication(authentication);
            if (email == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Không thể xác định người dùng!");
            }
            NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
            lichSuTimKiemRepository.deleteByTuKhoaAndNguoiDungId(keyword, nguoiDung.getId());
            return ResponseEntity.ok("Đã xóa mục lịch sử tìm kiếm!");
        } catch (Exception e) {
            logger.error("Lỗi khi xóa mục lịch sử tìm kiếm: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi xóa mục lịch sử tìm kiếm!");
        }
    }

    @PostMapping("/api/save-search-history")
    @ResponseBody
    public ResponseEntity<String> saveSearchHistory(@RequestParam("keyword") String keyword, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Unauthorized attempt to save search history");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập để lưu lịch sử tìm kiếm!");
            }
            String email = extractEmailFromAuthentication(authentication);
            if (email == null) {
                logger.warn("Email extracted from authentication is null");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Không thể xác định người dùng!");
            }
            NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email)
                    .orElse(null);
            if (nguoiDung == null) {
                logger.warn("User not found for email: {}", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Người dùng không tồn tại!");
            }
            khachhangSanPhamService.saveSearchHistory(nguoiDung, keyword);
            return ResponseEntity.ok("Đã lưu lịch sử tìm kiếm!");
        } catch (Exception e) {
            logger.error("Lỗi khi lưu lịch sử tìm kiếm cho từ khóa '{}': {}", keyword, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi lưu lịch sử tìm kiếm: " + e.getMessage());
        }
    }

    @GetMapping("/api/product/{sanPhamId}/ratings")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getProductRatings(@PathVariable UUID sanPhamId, Authentication authentication) {
        List<DanhGia> allReviews = danhGiaRepository.findByChiTietSanPham_SanPham_IdOrderByThoiGianDanhGiaDesc(sanPhamId);

        UUID currentUserId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            String email = extractEmailFromAuthentication(authentication);
            if (email != null) {
                currentUserId = nguoiDungRepository.findByEmail(email)
                        .map(NguoiDung::getId)
                        .orElse(null);
            }
        }

        List<Map<String, Object>> myReviews = new ArrayList<>();
        List<Map<String, Object>> otherReviews = new ArrayList<>();

        for (DanhGia dg : allReviews) {
            Map<String, Object> map = new HashMap<>();
            map.put("user", dg.getNguoiDung().getHoTen());
            map.put("rating", dg.getXepHang());
            map.put("content", dg.getNoiDung());
            map.put("date", dg.getThoiGianDanhGia());
            map.put("media", dg.getUrlHinhAnh());

            if (currentUserId != null && dg.getNguoiDung().getId().equals(currentUserId)) {
                myReviews.add(map);
            } else {
                otherReviews.add(map);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("myReviews", myReviews);
        response.put("otherReviews", otherReviews);
        return ResponseEntity.ok(response);
    }


}