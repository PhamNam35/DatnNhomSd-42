package com.example.AsmGD1.service.NguoiDung;

import com.example.AsmGD1.entity.DiaChiNguoiDung;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.NguoiDung.ProfileRepository;
import com.example.AsmGD1.repository.NguoiDung.DiaChiNguoiDungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ProfileService {

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private DiaChiNguoiDungRepository diaChiNguoiDungRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^0[1-9][0-9]{8}$");

    public NguoiDung getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Không có người dùng nào đang đăng nhập");
        }

        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            String email = oAuth2User.getAttribute("email");
            if (email == null) {
                throw new RuntimeException("Không tìm thấy email trong thông tin OAuth2");
            }
            return profileRepository.findByEmailAndTrangThai(email, true)
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại hoặc đã bị xóa"));
        } else {
            String username = authentication.getName();
            return profileRepository.findByTenDangNhapAndTrangThai(username, true)
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại hoặc đã bị xóa"));
        }
    }

    public NguoiDung updateUser(NguoiDung userData) {
        UUID id = getCurrentUser().getId();

        // Validate
        if (userData.getHoTen() == null || userData.getHoTen().trim().isEmpty()) {
            throw new RuntimeException("Họ tên không được để trống");
        }
        if (userData.getEmail() == null || userData.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email không được để trống");
        }
        if (userData.getSoDienThoai() == null || userData.getSoDienThoai().trim().isEmpty()) {
            throw new RuntimeException("Số điện thoại không được để trống");
        }
        if (!EMAIL_PATTERN.matcher(userData.getEmail()).matches()) {
            throw new RuntimeException("Email không hợp lệ");
        }
        if (!PHONE_PATTERN.matcher(userData.getSoDienThoai()).matches()) {
            throw new RuntimeException("Số điện thoại không hợp lệ");
        }
        if (!profileRepository.isEmailAndPhoneUnique(id, userData.getEmail(), userData.getSoDienThoai())) {
            throw new RuntimeException("Email hoặc số điện thoại đã được sử dụng");
        }

        // Update
        NguoiDung existingUser = profileRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại hoặc đã bị xóa"));

        existingUser.setHoTen(userData.getHoTen());
        existingUser.setEmail(userData.getEmail());
        existingUser.setSoDienThoai(userData.getSoDienThoai());
        existingUser.setNgaySinh(userData.getNgaySinh());
        existingUser.setGioiTinh(userData.getGioiTinh());
        existingUser.setTinhThanhPho(userData.getTinhThanhPho());
        existingUser.setQuanHuyen(userData.getQuanHuyen());
        existingUser.setPhuongXa(userData.getPhuongXa());
        existingUser.setChiTietDiaChi(userData.getChiTietDiaChi());
        existingUser.setVaiTro(userData.getVaiTro());
        if (userData.getMatKhau() != null && !userData.getMatKhau().isEmpty()) {
            existingUser.setMatKhau(passwordEncoder.encode(userData.getMatKhau()));
        }

        return profileRepository.save(existingUser);
    }

    public void deleteUser() {
        UUID id = getCurrentUser().getId();
        NguoiDung user = profileRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại hoặc đã bị xóa"));
        user.setTrangThai(false);
        profileRepository.save(user);
    }

    // -------------------- Địa chỉ --------------------

    @Transactional
    public DiaChiNguoiDung addAddress(DiaChiNguoiDung address) {
        NguoiDung user = getCurrentUser();
        validateAddress(address);

        address.setNguoiDung(user);
        address.setThoiGianTao(LocalDateTime.now());
        if (address.getMacDinh() == null) {
            address.setMacDinh(false);
        }

        if (Boolean.TRUE.equals(address.getMacDinh())) {
            // Unset macDinh for all other addresses
            diaChiNguoiDungRepository.removeDefaultFlag(user.getId());
            // Update NguoiDung with new default address
            user.setTinhThanhPho(address.getTinhThanhPho());
            user.setQuanHuyen(address.getQuanHuyen());
            user.setPhuongXa(address.getPhuongXa());
            user.setChiTietDiaChi(address.getChiTietDiaChi());
            profileRepository.save(user);
        }

        return diaChiNguoiDungRepository.save(address);
    }

    public List<DiaChiNguoiDung> getUserAddresses() {
        NguoiDung user = getCurrentUser();
        if (user == null || !user.getTrangThai()) {
            return new ArrayList<>();
        }
        List<DiaChiNguoiDung> addresses = diaChiNguoiDungRepository.findByNguoiDung_Id(user.getId());
        // Đảm bảo chỉ một địa chỉ là mặc định
        boolean hasDefault = false;
        for (DiaChiNguoiDung addr : addresses) {
            if (addr.getMacDinh() && hasDefault) {
                addr.setMacDinh(false);
                diaChiNguoiDungRepository.save(addr);
            } else if (addr.getMacDinh()) {
                hasDefault = true;
            }
        }
        return addresses;
    }

    public DiaChiNguoiDung getAddressById(UUID addressId) {
        UUID userId = getCurrentUser().getId();
        return diaChiNguoiDungRepository.findById(addressId)
                .filter(addr -> addr.getNguoiDung().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Địa chỉ không tồn tại hoặc không thuộc về bạn"));
    }

    @Transactional
    public DiaChiNguoiDung updateAddress(UUID addressId, DiaChiNguoiDung addressData) {
        DiaChiNguoiDung address = getAddressById(addressId);
        validateAddress(addressData);

        address.setTinhThanhPho(addressData.getTinhThanhPho());
        address.setQuanHuyen(addressData.getQuanHuyen());
        address.setPhuongXa(addressData.getPhuongXa());
        address.setChiTietDiaChi(addressData.getChiTietDiaChi());

        if (Boolean.TRUE.equals(addressData.getMacDinh())) {
            NguoiDung user = address.getNguoiDung();
            // Unset macDinh for all other addresses
            diaChiNguoiDungRepository.removeDefaultFlag(user.getId());
            // Update NguoiDung with new default address
            user.setTinhThanhPho(address.getTinhThanhPho());
            user.setQuanHuyen(address.getQuanHuyen());
            user.setPhuongXa(address.getPhuongXa());
            user.setChiTietDiaChi(address.getChiTietDiaChi());
            profileRepository.save(user);
            address.setMacDinh(true);
        } else {
            address.setMacDinh(false);
        }

        return diaChiNguoiDungRepository.save(address);
    }

    @Transactional
    public DiaChiNguoiDung setDefaultAddress(UUID addressId) {
        DiaChiNguoiDung address = getAddressById(addressId);
        NguoiDung user = address.getNguoiDung();

        // Unset macDinh for all other addresses
        diaChiNguoiDungRepository.removeDefaultFlag(user.getId());
        // Update NguoiDung with new default address
        user.setTinhThanhPho(address.getTinhThanhPho());
        user.setQuanHuyen(address.getQuanHuyen());
        user.setPhuongXa(address.getPhuongXa());
        user.setChiTietDiaChi(address.getChiTietDiaChi());
        profileRepository.save(user);

        address.setMacDinh(true);
        return diaChiNguoiDungRepository.save(address);
    }

    @Transactional
    public void deleteAddress(UUID addressId) {
        DiaChiNguoiDung address = getAddressById(addressId);
        diaChiNguoiDungRepository.delete(address);
    }

    private void validateAddress(DiaChiNguoiDung address) {
        if (address.getTinhThanhPho() == null || address.getTinhThanhPho().trim().isEmpty() ||
                address.getQuanHuyen() == null || address.getQuanHuyen().trim().isEmpty() ||
                address.getPhuongXa() == null || address.getPhuongXa().trim().isEmpty() ||
                address.getChiTietDiaChi() == null || address.getChiTietDiaChi().trim().isEmpty()) {
            throw new RuntimeException("Tất cả các trường địa chỉ là bắt buộc");
        }
    }

    // -------------------- OTP / Mật khẩu --------------------

    public void sendOtp(String email) {
        NguoiDung user = profileRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));

        String otp = generateOtp();
        user.setResetOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(3));
        profileRepository.save(user);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Mã OTP đặt lại mật khẩu");
        message.setText("Mã OTP của bạn là: " + otp);
        mailSender.send(message);
    }

    public String verifyOtp(String email, String otp) {
        Optional<NguoiDung> userOptional = profileRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            return "not_found";
        }

        NguoiDung user = userOptional.get();
        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            return "expired";
        }
        return otp.equals(user.getResetOtp()) ? "valid" : "invalid";
    }

    public void resetPassword(String email, String newPassword) {
        NguoiDung user = profileRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));

        if (newPassword == null || newPassword.trim().isEmpty() || newPassword.length() < 6) {
            throw new RuntimeException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }

        user.setMatKhau(passwordEncoder.encode(newPassword));
        user.setResetOtp(null);
        user.setOtpExpiry(null);
        profileRepository.save(user);
    }

    private String generateOtp() {
        return String.valueOf(new Random().nextInt(900000) + 100000);
    }
}