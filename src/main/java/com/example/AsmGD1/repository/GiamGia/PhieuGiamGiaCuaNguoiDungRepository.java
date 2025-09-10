package com.example.AsmGD1.repository.GiamGia;

import com.example.AsmGD1.entity.PhieuGiamGiaCuaNguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhieuGiamGiaCuaNguoiDungRepository extends JpaRepository<PhieuGiamGiaCuaNguoiDung, UUID> {
    List<PhieuGiamGiaCuaNguoiDung> findByPhieuGiamGia_Id(UUID phieuId);
    List<PhieuGiamGiaCuaNguoiDung> findByNguoiDungId(UUID nguoiDungId);
    Optional<PhieuGiamGiaCuaNguoiDung> findByPhieuGiamGia_IdAndNguoiDung_Id(UUID phieuGiamGiaId, UUID nguoiDungId);
}