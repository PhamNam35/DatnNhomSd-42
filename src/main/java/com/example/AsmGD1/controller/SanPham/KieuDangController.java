package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.entity.KieuDang;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.KieuDangService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/acvstore")
public class KieuDangController {

    @Autowired
    private KieuDangService kieuDangService;

    @Autowired
    private NguoiDungService nguoiDungService;

    @GetMapping("/kieu-dang")
    public String listKieuDang(@RequestParam(value = "search", required = false) String search,
                               @RequestParam(value = "error", required = false) String errorMessage,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               Model model) {

        Pageable pageable = PageRequest.of(page, 5); // 5 items per page
        Page<KieuDang> kieuDangPage;

        try {
            kieuDangPage = search != null && !search.trim().isEmpty()
                    ? kieuDangService.searchKieuDang(search, pageable)
                    : kieuDangService.getAllKieuDang(pageable);

            if (kieuDangPage == null) {
                kieuDangPage = Page.empty(pageable);
            }

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Lỗi khi tải danh sách kiểu dáng: " + e.getMessage());
            kieuDangPage = Page.empty(pageable);
        }

        model.addAttribute("kieuDangList", kieuDangPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", kieuDangPage.getTotalPages());
        model.addAttribute("search", search);

        // Lấy thông tin user hiện tại và phân quyền
        UserInfo userInfo = getCurrentUserInfo();
        model.addAttribute("user", userInfo.getUser());
        model.addAttribute("isAdmin", userInfo.isAdmin());

        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.addAttribute("errorMessage", errorMessage);
        }

        return "WebQuanLy/kieu-dang";
    }

    @PostMapping("/kieu-dang/save")
    public String saveKieuDang(@ModelAttribute KieuDang kieuDang,
                               RedirectAttributes redirectAttributes) {

        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền thực hiện thao tác này!");
            return "redirect:/acvstore/kieu-dang";
        }

        try {
            // Kiểm tra id trước khi lưu để xác định là thêm mới hay cập nhật
            boolean isUpdate = kieuDang.getId() != null;
            kieuDangService.saveKieuDang(kieuDang);
            String message = isUpdate ? "Cập nhật kiểu dáng thành công!" : "Thêm kiểu dáng thành công!";
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lưu kiểu dáng thất bại: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/acvstore/kieu-dang";
    }

    @GetMapping("/kieu-dang/delete/{id}")
    public String deleteKieuDang(@PathVariable UUID id,
                                 RedirectAttributes redirectAttributes) {

        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền thực hiện thao tác này!");
            return "redirect:/acvstore/kieu-dang";
        }

        try {
            kieuDangService.deleteKieuDang(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa kiểu dáng thành công!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Xóa kiểu dáng thất bại: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/acvstore/kieu-dang";
    }

    // Helper methods
    private boolean isCurrentUserAdmin() {
        return getCurrentUserInfo().isAdmin();
    }

    private UserInfo getCurrentUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung currentUser = (NguoiDung) auth.getPrincipal();
            boolean isAdmin = "admin".equalsIgnoreCase(currentUser.getVaiTro());
            return new UserInfo(currentUser, isAdmin);
        }

        // Fallback - tạo user mặc định
        NguoiDung defaultUser = new NguoiDung();
        defaultUser.setTenDangNhap("guest");
        defaultUser.setVaiTro("employee");
        return new UserInfo(defaultUser, false);
    }

    // Inner class để đóng gói thông tin user
    private static class UserInfo {
        private final NguoiDung user;
        private final boolean isAdmin;

        public UserInfo(NguoiDung user, boolean isAdmin) {
            this.user = user;
            this.isAdmin = isAdmin;
        }

        public NguoiDung getUser() { return user; }
        public boolean isAdmin() { return isAdmin; }
    }
}