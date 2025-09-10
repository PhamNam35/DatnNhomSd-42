package com.example.AsmGD1.controller.NguoiDung;

import com.example.AsmGD1.entity.DiaChiNguoiDung;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.DiaChiKhachHangService;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/acvstore/customers")
public class CustomerController {

    @Autowired
    private NguoiDungService nguoiDungService;
    @Autowired
    private DiaChiKhachHangService diaChiKhachHangService;

    // Helper method
    // ể kiểm tra quyền admin
    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung currentUser = (NguoiDung) auth.getPrincipal();
            return "ADMIN".equalsIgnoreCase(currentUser.getVaiTro());
        }
        return false;
    }

    // Helper method để kiểm tra quyền employee
    private boolean isCurrentUserEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) auth.getPrincipal();
            return "EMPLOYEE".equalsIgnoreCase(user.getVaiTro());
        }
        return false;
    }

    // Helper method để kiểm tra quyền admin hoặc employee
    private boolean isCurrentUserAdminOrEmployee() {
        return isCurrentUserAdmin() || isCurrentUserEmployee();
    }

    // Helper method để thêm thông tin user vào model
    private void addUserInfoToModel(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) auth.getPrincipal();
            model.addAttribute("user", user);
            model.addAttribute("currentUser", user);
            model.addAttribute("isAdmin", "ADMIN".equalsIgnoreCase(user.getVaiTro()));
            model.addAttribute("isEmployee", "EMPLOYEE".equalsIgnoreCase(user.getVaiTro()));
        } else {
            List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("ADMIN", "", 0, 1).getContent();
            NguoiDung defaultUser = admins.isEmpty() ? new NguoiDung() : admins.get(0);
            model.addAttribute("user", defaultUser);
            model.addAttribute("currentUser", defaultUser);
            model.addAttribute("isAdmin", false);
            model.addAttribute("isEmployee", false);
        }
    }

    @GetMapping
    public String listCustomers(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "") String keyword,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (!isCurrentUserAdminOrEmployee()) {
            redirectAttributes.addFlashAttribute("message", "Bạn không có quyền truy cập chức năng này!");
            redirectAttributes.addFlashAttribute("messageType", "danger");
            return "redirect:/acvstore/thong-ke";
        }

        addUserInfoToModel(model);

        Page<NguoiDung> customers = nguoiDungService.findUsersByVaiTro("customer", keyword, page, 5);
        List<DiaChiNguoiDung> defaultAddresses = customers.getContent().stream()
                .map(customer -> diaChiKhachHangService.getDefaultAddress(customer.getId()))
                .toList();

        model.addAttribute("customers", customers.getContent());
        model.addAttribute("defaultAddresses", defaultAddresses);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", customers.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("customer", new NguoiDung());

        return "WebQuanLy/list-khach-hang";
    }

    @PostMapping("/add")
    public String addCustomer(@ModelAttribute("customer") NguoiDung customer,
                              @RequestParam(required = false) String tinhThanhPho,
                              @RequestParam(required = false) String quanHuyen,
                              @RequestParam(required = false) String phuongXa,
                              @RequestParam(required = false) String chiTietDiaChi,
                              RedirectAttributes redirectAttributes) {
        if (!isCurrentUserAdminOrEmployee()) {
            redirectAttributes.addFlashAttribute("message", "Bạn không có quyền thêm khách hàng!");
            redirectAttributes.addFlashAttribute("messageType", "danger");
            return "redirect:/acvstore/customers";
        }

        try {

            // Trim và validate họ tên
            String hoTen = customer.getHoTen().trim();
            customer.setHoTen(hoTen); // Cập nhật lại họ tên đã trim
            if (!hoTen.matches("^[A-Za-zÀ-ỹ\\s]+$")) {
                throw new IllegalArgumentException("Họ tên chỉ được chứa chữ cái và khoảng trắng");
            }
            if (hoTen.contains("  ")) { // Kiểm tra nhiều hơn 1 khoảng trắng
                throw new IllegalArgumentException("Họ tên chỉ được chứa 1 khoảng trắng giữa các từ");
            }
            if (!customer.getTenDangNhap().matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]+$")) {
                throw new IllegalArgumentException("Tên đăng nhập phải chứa cả chữ cái và số");
            }
            if (!customer.getEmail().matches("^[a-zA-Z0-9._%+-]+@gmail\\.com$")) {
                throw new IllegalArgumentException("Email phải có định dạng @gmail.com");
            }
            if (!customer.getSoDienThoai().matches("^[0-9]{10}$")) {
                throw new IllegalArgumentException("Số điện thoại phải là 10 chữ số");
            }
            if (customer.getMatKhau().length() < 6) {
                throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự");
            }
            if (tinhThanhPho == null || quanHuyen == null || phuongXa == null) {
                throw new IllegalArgumentException("Vui lòng chọn đầy đủ tỉnh/thành phố, quận/huyện, phường/xã");
            }

            // Đặt các trường địa chỉ từ form vào customer để DiaChiKhachHangService xử lý
            customer.setTinhThanhPho(tinhThanhPho);
            customer.setQuanHuyen(quanHuyen);
            customer.setPhuongXa(phuongXa);
            customer.setChiTietDiaChi(chiTietDiaChi);

            // Lưu khách hàng và địa chỉ
            diaChiKhachHangService.saveCustomerWithDefaultAddress(customer);

            redirectAttributes.addFlashAttribute("message", "Thêm khách hàng thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("message", "Thêm khách hàng thất bại: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Thêm khách hàng thất bại: Lỗi hệ thống");
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        return "redirect:/acvstore/customers";
    }

    @PostMapping("/edit")
    public String editCustomer(@ModelAttribute("customer") NguoiDung customer,
                               @RequestParam(required = false) String tinhThanhPho,
                               @RequestParam(required = false) String quanHuyen,
                               @RequestParam(required = false) String phuongXa,
                               @RequestParam(required = false) String chiTietDiaChi,
                               RedirectAttributes redirectAttributes) {
        if (!isCurrentUserAdminOrEmployee()) {
            redirectAttributes.addFlashAttribute("message", "Bạn không có quyền sửa khách hàng!");
            redirectAttributes.addFlashAttribute("messageType", "danger");
            return "redirect:/acvstore/customers";
        }

        try {
            // Trim và validate họ tên
            String hoTen = customer.getHoTen().trim();
            customer.setHoTen(hoTen); // Cập nhật lại họ tên đã trim
            if (!hoTen.matches("^[A-Za-zÀ-ỹ\\s]+$")) {
                throw new IllegalArgumentException("Họ tên chỉ được chứa chữ cái và khoảng trắng");
            }
            if (hoTen.contains("  ")) { // Kiểm tra nhiều hơn 1 khoảng trắng
                throw new IllegalArgumentException("Họ tên chỉ được chứa 1 khoảng trắng giữa các từ");
            }
            if (!customer.getTenDangNhap().matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]+$")) {
                throw new IllegalArgumentException("Tên đăng nhập phải chứa cả chữ cái và số");
            }
            if (!customer.getEmail().matches("^[a-zA-Z0-9._%+-]+@gmail\\.com$")) {
                throw new IllegalArgumentException("Email phải có định dạng @gmail.com");
            }
            if (!customer.getSoDienThoai().matches("^[0-9]{10}$")) {
                throw new IllegalArgumentException("Số điện thoại phải là 10 chữ số");
            }
            if (customer.getMatKhau().length() < 6) {
                throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự");
            }
            if (tinhThanhPho == null || quanHuyen == null || phuongXa == null) {
                throw new IllegalArgumentException("Vui lòng chọn đầy đủ tỉnh/thành phố, quận/huyện, phường/xã");
            }
            // Đặt các trường địa chỉ từ form vào customer để DiaChiKhachHangService xử lý
            customer.setTinhThanhPho(tinhThanhPho);
            customer.setQuanHuyen(quanHuyen);
            customer.setPhuongXa(phuongXa);
            customer.setChiTietDiaChi(chiTietDiaChi);

            // Cập nhật khách hàng và địa chỉ
            diaChiKhachHangService.updateCustomerAndAppendAddress(customer);

            redirectAttributes.addFlashAttribute("message", "Sửa khách hàng thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Sửa thất bại: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        return "redirect:/acvstore/customers";
    }






    @GetMapping("/dashboard")
    public String showDashboard(Model model, Authentication authentication) {
        addUserInfoToModel(model);
        model.addAttribute("message", "Chào mừng đến với dashboard!");
        model.addAttribute("messageType", "success");
        return "WebQuanLy/customer-dashboard";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "WebQuanLy/customer-login";
    }

    @GetMapping("/by-phone/{phone}")
    @ResponseBody
    public ResponseEntity<?> getCustomerByPhone(@PathVariable String phone) {
        NguoiDung customer = nguoiDungService.findBySoDienThoai(phone);
        if (customer == null || !"CUSTOMER".equalsIgnoreCase(customer.getVaiTro())) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok().body(new SimpleCustomerDTO(
                customer.getId(),
                customer.getHoTen(),
                customer.getEmail()
        ));
    }

    record SimpleCustomerDTO(UUID id, String hoTen, String email) {}

}
