package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/customers")
public class QuenMKController {

    @Autowired
    private NguoiDungService nguoiDungService;

    // Regex cơ bản, đủ an toàn cho validate phía server
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @GetMapping("/auth/forgot-password")
    public String showForgotPasswordForm(Model model) {
        if (!model.containsAttribute("email")) {
            model.addAttribute("email", "");
        }
        return "WebKhachHang/forgot-password";
    }

    @PostMapping("/auth/forgot-password")
    public String processForgotPassword(@RequestParam String email, Model model) {
        String mail = email == null ? "" : email.trim().toLowerCase();
        model.addAttribute("email", mail);

        // 1) Rỗng
        if (mail.isEmpty()) {
            model.addAttribute("error", "Vui lòng nhập email.");
            return "WebKhachHang/forgot-password";
        }

        // 2) Sai định dạng
        if (!EMAIL_PATTERN.matcher(mail).matches()) {
            model.addAttribute("error", "Email không đúng định dạng (vd: ten@domain.com).");
            return "WebKhachHang/forgot-password";
        }

        // 3) Không tồn tại
        NguoiDung user = nguoiDungService.getUserByEmail(mail);
        if (user == null) {
            model.addAttribute("error", "Email này chưa được đăng ký trong hệ thống.");
            return "WebKhachHang/forgot-password";
        }

        // 4) Gửi OTP
        try {
            nguoiDungService.sendOtp(mail);
            model.addAttribute("otpExpiry", user.getOtpExpiry());
            return "WebKhachHang/verify-otp";
        } catch (Exception e) {
            model.addAttribute("error", "Gửi OTP thất bại: " + e.getMessage());
            return "WebKhachHang/forgot-password";
        }
    }

    @PostMapping("/auth/verify-otp")
    public String verifyOtp(@RequestParam String email, @RequestParam String otp, Model model) {
        String mail = email == null ? "" : email.trim().toLowerCase();
        String result = nguoiDungService.verifyOtp(mail, otp);
        NguoiDung user = nguoiDungService.getUserByEmail(mail);

        model.addAttribute("email", mail);
        if (user != null && user.getOtpExpiry() != null) {
            model.addAttribute("otpExpiry", user.getOtpExpiry());
        }

        if ("expired".equals(result)) {
            model.addAttribute("error", "expired");
            return "WebKhachHang/verify-otp";
        }
        if ("invalid".equals(result)) {
            model.addAttribute("error", "invalid");
            return "WebKhachHang/verify-otp";
        }
        if ("valid".equals(result)) {
            return "WebKhachHang/reset-password";
        }

        model.addAttribute("error", "not_found");
        return "WebKhachHang/verify-otp";
    }

    @PostMapping("/auth/reset-password")
    public String processResetPassword(@RequestParam String email,
                                       @RequestParam String newPassword,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        String mail = email == null ? "" : email.trim().toLowerCase();

        try {
            nguoiDungService.resetPassword(mail, newPassword);
            redirectAttributes.addFlashAttribute("message", "Đặt lại mật khẩu thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
            return "redirect:/customers/login";
        } catch (Exception e) {
            model.addAttribute("email", mail);
            model.addAttribute("error", "Đặt lại mật khẩu thất bại: " + e.getMessage());
            return "WebKhachHang/reset-password";
        }
    }

    @PostMapping("/auth/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> req) {
        String email = req.get("email");
        String mail = email == null ? "" : email.trim().toLowerCase();

        if (mail.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Email không được để trống"));
        }
        if (!EMAIL_PATTERN.matcher(mail).matches()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Email không đúng định dạng"));
        }
        if (nguoiDungService.getUserByEmail(mail) == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Email không tồn tại"));
        }

        try {
            nguoiDungService.sendOtp(mail);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // === API check email tồn tại & hợp lệ để client kiểm tra realtime ===
    @GetMapping("/auth/check-email")
    @ResponseBody
    public Map<String, Object> checkEmail(@RequestParam String email) {
        String mail = email == null ? "" : email.trim().toLowerCase();
        boolean validFormat = EMAIL_PATTERN.matcher(mail).matches();
        boolean exists = validFormat && (nguoiDungService.getUserByEmail(mail) != null);
        return Map.of("validFormat", validFormat, "exists", exists);
    }
}
