package com.example.AsmGD1.repository.GiamGia;

import com.example.AsmGD1.entity.PhieuGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhieuGiamGiaRepository extends JpaRepository<PhieuGiamGia, UUID>, JpaSpecificationExecutor<PhieuGiamGia> {
    boolean existsByMaIgnoreCase(String ma);

    @Query(value = "DELETE FROM phieu_giam_gia_phuong_thuc_thanh_toan WHERE id_phieu_giam_gia = :phieuGiamGiaId", nativeQuery = true)
    @Modifying
    void deletePhuongThucThanhToanByPhieuGiamGiaId(UUID phieuGiamGiaId);

    @Query(value = "SELECT * FROM phieu_giam_gia_phuong_thuc_thanh_toan WHERE id_phieu_giam_gia = :phieuGiamGiaId", nativeQuery = true)
    List<Object[]> findJoinTableRecords(UUID phieuGiamGiaId);
}