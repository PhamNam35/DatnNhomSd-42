package com.example.AsmGD1.service.NguoiDung; // Hoặc package tương ứng của bạn

import com.example.AsmGD1.entity.NguoiDung; // Đảm bảo import đúng lớp NguoiDung
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Tìm người dùng trong cơ sở dữ liệu
        NguoiDung user = nguoiDungRepository.findByTenDangNhap(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username));

        // TRẢ VỀ TRỰC TIẾP ĐỐI TƯỢNG NGUOIDUNG CỦA BẠN
        // Vì NguoiDung đã implement UserDetails, bạn có thể trả về nó ở đây.
        return user;
    }
}