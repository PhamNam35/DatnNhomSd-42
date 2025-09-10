package com.example.AsmGD1.repository.HoaDon;
import com.example.AsmGD1.entity.HoaDon;
import com.example.AsmGD1.entity.LichSuTraHang;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LichSuTraHangRepository extends JpaRepository<LichSuTraHang, UUID> {
    Page<LichSuTraHang> findByTrangThai(String trangThai, Pageable pageable);
    List<LichSuTraHang> findByHoaDonAndTrangThai(HoaDon hoaDon, String trangThai);
    List<LichSuTraHang> findByHoaDon(HoaDon hoaDon);
}