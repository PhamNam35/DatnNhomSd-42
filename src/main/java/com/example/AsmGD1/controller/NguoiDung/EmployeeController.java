package com.example.AsmGD1.controller.NguoiDung;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.ThongBao.ThongBaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/acvstore/employees")
public class EmployeeController {

    @Autowired
    private NguoiDungService nguoiDungService;
    @Autowired
    private ThongBaoService thongBaoService;

    @GetMapping
    public String listEmployees(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "") String keyword,
                                @RequestParam(defaultValue = "") String vaiTro,
                                Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        NguoiDung currentUser = auth != null && auth.getPrincipal() instanceof NguoiDung ? (NguoiDung) auth.getPrincipal() : null;
        boolean isAdmin = currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getVaiTro());

        Page<NguoiDung> employees;
        if (vaiTro.isEmpty()) {
            employees = nguoiDungService.findUsersByVaiTroNotCustomer(keyword, page, 5);
        } else {
            employees = nguoiDungService.findUsersByVaiTro(vaiTro, keyword, page, 5);
        }

        model.addAttribute("employees", employees.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", employees.getTotalPages());
        model.addAttribute("pageSize", employees.getSize()); // ✅ Thêm dòng này
        model.addAttribute("keyword", keyword);
        model.addAttribute("vaiTro", vaiTro);
        model.addAttribute("employee", new NguoiDung());
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("currentUser", currentUser);

        return "WebQuanLy/list-nhan-vien";
    }


    @PostMapping("/add")
    public String addEmployee(@ModelAttribute("employee") NguoiDung employee, RedirectAttributes redirectAttributes, Authentication authentication) {
        try {
            NguoiDung currentUser = (NguoiDung) authentication.getPrincipal();

            if (!"ADMIN".equalsIgnoreCase(currentUser.getVaiTro())) {
                redirectAttributes.addFlashAttribute("message", "Bạn không có quyền thêm nhân viên!");
                redirectAttributes.addFlashAttribute("messageType", "danger");
                return "redirect:/acvstore/employees";
            }

            if (employee.getTrangThai() == null) {
                employee.setTrangThai(true);
            }

            // 💡 Lấy thông tin cần thiết NGAY trước khi session bị detach
            String hoTenNguoiThem = currentUser.getHoTen();
            String hoTenNhanVienMoi = employee.getHoTen();

            nguoiDungService.save(employee);

            thongBaoService.taoThongBaoHeThong(
                    "admin",
                    "Thêm nhân viên",
                    "Nhân viên " + hoTenNhanVienMoi + " đã được thêm bởi " + hoTenNguoiThem
            );

            redirectAttributes.addFlashAttribute("message", "Thêm nhân viên thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Thêm nhân viên thất bại: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        return "redirect:/acvstore/employees";
    }


    @PostMapping("/edit")
    public String editEmployee(@ModelAttribute("employee") NguoiDung employee, RedirectAttributes redirectAttributes, Authentication authentication) {
        try {
            NguoiDung currentUser = (NguoiDung) authentication.getPrincipal();

            if (!"ADMIN".equalsIgnoreCase(currentUser.getVaiTro())) {
                redirectAttributes.addFlashAttribute("message", "Bạn không có quyền sửa nhân viên!");
                redirectAttributes.addFlashAttribute("messageType", "danger");
                return "redirect:/acvstore/employees";
            }

            if (employee.getTrangThai() == null) {
                employee.setTrangThai(true);
            }

            nguoiDungService.save(employee);

            thongBaoService.taoThongBaoHeThong(
                    "admin",
                    "Cập nhật nhân viên",
                    "Thông tin nhân viên **" + employee.getHoTen() + "** đã được cập nhật bởi **" + currentUser.getHoTen() + "**"
            );

            redirectAttributes.addFlashAttribute("message", "Sửa nhân viên thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Sửa nhân viên thất bại: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        return "redirect:/acvstore/employees";
    }


    @PostMapping("/delete/{id}")
    public String deleteEmployee(@PathVariable UUID id, RedirectAttributes redirectAttributes, Authentication authentication) {
        try {
            NguoiDung currentUser = (NguoiDung) authentication.getPrincipal();

            if (!"ADMIN".equalsIgnoreCase(currentUser.getVaiTro())) {
                redirectAttributes.addFlashAttribute("message", "Bạn không có quyền xóa nhân viên!");
                redirectAttributes.addFlashAttribute("messageType", "danger");
                return "redirect:/acvstore/employees";
            }

            NguoiDung deletedUser = nguoiDungService.findById(id);
            nguoiDungService.deleteById(id);

            thongBaoService.taoThongBaoHeThong(
                    "admin",
                    "Xóa nhân viên",
                    "Nhân viên **" + deletedUser.getHoTen() + "** đã bị xóa bởi **" + currentUser.getHoTen() + "**"
            );

            redirectAttributes.addFlashAttribute("message", "Xóa nhân viên thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Xóa nhân viên thất bại: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        return "redirect:/acvstore/employees";
    }


    @GetMapping("/employee-dashboard")
    public String showEmployeeDashboard(Model model, Authentication authentication) {
        NguoiDung currentUser = (NguoiDung) authentication.getPrincipal();
        model.addAttribute("message", "Chào mừng Nhân viên đến với dashboard!");
        model.addAttribute("messageType", "success");
        model.addAttribute("user", currentUser);
        return "WebQuanLy/employee-dashboard";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "WebQuanLy/employee-login";
    }

    // Phần verify-face có thể tạm thời bỏ qua nếu chưa sử dụng
}