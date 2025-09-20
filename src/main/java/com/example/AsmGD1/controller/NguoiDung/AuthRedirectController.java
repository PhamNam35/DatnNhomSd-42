package com.example.AsmGD1.controller.NguoiDung;

import com.example.AsmGD1.entity.NguoiDung;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthRedirectController {

    @GetMapping("/polyshoe/redirect")
    public String redirectAfterLogin(Authentication auth) {
        if (auth == null || auth.getAuthorities().isEmpty()) {
            return "redirect:/polyshoe/login?error=unauthorized";
        }

        NguoiDung nguoiDung = (NguoiDung) auth.getPrincipal();
        String vaiTro = nguoiDung.getVaiTro();

        if ("CUSTOMER".equalsIgnoreCase(vaiTro)) {
            SecurityContextHolder.clearContext();
            return "redirect:/polyshoe/login?error=accessDenied";
        } else if ("ADMIN".equalsIgnoreCase(vaiTro)) {
            byte[] descriptor = nguoiDung.getFaceDescriptor();
            if (descriptor == null || descriptor.length == 0) {
                return "redirect:/polyshoe/register-face";
            }
            return "redirect:/polyshoe/verify-face";
        } else if ("EMPLOYEE".equalsIgnoreCase(vaiTro)) {
            return "redirect:/polyshoe/ban-hang";
        } else {
            return "redirect:/polyshoe/login?error=unauthorizedRole";
        }
    }

    @GetMapping("/polyshoe/login")
    public String showLoginForm() {
        return "WebQuanLy/employee-login";
    }

    @GetMapping("/customers/login")
    public String showLoginFormCustomer() {
        return "WebQuanLy/customer-login";
    }
}