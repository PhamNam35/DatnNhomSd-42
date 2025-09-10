package com.example.AsmGD1.repository.NguoiDung;

import com.example.AsmGD1.entity.DiaChiNguoiDung;
import com.example.AsmGD1.entity.NguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileRepository extends JpaRepository<NguoiDung, UUID> {

    @Query("SELECT u FROM NguoiDung u WHERE u.id = :id AND u.trangThai = true")
    Optional<NguoiDung> findActiveById(UUID id);

    @Query("SELECT u FROM NguoiDung u WHERE u.tenDangNhap = :tenDangNhap AND u.trangThai = true")
    Optional<NguoiDung> findByTenDangNhapAndTrangThai(String tenDangNhap, boolean trangThai);

    @Query("SELECT COUNT(u) = 0 FROM NguoiDung u WHERE (u.email = :email OR u.soDienThoai = :soDienThoai) AND u.id != :id AND u.trangThai = true")
    boolean isEmailAndPhoneUnique(UUID id, String email, String soDienThoai);

    Optional<NguoiDung> findByEmail(String email);

    @Modifying
    @Query("UPDATE DiaChiNguoiDung d SET d.macDinh = false WHERE d.nguoiDung.id = :nguoiDungId")
    void removeDefaultFlag(UUID nguoiDungId);

    Optional<NguoiDung> findByEmailAndTrangThai(String email, boolean trangThai);
}