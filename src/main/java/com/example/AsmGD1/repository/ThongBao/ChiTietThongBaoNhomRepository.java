package com.example.AsmGD1.repository.ThongBao;

import com.example.AsmGD1.entity.ChiTietThongBaoNhom;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChiTietThongBaoNhomRepository extends JpaRepository<ChiTietThongBaoNhom, UUID> {

    Page<ChiTietThongBaoNhom> findByNguoiDungId(UUID nguoiDungId, Pageable pageable);

    List<ChiTietThongBaoNhom> findByNguoiDungIdOrderByThongBaoNhom_ThoiGianTaoDesc(UUID nguoiDungId);

    List<ChiTietThongBaoNhom> findByNguoiDungIdAndDaXemFalseOrderByThongBaoNhom_ThoiGianTaoDesc(UUID nguoiDungId);

    long countByNguoiDungIdAndDaXemFalse(UUID nguoiDungId);

    long countByNguoiDungId(UUID nguoiDungId);

    List<ChiTietThongBaoNhom> findTop5ByNguoiDungIdAndDaXemFalseOrderByThongBaoNhom_ThoiGianTaoDesc(UUID nguoiDungId);

    @Modifying
    @Transactional
    void deleteByNguoiDungId(UUID nguoiDungId);

    // Updated methods to return Page instead of List
    Page<ChiTietThongBaoNhom> findByNguoiDungIdAndDaXemFalse(UUID nguoiDungId, Pageable pageable);

    Page<ChiTietThongBaoNhom> findByNguoiDungIdAndDaXemTrue(UUID nguoiDungId, Pageable pageable);

    // Add method for counting read notifications
    long countByNguoiDungIdAndDaXemTrue(UUID nguoiDungId);
}