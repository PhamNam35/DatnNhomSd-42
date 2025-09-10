package com.example.AsmGD1.service.NguoiDung;

import com.example.AsmGD1.entity.NguoiDung;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final NguoiDungService nguoiDungService;
    private final HttpSession session;

    public CustomOAuth2UserService(NguoiDungService nguoiDungService, HttpSession session) {
        this.nguoiDungService = nguoiDungService;
        this.session = session;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        NguoiDung nguoiDung = processOAuth2User(oAuth2User, userRequest.getClientRegistration().getRegistrationId());

        // Kiểm tra nếu người dùng chưa hoàn tất đăng ký
        boolean isNewUser = nguoiDung.getTenDangNhap() == null || nguoiDung.getMatKhau() == null;
        if (isNewUser) {
            session.setAttribute("pendingUser", nguoiDung);
        } else {
            session.removeAttribute("pendingUser");
        }

        // Trả về DefaultOAuth2User với thông tin NguoiDung
        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("email", nguoiDung.getEmail());
        attributes.put("name", nguoiDung.getHoTen());
        attributes.put("id", nguoiDung.getId() != null ? nguoiDung.getId().toString() : null);

        return new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + nguoiDung.getVaiTro().toUpperCase())),
                attributes,
                "name"
        );
    }

    private NguoiDung processOAuth2User(OAuth2User oAuth2User, String provider) {
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        if (email == null) {
            throw new IllegalArgumentException("Email không thể null khi đăng nhập OAuth2");
        }

        // Đồng bộ hóa để tránh xung đột
        synchronized (email.intern()) {
            NguoiDung existingUser = nguoiDungService.getUserByEmail(email);
            if (existingUser != null) {
                return existingUser; // Trả về người dùng hiện có nếu tìm thấy
            }

            // Tạo đối tượng NguoiDung nhưng không lưu ngay
            NguoiDung newUser = new NguoiDung();
            newUser.setEmail(email);
            newUser.setHoTen(name != null ? name : "Unknown");
            newUser.setVaiTro("CUSTOMER");
            newUser.setThoiGianTao(LocalDateTime.now());
            newUser.setTrangThai(true);
            return newUser; // Trả về đối tượng mà không lưu vào DB
        }
    }
}