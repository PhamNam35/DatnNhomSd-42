package com.example.AsmGD1.repository.NguoiDung;

import com.example.AsmGD1.entity.NguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KHNguoiDungRepository extends JpaRepository<NguoiDung, UUID> {
    Optional<NguoiDung> findByTenDangNhap(String tenDangNhap);
    Optional<NguoiDung> findByEmail(String email);
    Optional<NguoiDung> findBySoDienThoai(String soDienThoai);
    List<NguoiDung> findByVaiTro(String vaiTro);
}