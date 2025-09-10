package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.entity.XuatXu;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.XuatXuService;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/acvstore")
public class XuatXuController {

    @Autowired
    private XuatXuService xuatXuService;

    @Autowired
    private NguoiDungService nguoiDungService;

    @GetMapping("/xuat-xu")
    public String listXuatXu(@RequestParam(value = "search", required = false) String search,
                             @RequestParam(value = "error", required = false) String errorMessage,
                             @RequestParam(value = "page", defaultValue = "0") int page,
                             Model model) {

        Pageable pageable = PageRequest.of(page, 5);
        Page<XuatXu> xuatXuPage;

        try {
            xuatXuPage = search != null && !search.trim().isEmpty()
                    ? xuatXuService.searchXuatXu(search, pageable)
                    : xuatXuService.getAllXuatXu(pageable);

            if (xuatXuPage == null) {
                xuatXuPage = Page.empty(pageable);
            }

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Lỗi khi tải danh sách xuất xứ: " + e.getMessage());
            xuatXuPage = Page.empty(pageable);
        }

        model.addAttribute("xuatXuList", xuatXuPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", xuatXuPage.getTotalPages());
        model.addAttribute("search", search);

        // Lấy thông tin user hiện tại và phân quyền
        UserInfo userInfo = getCurrentUserInfo();
        model.addAttribute("user", userInfo.getUser());
        model.addAttribute("isAdmin", userInfo.isAdmin());

        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.addAttribute("errorMessage", errorMessage);
        }

        return "WebQuanLy/xuat-xu";
    }

    @PostMapping("/xuat-xu/save")
    public String saveXuatXu(@ModelAttribute XuatXu xuatXu,
                             RedirectAttributes redirectAttributes) {

        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền thực hiện thao tác này!");
            return "redirect:/acvstore/xuat-xu";
        }

        try {
            boolean isUpdate = xuatXu.getId() != null;
            xuatXuService.saveXuatXu(xuatXu);
            String message = isUpdate ? "Cập nhật xuất xứ thành công!" : "Thêm xuất xứ thành công!";
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lưu xuất xứ thất bại: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/acvstore/xuat-xu";
    }

    @GetMapping("/xuat-xu/delete/{id}")
    public String deleteXuatXu(@PathVariable UUID id,
                               RedirectAttributes redirectAttributes) {

        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền thực hiện thao tác này!");
            return "redirect:/acvstore/xuat-xu";
        }

        try {
            xuatXuService.deleteXuatXu(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa xuất xứ thành công!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Xóa xuất xứ thất bại: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/acvstore/xuat-xu";
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
