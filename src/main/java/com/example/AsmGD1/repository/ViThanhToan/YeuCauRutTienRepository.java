package com.example.AsmGD1.repository.ViThanhToan;

import com.example.AsmGD1.entity.YeuCauRutTien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface YeuCauRutTienRepository extends JpaRepository<YeuCauRutTien, UUID> {
    Optional<YeuCauRutTien> findByMaGiaoDich(String maGiaoDich);

    @Query("SELECT COALESCE(SUM(y.soTien), 0) FROM YeuCauRutTien y WHERE y.viThanhToan.id = :viId AND y.trangThai = 'Đang chờ'")
    BigDecimal tongTienDangCho(@Param("viId") UUID viId);

}
