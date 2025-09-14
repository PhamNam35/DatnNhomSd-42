package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.entity.DayGiay;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.DayGiayService;
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
@RequestMapping("/polyshoe")
public class DayGiayController {

    @Autowired
    private DayGiayService dayGiayService;

    @Autowired
    private NguoiDungService nguoiDungService;

    @GetMapping("/day-giay")
    public String listDayGiay(@RequestParam(value = "search", required = false) String search,
                             @RequestParam(value = "error", required = false) String errorMessage,
                             @RequestParam(value = "page", defaultValue = "0") int page,
                             Model model) {

        Pageable pageable = PageRequest.of(page, 5);
        Page<DayGiay> dayGiayPage;

        try {
            dayGiayPage = search != null && !search.trim().isEmpty()
                    ? dayGiayService.searchDayGiay(search, pageable)
                    : dayGiayService.getAllDayGiay(pageable);

            if (dayGiayPage == null) {
                dayGiayPage = Page.empty(pageable);
            }

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Lỗi khi tải danh sách dây giày: " + e.getMessage());
            dayGiayPage = Page.empty(pageable);
        }

        model.addAttribute("dayGiayList", dayGiayPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", dayGiayPage.getTotalPages());
        model.addAttribute("search", search);

        // Lấy thông tin user hiện tại và phân quyền
        UserInfo userInfo = getCurrentUserInfo();
        model.addAttribute("user", userInfo.getUser());
        model.addAttribute("isAdmin", userInfo.isAdmin());

        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.addAttribute("errorMessage", errorMessage);
        }

        return "WebQuanLy/day-giay";
    }

    @PostMapping("/day-giay/save")
    public String saveDayGiay(@ModelAttribute DayGiay dayGiay,
                             RedirectAttributes redirectAttributes) {

        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền thực hiện thao tác này!");
            return "redirect:/polyshoe/day-giay";
        }

        try {
            boolean isUpdate = dayGiay.getId() != null;
            dayGiayService.saveDayGiay(dayGiay);
            String message = isUpdate ? "Cập nhật dây giày thành công!" : "Thêm dây giầy thành công!";
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lưu dây giầy thất bại: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/polyshoe/day-giay";
    }

    @GetMapping("/day-giay/delete/{id}")
    public String deleteDayGiay(@PathVariable UUID id,
                               RedirectAttributes redirectAttributes) {

        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền thực hiện thao tác này!");
            return "redirect:/polyshoe/day-giay";
        }

        try {
            dayGiayService.deleteDayGiay(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa dây giầy thành công!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Xóa dây giầy thất bại: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/polyshoe/day-giay";
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