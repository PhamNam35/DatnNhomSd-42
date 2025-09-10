package com.example.AsmGD1.service.NguoiDung;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.NguoiDung.KHNguoiDungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class KHNguoiDungService {

    @Autowired
    private KHNguoiDungRepository nguoiDungRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void save(NguoiDung nguoiDung) {
        // --- VALIDATE CHẶT CHẼ ---

        // 1. Tên đăng nhập
        String username = nguoiDung.getTenDangNhap();
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống.");
        }
        if (!username.matches("^[a-zA-Z0-9._-]{4,30}$")) {
            throw new IllegalArgumentException("Tên đăng nhập chỉ chứa chữ, số, dấu chấm, gạch dưới và dài 4–30 ký tự.");
        }
        if (nguoiDungRepository.findByTenDangNhap(username).isPresent()) {
            throw new IllegalArgumentException("Tên đăng nhập đã được sử dụng.");
        }

        // 2. Mật khẩu
        String password = nguoiDung.getMatKhau();
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống.");
        }
        if (password.length() < 6 || password.length() > 100) {
            throw new IllegalArgumentException("Mật khẩu phải từ 6 đến 100 ký tự.");
        }

        // 3. Họ tên
        String hoTen = nguoiDung.getHoTen();
        if (hoTen == null || hoTen.trim().isEmpty()) {
            throw new IllegalArgumentException("Họ tên không được để trống.");
        }
        if (hoTen.length() > 100) {
            throw new IllegalArgumentException("Họ tên tối đa 100 ký tự.");
        }

        // 4. Email
        String email = nguoiDung.getEmail();
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email không được để trống.");
        }
        if (!email.matches("^[\\w-.]+@[\\w-]+(\\.[\\w-]+)+$")) {
            throw new IllegalArgumentException("Email không hợp lệ.");
        }
        if (nguoiDungRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email đã được sử dụng.");
        }

        // 5. Số điện thoại
        String phone = nguoiDung.getSoDienThoai();
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Số điện thoại không được để trống.");
        }
        if (!phone.matches("^(0[3-9][0-9]{8})$")) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ. Ví dụ: 036xxxxxxx");
        }
        if (nguoiDungRepository.findBySoDienThoai(phone).isPresent()) {
            throw new IllegalArgumentException("Số điện thoại đã được sử dụng.");
        }

        // 6. Ngày sinh (có thể null, nhưng nếu có thì phải là quá khứ)
        if (nguoiDung.getNgaySinh() != null && nguoiDung.getNgaySinh().isAfter(LocalDateTime.now().toLocalDate())) {
            throw new IllegalArgumentException("Ngày sinh không hợp lệ.");
        }

        // 7. Tỉnh/Thành phố
        if (nguoiDung.getTinhThanhPho() == null || nguoiDung.getTinhThanhPho().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn tỉnh/thành phố.");
        }

        // 8. Quận/Huyện
        if (nguoiDung.getQuanHuyen() == null || nguoiDung.getQuanHuyen().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn quận/huyện.");
        }

        // 9. Phường/Xã
        if (nguoiDung.getPhuongXa() == null || nguoiDung.getPhuongXa().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn phường/xã.");
        }

        // 10. Chi tiết địa chỉ
        if (nguoiDung.getChiTietDiaChi() == null || nguoiDung.getChiTietDiaChi().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập chi tiết địa chỉ.");
        }

        // --- SET THÔNG TIN MẶC ĐỊNH ---
        nguoiDung.setVaiTro("CUSTOMER");
        nguoiDung.setTrangThai(true);
        nguoiDung.setThoiGianTao(LocalDateTime.now());

        // --- MÃ HÓA MẬT KHẨU ---
        nguoiDung.setMatKhau(passwordEncoder.encode(password));

        // --- LƯU VÀO DB ---
        nguoiDungRepository.save(nguoiDung);
    }

}
