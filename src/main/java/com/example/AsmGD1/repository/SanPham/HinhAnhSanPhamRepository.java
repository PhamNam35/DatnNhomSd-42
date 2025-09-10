package com.example.AsmGD1.repository.SanPham;

import com.example.AsmGD1.entity.HinhAnhSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HinhAnhSanPhamRepository extends JpaRepository<HinhAnhSanPham, UUID> {
    @Query("""
       SELECT ha.urlHinhAnh 
       FROM HinhAnhSanPham ha 
       WHERE ha.chiTietSanPham.id = :idChiTietSanPham
       ORDER BY RAND()
       LIMIT 1
       """)
    Optional<String> findFirstImageByChiTietSanPham(@Param("idChiTietSanPham") UUID idChiTietSanPham);

    @Query("SELECT h FROM HinhAnhSanPham h WHERE h.chiTietSanPham.id = :chiTietSanPhamId ORDER BY h.thuTu")
    List<HinhAnhSanPham> findByChiTietSanPhamIdOrderByThuTu(UUID chiTietSanPhamId);
}