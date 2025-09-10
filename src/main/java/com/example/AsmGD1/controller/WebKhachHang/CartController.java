package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.dto.BanHang.CartAddDto;
import com.example.AsmGD1.entity.ChiTietGioHang;
import com.example.AsmGD1.entity.GioHang;
import com.example.AsmGD1.entity.HoaDon;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.GioHang.GioHangRepository;
import com.example.AsmGD1.repository.HoaDon.HoaDonRepository;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.service.GioHang.ChiTietGioHangService;
import com.example.AsmGD1.service.GioHang.KhachHangGioHangService;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private KhachHangGioHangService khachHangGioHangService;

    @Autowired
    private ChiTietGioHangService chiTietGioHangService;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private NguoiDungService nguoiDungService;

    @Autowired
    private GioHangRepository gioHangRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCart(Authentication authentication) {
        try {
            UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
            if (nguoiDungId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Vui lòng đăng nhập để xem giỏ hàng"));
            }

            GioHang gioHang = khachHangGioHangService.getOrCreateGioHang(nguoiDungId);
            List<ChiTietGioHang> chiTietGioHangs = khachHangGioHangService.getGioHangChiTiets(gioHang.getId());

            List<Map<String, Object>> chiTietResponse = chiTietGioHangs.stream().map(item -> {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("id", item.getId());
                itemMap.put("soLuong", item.getSoLuong());
                itemMap.put("gia", item.getGia());
                itemMap.put("tienGiam", item.getTienGiam() != null ? item.getTienGiam() : BigDecimal.ZERO);
                itemMap.put("ghiChu", item.getGhiChu());
                itemMap.put("thoiGianThem", item.getThoiGianThem());
                itemMap.put("trangThai", item.getTrangThai());

                Map<String, Object> chiTietSanPhamMap = new HashMap<>();
                chiTietSanPhamMap.put("id", item.getChiTietSanPham().getId());
                chiTietSanPhamMap.put("gia", item.getChiTietSanPham().getGia());
                chiTietSanPhamMap.put("soLuongTonKho", item.getChiTietSanPham().getSoLuongTonKho());
                chiTietSanPhamMap.put("gioiTinh", item.getChiTietSanPham().getGioiTinh());
                chiTietSanPhamMap.put("thoiGianTao", item.getChiTietSanPham().getThoiGianTao());
                chiTietSanPhamMap.put("trangThai", item.getChiTietSanPham().getTrangThai());

                Map<String, Object> sanPhamMap = new HashMap<>();
                sanPhamMap.put("id", item.getChiTietSanPham().getSanPham().getId());
                sanPhamMap.put("maSanPham", item.getChiTietSanPham().getSanPham().getMaSanPham());
                sanPhamMap.put("tenSanPham", item.getChiTietSanPham().getSanPham().getTenSanPham());
                sanPhamMap.put("moTa", item.getChiTietSanPham().getSanPham().getMoTa());
                sanPhamMap.put("urlHinhAnh", item.getChiTietSanPham().getSanPham().getUrlHinhAnh());
                sanPhamMap.put("thoiGianTao", item.getChiTietSanPham().getSanPham().getThoiGianTao());
                sanPhamMap.put("trangThai", item.getChiTietSanPham().getSanPham().getTrangThai());
                sanPhamMap.put("tongSoLuong", item.getChiTietSanPham().getSanPham().getTongSoLuong());
                sanPhamMap.put("maxPrice", item.getChiTietSanPham().getSanPham().getMaxPrice());
                sanPhamMap.put("minPrice", item.getChiTietSanPham().getSanPham().getMinPrice());
                sanPhamMap.put("totalStockQuantity", item.getChiTietSanPham().getSanPham().getTotalStockQuantity());
                sanPhamMap.put("minPriceFormatted", item.getChiTietSanPham().getSanPham().getMinPriceFormatted());
                sanPhamMap.put("maxPriceFormatted", item.getChiTietSanPham().getSanPham().getMaxPriceFormatted());

                Map<String, Object> danhMucMap = new HashMap<>();
                danhMucMap.put("id", item.getChiTietSanPham().getSanPham().getDanhMuc().getId());
                danhMucMap.put("tenDanhMuc", item.getChiTietSanPham().getSanPham().getDanhMuc().getTenDanhMuc());
                sanPhamMap.put("danhMuc", danhMucMap);

                chiTietSanPhamMap.put("sanPham", sanPhamMap);

                if (item.getChiTietSanPham().getKichCo() != null) {
                    Map<String, Object> kichCoMap = new HashMap<>();
                    kichCoMap.put("id", item.getChiTietSanPham().getKichCo().getId());
                    kichCoMap.put("ten", item.getChiTietSanPham().getKichCo().getTen());
                    chiTietSanPhamMap.put("kichCo", kichCoMap);
                }

                if (item.getChiTietSanPham().getMauSac() != null) {
                    Map<String, Object> mauSacMap = new HashMap<>();
                    mauSacMap.put("id", item.getChiTietSanPham().getMauSac().getId());
                    mauSacMap.put("tenMau", item.getChiTietSanPham().getMauSac().getTenMau());
                    chiTietSanPhamMap.put("mauSac", mauSacMap);
                }

                if (item.getChiTietSanPham().getChatLieu() != null) {
                    Map<String, Object> chatLieuMap = new HashMap<>();
                    chatLieuMap.put("id", item.getChiTietSanPham().getChatLieu().getId());
                    chatLieuMap.put("tenChatLieu", item.getChiTietSanPham().getChatLieu().getTenChatLieu());
                    chiTietSanPhamMap.put("chatLieu", chatLieuMap);
                }

                if (item.getChiTietSanPham().getXuatXu() != null) {
                    Map<String, Object> xuatXuMap = new HashMap<>();
                    xuatXuMap.put("id", item.getChiTietSanPham().getXuatXu().getId());
                    xuatXuMap.put("tenXuatXu", item.getChiTietSanPham().getXuatXu().getTenXuatXu());
                    chiTietSanPhamMap.put("xuatXu", xuatXuMap);
                }

                if (item.getChiTietSanPham().getTayAo() != null) {
                    Map<String, Object> tayAoMap = new HashMap<>();
                    tayAoMap.put("id", item.getChiTietSanPham().getTayAo().getId());
                    tayAoMap.put("tenTayAo", item.getChiTietSanPham().getTayAo().getTenTayAo());
                    chiTietSanPhamMap.put("tayAo", tayAoMap);
                }

                if (item.getChiTietSanPham().getCoAo() != null) {
                    Map<String, Object> coAoMap = new HashMap<>();
                    coAoMap.put("id", item.getChiTietSanPham().getCoAo().getId());
                    coAoMap.put("tenCoAo", item.getChiTietSanPham().getCoAo().getTenCoAo());
                    chiTietSanPhamMap.put("coAo", coAoMap);
                }

                if (item.getChiTietSanPham().getKieuDang() != null) {
                    Map<String, Object> kieuDangMap = new HashMap<>();
                    kieuDangMap.put("id", item.getChiTietSanPham().getKieuDang().getId());
                    kieuDangMap.put("tenKieuDang", item.getChiTietSanPham().getKieuDang().getTenKieuDang());
                    chiTietSanPhamMap.put("kieuDang", kieuDangMap);
                }

                if (item.getChiTietSanPham().getThuongHieu() != null) {
                    Map<String, Object> thuongHieuMap = new HashMap<>();
                    thuongHieuMap.put("id", item.getChiTietSanPham().getThuongHieu().getId());
                    thuongHieuMap.put("tenThuongHieu", item.getChiTietSanPham().getThuongHieu().getTenThuongHieu());
                    chiTietSanPhamMap.put("thuongHieu", thuongHieuMap);
                }

                List<Map<String, Object>> hinhAnhList = item.getChiTietSanPham().getHinhAnhSanPhams().stream().map(hinhAnh -> {
                    Map<String, Object> hinhAnhMap = new HashMap<>();
                    hinhAnhMap.put("id", hinhAnh.getId());
                    hinhAnhMap.put("urlHinhAnh", hinhAnh.getUrlHinhAnh());
                    return hinhAnhMap;
                }).collect(Collectors.toList());
                chiTietSanPhamMap.put("hinhAnhSanPhams", hinhAnhList);

                itemMap.put("chiTietSanPham", chiTietSanPhamMap);
                return itemMap;
            }).collect(Collectors.toList());

            BigDecimal tongTien = chiTietGioHangs.stream()
                    .map(item -> item.getGia().multiply(BigDecimal.valueOf(item.getSoLuong())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (!tongTien.equals(gioHang.getTongTien())) {
                gioHang.setTongTien(tongTien);
                gioHangRepository.save(gioHang);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("gioHangId", gioHang.getId());
            response.put("chiTietGioHang", chiTietResponse);
            response.put("tongTien", tongTien);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không thể tải giỏ hàng: " + e.getMessage()));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addToCart(@RequestBody CartAddDto payload, Authentication authentication) {
        try {
            UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
            System.out.println("NguoiDungId: " + nguoiDungId);
            if (nguoiDungId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Vui lòng đăng nhập để thêm sản phẩm"));
            }

            UUID chiTietSanPhamId = payload.getId();
            int quantity = payload.getQuantity();

            GioHang gioHang = khachHangGioHangService.getOrCreateGioHang(nguoiDungId);
            khachHangGioHangService.addToGioHang(gioHang.getId(), chiTietSanPhamId, quantity);
            return ResponseEntity.ok(Map.of("message", "Sản phẩm đã được thêm vào giỏ hàng"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi thêm sản phẩm: " + e.getMessage()));
        }
    }

    @PutMapping("/update-quantity/{chiTietGioHangId}")
    public ResponseEntity<Map<String, Object>> updateQuantity(@PathVariable UUID chiTietGioHangId, @RequestParam Integer quantity, Authentication authentication) {
        try {
            UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
            if (nguoiDungId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Vui lòng đăng nhập để cập nhật giỏ hàng"));
            }
            chiTietGioHangService.updateSoLuong(chiTietGioHangId, quantity);
            return ResponseEntity.ok(Map.of("message", "Cập nhật số lượng thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi cập nhật số lượng: " + e.getMessage()));
        }
    }

    @DeleteMapping("/remove")
    public ResponseEntity<Map<String, Object>> removeFromCart(@RequestParam UUID gioHangId, @RequestParam UUID chiTietSanPhamId, Authentication authentication) {
        try {
            UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
            if (nguoiDungId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Vui lòng đăng nhập để xóa sản phẩm"));
            }
            chiTietGioHangService.removeChiTietGioHang(gioHangId, chiTietSanPhamId);
            return ResponseEntity.ok(Map.of("message", "Xóa sản phẩm thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xóa sản phẩm: " + e.getMessage()));
        }
    }

    @GetMapping("/check-auth")
    public ResponseEntity<Map<String, Boolean>> checkAuthentication(Authentication authentication) {
        Map<String, Boolean> response = new HashMap<>();
        System.out.println("Check-auth: Authentication = " + authentication);
        if (authentication != null) {
            System.out.println("Principal = " + authentication.getPrincipal());
            System.out.println("IsAuthenticated = " + authentication.isAuthenticated());
        }
        response.put("isAuthenticated", authentication != null && authentication.isAuthenticated());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/get-user")
    public ResponseEntity<?> getUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ResponseEntity.ok((NguoiDung) principal);
        } else if (principal instanceof OAuth2User) {
            String email = ((OAuth2User) principal).getAttribute("email");
            if (email == null) {
                return ResponseEntity.badRequest().body("Email không được tìm thấy");
            }
            NguoiDung nguoiDung = nguoiDungService.getUserByEmail(email);
            if (nguoiDung == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Người dùng không tồn tại");
            }
            return ResponseEntity.ok(nguoiDung);
        }
        return ResponseEntity.badRequest().body("Không thể xác định người dùng");
    }

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
//    private UUID getNguoiDungIdFromAuthentication(Authentication authentication) {
//        if (authentication == null || !authentication.isAuthenticated()) {
//            return null;
//        }
//
//        Object principal = authentication.getPrincipal();
//
//        if (principal instanceof NguoiDung) {
//            return ((NguoiDung) principal).getId();
//        }
//
//        // Nếu principal là String (username), thì fetch từ DB
//        if (principal instanceof String) {
//            NguoiDung nguoiDung = nguoiDungRepository.findByTenDangNhap((String) principal)
//                    .orElse(null);
//            return nguoiDung != null ? nguoiDung.getId() : null;
//        }
//
//        return null;
//    }
@GetMapping("/cart/add-from-order/{id}")
public String addToCartFromOrder(@PathVariable("id") UUID id, Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
        return "redirect:/dang-nhap";
    }
    NguoiDung nguoiDung = (NguoiDung) authentication.getPrincipal();
    HoaDon hoaDon = hoaDonRepository.findById(id).orElse(null);
    if (hoaDon == null || !hoaDon.getDonHang().getNguoiDung().getId().equals(nguoiDung.getId())) {
        return "redirect:/dsdon-mua";
    }
    // Logic để thêm các sản phẩm từ hoaDon.donHang.chiTietDonHangs vào giỏ hàng
    // (Cần thêm service để xử lý giỏ hàng)
    return "redirect:/cart";
}
}