package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.DiaChiKhachHangService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/customers/register")
public class RegisterController {

    @Autowired
    private DiaChiKhachHangService diaChiKhachHangService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ghn.api.token}")
    private String ghnToken;

    @Value("${ghn.api.url}")
    private String ghnApiUrl;

    @GetMapping
    public String showRegisterForm(Model model) {
        model.addAttribute("nguoiDung", new NguoiDung());

        // Gọi API GHN để lấy danh sách tỉnh/thành phố
        String provinceUrl = ghnApiUrl + "/master-data/province";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghnToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            Object response = restTemplate.exchange(provinceUrl, HttpMethod.GET, entity, Object.class).getBody();
            model.addAttribute("provinces", response);
        } catch (Exception e) {
            model.addAttribute("error", "Không thể tải danh sách tỉnh/thành phố: " + e.getMessage());
        }

        return "WebKhachHang/register";
    }

    @PostMapping("/saveUser")
    public String saveUser(@Valid @ModelAttribute("nguoiDung") NguoiDung nguoiDung,
                           BindingResult result,
                           Model model) {
        // Gọi lại danh sách tỉnh phòng khi form bị lỗi
        String provinceUrl = ghnApiUrl + "/master-data/province";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghnToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        Object response = restTemplate.exchange(provinceUrl, HttpMethod.GET, entity, Object.class).getBody();
        model.addAttribute("provinces", response);

        if (result.hasErrors()) {
            return "WebKhachHang/register";
        }

        try {
            // Quan trọng: gọi service mới để tạo user + địa chỉ mặc định + dọn địa chỉ khỏi bảng nguoi_dung
            diaChiKhachHangService.saveCustomerWithDefaultAddress(nguoiDung);

            String msg = URLEncoder.encode("Đăng ký thành công! Vui lòng đăng nhập.", StandardCharsets.UTF_8);
            return "redirect:/customers/login?success=" + msg;

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "WebKhachHang/register";
        }
    }

    @GetMapping("/districts")
    @ResponseBody
    public Object getDistricts(@RequestParam("provinceId") int provinceId) {
        String districtUrl = ghnApiUrl + "/master-data/district?province_id=" + provinceId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghnToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(districtUrl, HttpMethod.GET, entity, Object.class).getBody();
    }

    @GetMapping("/wards")
    @ResponseBody
    public Object getWards(@RequestParam("districtId") int districtId) {
        String wardUrl = ghnApiUrl + "/master-data/ward?district_id=" + districtId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghnToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(wardUrl, HttpMethod.GET, entity, Object.class).getBody();
    }
}
