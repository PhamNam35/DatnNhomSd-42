package com.example.AsmGD1.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority; // Import này
import org.springframework.security.core.authority.SimpleGrantedAuthority; // Import này
import org.springframework.security.core.userdetails.UserDetails; // Import này

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection; // Import này
import java.util.Collections; // Import này
import java.util.UUID;

@Entity
@Table(name = "nguoi_dung")
@Data // Lombok @Data sẽ tự động tạo getters/setters, constructors
public class NguoiDung implements UserDetails { // Thêm "implements UserDetails"

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UNIQUEIDENTIFIER")
    private UUID id;

    @Column(name = "ten_dang_nhap", nullable = false, length = 50, unique = true)
    private String tenDangNhap;

    @Column(name = "mat_khau", nullable = false, length = 100)
    private String matKhau;

    @Column(name = "ho_ten", nullable = false, length = 100)
    private String hoTen;

    @Column(name = "email", nullable = false, length = 100, unique = true)
    private String email;

    @Column(name = "so_dien_thoai", nullable = false, length = 20, unique = true)
    private String soDienThoai;


    @Column(name = "vai_tro", length = 50)
    private String vaiTro; // Ví dụ: "CUSTOMER", "ADMIN", "EMPLOYEE"

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    @Column(name = "gioi_tinh")
    private Boolean gioiTinh;

    @Column(name = "tinh_thanh_pho", length = 100)
    private String tinhThanhPho;

    @Column(name = "quan_huyen", length = 100)
    private String quanHuyen;

    @Column(name = "phuong_xa", length = 100)
    private String phuongXa;

    @Column(name = "chi_tiet_dia_chi", columnDefinition = "NVARCHAR(MAX)")
    private String chiTietDiaChi;

    @Column(name = "thoi_gian_tao")
    private LocalDateTime thoiGianTao;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;

    @Lob
    @Column(name = "face_descriptor")
    private byte[] faceDescriptor;

    @Column(name = "face_registered")
    private Boolean faceRegistered = false;

    @Column(name = "face_verified")
    private Boolean faceVerified = false;

    @Column(name = "reset_otp")
    private String resetOtp;

    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;

    // --- Các phương thức của UserDetails (BẮT BUỘC) ---

    /**
     * Trả về danh sách quyền hạn (roles) của người dùng.
     * Mỗi quyền hạn thường bắt đầu bằng "ROLE_" theo quy ước của Spring Security.
     * Ví dụ: "CUSTOMER" sẽ trở thành "ROLE_CUSTOMER".
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Đảm bảo vaiTro không null để tránh lỗi NullPointerException.
        // Nếu vaiTro có thể null, bạn có thể trả về Collections.emptyList()
        // hoặc một quyền mặc định (ví dụ: "ROLE_UNKNOWN").
        if (this.vaiTro == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + vaiTro.toUpperCase()));
    }

    /**
     * Trả về mật khẩu của người dùng.
     * Vì bạn đã cấu hình NoOpPasswordEncoder, mật khẩu sẽ được lấy trực tiếp từ trường `matKhau`.
     */
    @Override
    public String getPassword() {
        return matKhau;
    }

    /**
     * Trả về tên đăng nhập (username) của người dùng.
     */
    @Override
    public String getUsername() {
        return tenDangNhap;
    }

    /**
     * Cho biết tài khoản có hết hạn hay không.
     * Trả về `true` nếu tài khoản hợp lệ (không hết hạn).
     */
    @Override
    public boolean isAccountNonExpired() {
        return true; // Giả sử tài khoản không bao giờ hết hạn. Bạn có thể thêm logic kiểm tra nếu cần.
    }

    /**
     * Cho biết tài khoản có bị khóa hay không.
     * Trả về `true` nếu tài khoản không bị khóa.
     */
    @Override
    public boolean isAccountNonLocked() {
        return trangThai != null ? trangThai : false; // Tài khoản không bị khóa nếu `trangThai` là `true`. Xử lý null.
    }

    /**
     * Cho biết thông tin đăng nhập (mật khẩu) có hết hạn hay không.
     * Trả về `true` nếu thông tin đăng nhập hợp lệ (không hết hạn).
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Giả sử mật khẩu không bao giờ hết hạn. Bạn có thể thêm logic kiểm tra nếu cần.
    }

    /**
     * Cho biết tài khoản có được kích hoạt hay không.
     * Trả về `true` nếu tài khoản được kích hoạt.
     */
    @Override
    public boolean isEnabled() {
        return trangThai != null ? trangThai : false; // Tài khoản được kích hoạt nếu `trangThai` là `true`. Xử lý null.
    }

    // Lombok @Data sẽ tự động tạo getter và setter cho các trường trên, bao gồm getId().
    // Điều này là cần thiết để các Controller có thể lấy được ID của người dùng đã đăng nhập.
}