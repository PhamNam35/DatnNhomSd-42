package com.example.AsmGD1.config;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private NguoiDungService nguoiDungService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {
        HttpSession session = request.getSession();

        // Check if user needs to complete registration
        if (session.getAttribute("pendingUser") != null) {
            response.sendRedirect("/customers/oauth2/register");
            return;
        }

        // Handle OAuth2 user and convert to NguoiDung-based authentication
        if (authentication.getPrincipal() instanceof DefaultOAuth2User) {
            DefaultOAuth2User oauth2User = (DefaultOAuth2User) authentication.getPrincipal();
            String email = (String) oauth2User.getAttributes().get("email");
            NguoiDung nguoiDung = nguoiDungService.getUserByEmail(email);

            if (nguoiDung != null) {
                // Create new authentication with NguoiDung as principal
                Authentication newAuth = new UsernamePasswordAuthenticationToken(
                        nguoiDung,
                        nguoiDung.getPassword(),
                        nguoiDung.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(newAuth);
                session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

                // Role-based redirection
                String vaiTro = nguoiDung.getVaiTro();
                if ("ADMIN".equalsIgnoreCase(vaiTro)) {
                    byte[] descriptor = nguoiDung.getFaceDescriptor();
                    if (descriptor == null || descriptor.length == 0) {
                        response.sendRedirect("/acvstore/register-face");
                    } else {
                        response.sendRedirect("/acvstore/verify-face");
                    }
                } else if ("EMPLOYEE".equalsIgnoreCase(vaiTro)) {
                    response.sendRedirect("/acvstore/employee-dashboard");
                } else {
                    response.sendRedirect("/");
                }
            } else {
                response.sendRedirect("/customers/login?error=notfound");
            }
        }
    }
}