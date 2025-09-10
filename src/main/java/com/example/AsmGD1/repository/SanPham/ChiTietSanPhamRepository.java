package com.example.AsmGD1.repository.SanPham;

import com.example.AsmGD1.entity.ChiTietSanPham;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChiTietSanPhamRepository extends JpaRepository<ChiTietSanPham, UUID> {


    long countBySoLuongTonKhoGreaterThanAndGiaGreaterThanEqual(int soLuongTonKho, BigDecimal gia);

    long countBySoLuongTonKhoGreaterThanAndGiaGreaterThanEqualAndSanPham_TenSanPhamContainingIgnoreCase(
            int soLuongTonKho, BigDecimal gia, String tenSanPham);


    Page<ChiTietSanPham> findBySoLuongTonKhoGreaterThan(int soLuongTonKho, Pageable pageable);

    // Phương thức tìm sản phẩm theo từ khóa và số lượng tồn kho
    Page<ChiTietSanPham> findBySoLuongTonKhoGreaterThanAndSanPham_TenSanPhamContainingIgnoreCase(
            int soLuongTonKho, String tenSanPham, Pageable pageable);

    List<ChiTietSanPham> findByGiaGreaterThanEqualAndSoLuongTonKhoGreaterThan(BigDecimal gia, int soLuongTonKho);

    Page<ChiTietSanPham> findBySoLuongTonKhoGreaterThanAndGiaGreaterThanEqual(int soLuongTonKho, BigDecimal gia, Pageable pageable);

    Page<ChiTietSanPham> findBySoLuongTonKhoGreaterThanAndGiaGreaterThanEqualAndSanPham_TenSanPhamContainingIgnoreCase(
            int soLuongTonKho, BigDecimal gia, String tenSanPham, Pageable pageable);

    @Query("SELECT c FROM ChiTietSanPham c WHERE c.trangThai = true AND (c.sanPham.tenSanPham LIKE %:keyword% OR c.mauSac.tenMau LIKE %:keyword% OR c.kichCo.ten LIKE %:keyword%)")
    List<ChiTietSanPham> findAllByTrangThaiAndKeyword(@Param("keyword") String keyword);

    @Modifying
    @Query("UPDATE ChiTietSanPham c SET c.soLuongTonKho = c.soLuongTonKho + :quantity WHERE c.id = :id")
    void updateStock(UUID id, int quantity);

    @Query("""
           SELECT ct FROM ChiTietSanPham ct
           JOIN FETCH ct.sanPham sp
           JOIN FETCH ct.kichCo kc
           JOIN FETCH ct.mauSac ms
           LEFT JOIN FETCH ct.chienDichGiamGia cdg
           WHERE sp.id = :idSanPham
           """)
    List<ChiTietSanPham> findBySanPhamId(@Param("idSanPham") UUID idSanPham);

    @Query("SELECT ct FROM ChiTietSanPham ct WHERE ct.chienDichGiamGia.id = :chienDichGiamGiaId")
    List<ChiTietSanPham> findByChienDichGiamGiaId(@Param("chienDichGiamGiaId") UUID chienDichGiamGiaId);

    @Query("SELECT CASE WHEN COUNT(ct) > 0 THEN true ELSE false END FROM ChiTietSanPham ct WHERE ct.id = :chiTietId AND ct.chienDichGiamGia IS NOT NULL")
    boolean isParticipatingInCampaign(@Param("chiTietId") UUID chiTietId);

    boolean existsByChienDichGiamGiaId(UUID chienDichGiamGiaId);

    @Modifying
    @Query("UPDATE ChiTietSanPham ct SET ct.chienDichGiamGia = null WHERE ct.chienDichGiamGia.id = :chienDichGiamGiaId")
    void removeChienDichGiamGiaById(@Param("chienDichGiamGiaId") UUID chienDichGiamGiaId);

    @Query("SELECT ct FROM ChiTietSanPham ct JOIN ct.sanPham sp WHERE ct.trangThai = true AND sp.trangThai = true")
    List<ChiTietSanPham> findAllByTrangThai();

    @Query("""
           SELECT ct FROM ChiTietSanPham ct
           WHERE ct.sanPham.id = :productId
             AND ct.mauSac.id = :mauSacId
             AND ct.kichCo.id = :kichCoId
           """)
    ChiTietSanPham findBySanPhamIdAndMauSacIdAndKichCoId(@Param("productId") UUID productId,
                                                         @Param("mauSacId") UUID mauSacId,
                                                         @Param("kichCoId") UUID kichCoId);

    @Query("SELECT c FROM ChiTietSanPham c WHERE c.id = :id")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ChiTietSanPham> findById(@Param("id") UUID id, LockModeType lockMode);

    @Query("SELECT c FROM ChiTietSanPham c WHERE c.id = :id AND c.chienDichGiamGia.id = :chienDichGiamGiaId")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ChiTietSanPham> findByIdAndChienDichGiamGiaId(@Param("id") UUID id,
                                                           @Param("chienDichGiamGiaId") UUID chienDichGiamGiaId,
                                                           LockModeType lockMode);

    Optional<ChiTietSanPham> findById(UUID id);

    @Query("""
           SELECT ct FROM ChiTietSanPham ct
           JOIN FETCH ct.sanPham sp
           JOIN FETCH ct.kichCo kc
           JOIN FETCH ct.mauSac ms
           JOIN FETCH ct.xuatXu xx
           JOIN FETCH ct.chatLieu cl
           JOIN FETCH ct.kieuDang kd
           JOIN FETCH ct.tayAo ta
           JOIN FETCH ct.coAo ca
           JOIN FETCH ct.thuongHieu th
           WHERE 1=1
             AND (:queryParams IS NULL OR (
                    sp.id = :productId
                AND (:colorId IS NULL OR ct.mauSac.id = :colorId)
                AND (:sizeId  IS NULL OR ct.kichCo.id = :sizeId)
                AND (:originId IS NULL OR ct.xuatXu.id = :originId)
                AND (:materialId IS NULL OR ct.chatLieu.id = :materialId)
                AND (:styleId IS NULL OR ct.kieuDang.id = :styleId)
                AND (:sleeveId IS NULL OR ct.tayAo.id = :sleeveId)
                AND (:collarId IS NULL OR ct.coAo.id = :collarId)
                AND (:brandId IS NULL OR ct.thuongHieu.id = :brandId)
                AND (:gender  IS NULL OR ct.gioiTinh = :gender)
                AND (:status  IS NULL OR ct.trangThai = :status)
             ))
           """)
    List<ChiTietSanPham> findByDynamicQuery(@Param("queryParams") String query,
                                            @Param("productId") UUID productId,
                                            @Param("colorId") UUID colorId,
                                            @Param("sizeId") UUID sizeId,
                                            @Param("originId") UUID originId,
                                            @Param("materialId") UUID materialId,
                                            @Param("styleId") UUID styleId,
                                            @Param("sleeveId") UUID sleeveId,
                                            @Param("collarId") UUID collarId,
                                            @Param("brandId") UUID brandId,
                                            @Param("gender") String gender,
                                            @Param("status") Boolean status);

    default List<ChiTietSanPham> findByDynamicQuery(String query, Map<String, Object> params) {
        return findByDynamicQuery(
                query,
                (UUID) params.get("productId"),
                (UUID) params.get("colorId"),
                (UUID) params.get("sizeId"),
                (UUID) params.get("originId"),
                (UUID) params.get("materialId"),
                (UUID) params.get("styleId"),
                (UUID) params.get("sleeveId"),
                (UUID) params.get("collarId"),
                (UUID) params.get("brandId"),
                (String) params.get("gender"),
                (Boolean) params.get("status")
        );
    }

    /**
     * CTSP còn "rảnh" theo 1 sản phẩm: chưa gán campaign hoặc campaign đã kết thúc (<= now)
     */
    @Query("""
           SELECT ct FROM ChiTietSanPham ct
           JOIN FETCH ct.sanPham sp
           JOIN FETCH ct.kichCo kc
           JOIN FETCH ct.mauSac ms
           LEFT JOIN ct.chienDichGiamGia cdg
           WHERE sp.id = :idSanPham
             AND (cdg IS NULL OR cdg.ngayKetThuc <= :now)
           """)
    List<ChiTietSanPham> findAvailableBySanPhamId(@Param("idSanPham") UUID idSanPham,
                                                  @Param("now") LocalDateTime now);

    /**
     * CTSP còn "rảnh" theo nhiều sản phẩm.
     * - excludeId: id campaign hiện đang edit (để không lọc mất chi tiết thuộc chính campaign đó)
     * - create: truyền excludeId = null
     */
    @Query("""
           SELECT ct FROM ChiTietSanPham ct
           JOIN FETCH ct.sanPham sp
           JOIN FETCH ct.kichCo kc
           JOIN FETCH ct.mauSac ms
           LEFT JOIN ct.chienDichGiamGia cdg
           WHERE sp.id IN :productIds
             AND (
                    cdg IS NULL
                 OR cdg.id = :excludeId
                 OR cdg.ngayKetThuc <= :now
                 )
           """)
    List<ChiTietSanPham> findAvailableByProductIds(@Param("productIds") List<UUID> productIds,
                                                   @Param("now") LocalDateTime now,
                                                   @Param("excludeId") UUID excludeId);
    @Modifying
    @Query("UPDATE ChiTietSanPham ct SET ct.chienDichGiamGia = NULL WHERE ct.chienDichGiamGia.id = :campaignId")
    void detachByCampaignId(@Param("campaignId") UUID campaignId);

    // Bulk detach CTSP khỏi campaign đã hết hạn
    @Modifying
    @Query("""
           UPDATE ChiTietSanPham ct
           SET ct.chienDichGiamGia = NULL
           WHERE ct.chienDichGiamGia IS NOT NULL
             AND ct.chienDichGiamGia.ngayKetThuc <= :now
           """)
    int detachEndedCampaigns(@Param("now") LocalDateTime now);

    boolean existsByXuatXuId(UUID xuatXuId);
    boolean existsByThuongHieuId(UUID thuongHieuId);
    boolean existsByTayAoId(UUID tayAoId);
    boolean existsByMauSacId(UUID mauSacId);
    boolean existsByKieuDangId(UUID kieuDangId);
    boolean existsByKichCoId(UUID kichCoId);
    boolean existsByCoAoId(UUID coAoId);
    boolean existsByChatLieuId(UUID chatLieuId);
}
