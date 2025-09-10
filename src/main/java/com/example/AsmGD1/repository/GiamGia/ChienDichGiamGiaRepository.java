package com.example.AsmGD1.repository.GiamGia;

import com.example.AsmGD1.entity.ChienDichGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ChienDichGiamGiaRepository extends JpaRepository<ChienDichGiamGia, UUID>, JpaSpecificationExecutor<ChienDichGiamGia> {

    boolean existsByMa(String ma);
    boolean existsByMaAndIdNot(String ma, UUID id);
    boolean existsByTenIgnoreCase(String ten);

    @Query("""
           SELECT c
           FROM ChienDichGiamGia c
           JOIN ChiTietSanPham ct ON ct.chienDichGiamGia.id = c.id
           WHERE ct.sanPham.id = :sanPhamId
             AND c.ngayBatDau <= :now
             AND c.ngayKetThuc >= :now
           """)
    List<ChienDichGiamGia> findBySanPhamIdAndActive(@Param("sanPhamId") UUID sanPhamId,
                                                    @Param("now") LocalDateTime now);
    @Query("""
       SELECT DISTINCT c.id
       FROM ChienDichGiamGia c
       JOIN ChiTietSanPham ct ON ct.chienDichGiamGia.id = c.id
       WHERE c.ngayKetThuc <= :now
       """)
    List<UUID> findEndedCampaignIdsWithDetails(@Param("now") LocalDateTime now);
    List<ChienDichGiamGia> findByNgayKetThucBefore(LocalDateTime time);

}
