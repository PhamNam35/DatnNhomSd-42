package com.example.AsmGD1.config;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class FaceVerificationFilter extends OncePerRequestFilter {

    private final NguoiDungService nguoiDungService;

    public FaceVerificationFilter(NguoiDungService nguoiDungService) {
        this.nguoiDungService = nguoiDungService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Áp dụng cho tất cả URL /acvstore/**, trừ một số trang sau:
        if (path.startsWith("/acvstore")
                && !path.equals("/acvstore/login")
                && !path.equals("/acvstore/logout")
                && !path.equals("/acvstore/register-face")
                && !path.equals("/acvstore/verify-face")
                && !path.equals("/acvstore/verify-success")) {


            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String username = auth.getName();
                NguoiDung nguoiDung = nguoiDungService.findByTenDangNhap(username);

                boolean isAdmin = auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(role -> role.equals("ROLE_ADMIN"));

                if (isAdmin) {
                    boolean isOnRegisterOrVerifyPage = path.equals("/acvstore/register-face") || path.equals("/acvstore/verify-face");

                    // 1. Chưa đăng ký khuôn mặt → yêu cầu đăng ký
                    if (nguoiDung == null || nguoiDung.getFaceDescriptor() == null || nguoiDung.getFaceDescriptor().length == 0) {
                        if (!isOnRegisterOrVerifyPage) {
                            response.sendRedirect("/acvstore/register-face");
                            return;
                        }
                    }

                    // 2. Đã đăng ký nhưng chưa xác minh → yêu cầu xác minh
                    else {
                        Boolean verified = (Boolean) request.getSession().getAttribute("faceVerified");
                        if ((verified == null || !verified) && !isOnRegisterOrVerifyPage) {
                            response.sendRedirect("/acvstore/verify-face");
                            return;
                        }
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
