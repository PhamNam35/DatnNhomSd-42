package com.example.AsmGD1.repository.GiamGia;

import com.example.AsmGD1.entity.PhieuGiamGiaCuaNguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KHPhieuGiamGiaCuaNguoiDungRepository extends JpaRepository<PhieuGiamGiaCuaNguoiDung, UUID> {
    @Query("SELECT p FROM PhieuGiamGiaCuaNguoiDung p WHERE p.nguoiDung.id = :nguoiDungId AND p.soLuotConLai > 0")
    List<PhieuGiamGiaCuaNguoiDung> findAvailableVouchersByNguoiDungId(UUID nguoiDungId);

    @Query("SELECT p FROM PhieuGiamGiaCuaNguoiDung p WHERE p.phieuGiamGia.ma = :ma AND p.nguoiDung.id = :nguoiDungId AND p.soLuotConLai > 0")
    Optional<PhieuGiamGiaCuaNguoiDung> findByMaAndNguoiDungId(String ma, UUID nguoiDungId);
}
