package com.example.AsmGD1.controller.NguoiDung;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.DiaChiKhachHangService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Controller
public class OAuth2RegistrationController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DiaChiKhachHangService diaChiKhachHangService;

    @Value("${ghn.api.token}")
    private String ghnToken;

    @Value("${ghn.api.url}")
    private String ghnApiUrl;

    private final HttpSession session;

    public OAuth2RegistrationController(HttpSession session) {
        this.session = session;
    }

    @GetMapping("/customers/oauth2/register")
    public String showRegistrationForm(Model model) {
        NguoiDung nguoiDung = (NguoiDung) session.getAttribute("pendingUser");
        if (nguoiDung == null) {
            return "redirect:/";
        }

        model.addAttribute("nguoiDung", nguoiDung);

        // Gọi API GHN để lấy danh sách tỉnh/thành phố
        String provinceUrl = ghnApiUrl + "/master-data/province";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghnToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            Object response = restTemplate.exchange(provinceUrl, HttpMethod.GET, entity, Object.class).getBody();
            model.addAttribute("provinces", response);
        } catch (Exception e) {
            model.addAttribute("error", "Không thể tải danh sách tỉnh/thành: " + e.getMessage());
        }

        return "WebQuanLy/oauth2-register";
    }

    @PostMapping("/customers/oauth2/register")
    public String completeRegistration(
            @Valid @ModelAttribute("nguoiDung") NguoiDung nguoiDung,
            BindingResult result,
            Model model,
            Authentication authentication,
            HttpServletRequest request) {

        NguoiDung pendingUser = (NguoiDung) session.getAttribute("pendingUser");
        if (pendingUser == null) {
            return "redirect:/";
        }

        // Gọi lại danh sách tỉnh/thành phố để hiển thị khi có lỗi
        String provinceUrl = ghnApiUrl + "/master-data/province";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghnToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            Object response = restTemplate.exchange(provinceUrl, HttpMethod.GET, entity, Object.class).getBody();
            model.addAttribute("provinces", response);
        } catch (Exception e) {
            model.addAttribute("error", "Không thể tải danh sách tỉnh/thành: " + e.getMessage());
            return "WebQuanLy/oauth2-register";
        }

        // Đồng bộ hóa để tránh xung đột
        synchronized (pendingUser.getEmail().intern()) {
            if (result.hasErrors()) {
                model.addAttribute("nguoiDung", pendingUser); // Giữ lại pendingUser
                return "WebQuanLy/oauth2-register";
            }

            // Cập nhật thông tin từ pendingUser
            pendingUser.setTenDangNhap(nguoiDung.getTenDangNhap());
            pendingUser.setMatKhau(nguoiDung.getMatKhau());
            pendingUser.setSoDienThoai(nguoiDung.getSoDienThoai());
            pendingUser.setTinhThanhPho(nguoiDung.getTinhThanhPho());
            pendingUser.setQuanHuyen(nguoiDung.getQuanHuyen());
            pendingUser.setPhuongXa(nguoiDung.getPhuongXa());
            pendingUser.setChiTietDiaChi(nguoiDung.getChiTietDiaChi());

            try {
                // Lưu user và địa chỉ mặc định
                diaChiKhachHangService.saveCustomerWithDefaultAddress(pendingUser);

                // Cập nhật authentication
                if (authentication != null && authentication.getPrincipal() instanceof OAuth2User) {
                    Map<String, Object> attributes = new HashMap<>(((OAuth2User) authentication.getPrincipal()).getAttributes());
                    attributes.put("id", pendingUser.getId().toString());
                    attributes.put("name", pendingUser.getHoTen());
                    attributes.put("email", pendingUser.getEmail());

                    OAuth2User updatedOAuth2User = new DefaultOAuth2User(
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + pendingUser.getVaiTro().toUpperCase())),
                            attributes,
                            "name"
                    );

                    Authentication newAuth = new OAuth2AuthenticationToken(
                            updatedOAuth2User,
                            updatedOAuth2User.getAuthorities(),
                            authentication.getName()
                    );

                    SecurityContextHolder.getContext().setAuthentication(newAuth);
                }

                // Xóa pendingUser và làm mới session
                session.removeAttribute("pendingUser");
                request.getSession().invalidate();
                request.getSession(true);

                String msg = URLEncoder.encode("Đăng ký thành công! Vui lòng đăng nhập.", StandardCharsets.UTF_8);
                return "redirect:/cart?success=" + msg;

            } catch (IllegalArgumentException e) {
                model.addAttribute("error", e.getMessage());
                model.addAttribute("nguoiDung", pendingUser);
                return "WebQuanLy/oauth2-register";
            }
        }
    }

    @GetMapping("/districts")
    public Object getDistricts(@RequestParam("provinceId") int provinceId) {
        String districtUrl = ghnApiUrl + "/master-data/district?province_id=" + provinceId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghnToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(districtUrl, HttpMethod.GET, entity, Object.class).getBody();
    }

    @GetMapping("/wards")
    public Object getWards(@RequestParam("districtId") int districtId) {
        String wardUrl = ghnApiUrl + "/master-data/ward?district_id=" + districtId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghnToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(wardUrl, HttpMethod.GET, entity, Object.class).getBody();
    }
}