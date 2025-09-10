package com.example.AsmGD1.repository.HoaDon;

import com.example.AsmGD1.entity.LichSuHoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LichSuHoaDonRepository extends JpaRepository<LichSuHoaDon, UUID> {
    // Trong LichSuHoaDonRepository.java
    Optional<LichSuHoaDon> findFirstByHoaDonIdAndTrangThaiOrderByThoiGianDesc(UUID hoaDonId, String trangThai);
}