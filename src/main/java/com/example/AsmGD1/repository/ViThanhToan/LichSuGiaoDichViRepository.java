package com.example.AsmGD1.repository.ViThanhToan;

import com.example.AsmGD1.entity.LichSuGiaoDichVi;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LichSuGiaoDichViRepository extends JpaRepository<LichSuGiaoDichVi, UUID> {
    List<LichSuGiaoDichVi> findByIdViThanhToanOrderByCreatedAtDesc(UUID idViThanhToan);
    boolean existsByMaGiaoDich(String maGiaoDich);
}