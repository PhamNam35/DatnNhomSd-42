package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.entity.DiaChiNguoiDung;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.NguoiDung.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private NguoiDungService nguoiDungService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserDetailsService userDetailsService; // load theo email (username)

    @GetMapping
    public String viewProfile(Model model, Authentication authentication) {
        try {
            System.out.println("Authentication name: " + authentication.getName());
            System.out.println("Principal: " + authentication.getPrincipal());
            NguoiDung user = profileService.getCurrentUser();
            if (user == null || !user.getTrangThai()) {
                model.addAttribute("error", "Người dùng không tồn tại hoặc đã bị khóa");
                return "WebKhachHang/profile";
            }
            model.addAttribute("user", user); // Truyền đối tượng NguoiDung với key "user"
            model.addAttribute("addresses", profileService.getUserAddresses());
            return "WebKhachHang/profile";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi tải trang hồ sơ: " + e.getMessage());
            return "WebKhachHang/profile";
        }
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestParam String hoTen,
                                                             @RequestParam String email,
                                                             @RequestParam String soDienThoai,
                                                             @RequestParam(required = false) String matKhau,
                                                             Authentication authentication,
                                                             HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            NguoiDung existingUser = profileService.getCurrentUser();
            if (existingUser == null || !existingUser.getTrangThai()) {
                response.put("status", "error");
                response.put("message", "Người dùng không tồn tại hoặc đã bị khóa");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate email/phone trùng
            NguoiDung userByEmail = nguoiDungService.findByEmail(email);
            if (userByEmail != null && !userByEmail.getId().equals(existingUser.getId())) {
                response.put("status", "error");
                response.put("message", "Email đã tồn tại");
                return ResponseEntity.badRequest().body(response);
            }
            NguoiDung userByPhone = nguoiDungService.findBySoDienThoai(soDienThoai);
            if (userByPhone != null && !userByPhone.getId().equals(existingUser.getId())) {
                response.put("status", "error");
                response.put("message", "Số điện thoại đã tồn tại");
                return ResponseEntity.badRequest().body(response);
            }

            // Cập nhật DB
            existingUser.setHoTen(hoTen);
            existingUser.setEmail(email);
            existingUser.setSoDienThoai(soDienThoai);
            if (matKhau != null && !matKhau.isEmpty()) {
                existingUser.setMatKhau(passwordEncoder.encode(matKhau));
            }
            profileService.updateUser(existingUser);

            // === ĐĂNG XUẤT NGAY SAU KHI CẬP NHẬT ===
            var session = request.getSession(false);
            if (session != null) session.invalidate();
            SecurityContextHolder.clearContext();

            // Báo cho FE để chuyển hướng
            response.put("status", "success");
            response.put("message", "Cập nhật thành công. Vui lòng đăng nhập lại.");
            response.put("logout", true);
            response.put("redirect", "/login"); // đổi thành URL đăng nhập thực tế của bạn nếu khác
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Lỗi khi cập nhật hồ sơ: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/address/add-ajax")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> addAddress(@RequestBody DiaChiNguoiDung diaChi, Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = authentication.getName();
            NguoiDung user = profileService.getCurrentUser();
            if (user == null || !user.getTrangThai()) {
                response.put("status", "error");
                response.put("message", "Người dùng không tồn tại hoặc đã bị khóa");
                return ResponseEntity.badRequest().body(response);
            }

            diaChi.setNguoiDung(user);

            DiaChiNguoiDung savedAddress = profileService.addAddress(diaChi);
            response.put("status", "success");
            response.put("message", "Thêm địa chỉ thành công");
            response.put("address", savedAddress);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Lỗi khi thêm địa chỉ: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/address/{id}/update-ajax")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> updateAddress(@PathVariable UUID id, @RequestBody DiaChiNguoiDung diaChi, Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = authentication.getName();
            NguoiDung user = profileService.getCurrentUser();
            if (user == null || !user.getTrangThai()) {
                response.put("status", "error");
                response.put("message", "Người dùng không tồn tại hoặc đã bị khóa");
                return ResponseEntity.badRequest().body(response);
            }
            DiaChiNguoiDung updatedAddress = profileService.updateAddress(id, diaChi);

            updatedAddress.setNguoiNhan(diaChi.getNguoiNhan());
            updatedAddress.setSoDienThoaiNguoiNhan(diaChi.getSoDienThoaiNguoiNhan());

            response.put("status", "success");
            response.put("message", "Cập nhật địa chỉ thành công");
            response.put("address", updatedAddress);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Lỗi khi cập nhật địa chỉ: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/address/{id}/delete-ajax")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteAddress(@PathVariable UUID id, Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = authentication.getName();
            NguoiDung user = profileService.getCurrentUser();
            if (user == null || !user.getTrangThai()) {
                response.put("status", "error");
                response.put("message", "Người dùng không tồn tại hoặc đã bị khóa");
                return ResponseEntity.badRequest().body(response);
            }
            profileService.deleteAddress(id);
            response.put("status", "success");
            response.put("message", "Xóa địa chỉ thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Lỗi khi xóa địa chỉ: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/address/{id}/set-default-ajax")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> setDefaultAddress(@PathVariable UUID id, Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = authentication.getName();
            NguoiDung user = profileService.getCurrentUser();
            if (user == null || !user.getTrangThai()) {
                response.put("status", "error");
                response.put("message", "Người dùng không tồn tại hoặc đã bị khóa");
                return ResponseEntity.badRequest().body(response);
            }
            DiaChiNguoiDung address = profileService.setDefaultAddress(id);
            response.put("status", "success");
            response.put("message", "Đặt địa chỉ mặc định thành công");
            response.put("address", address);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Lỗi khi đặt địa chỉ mặc định: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/delete")
    public String deleteAccount(Authentication authentication, Model model) {
        try {
            String username = authentication.getName();
            NguoiDung user = profileService.getCurrentUser();
            if (user == null || !user.getTrangThai()) {
                model.addAttribute("error", "Người dùng không tồn tại hoặc đã bị khóa");
                return "WebKhachHang/profile";
            }
            profileService.deleteUser();
            return "redirect:/logout";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi xóa tài khoản: " + e.getMessage());
            return "WebKhachHang/profile";
        }
    }
}