package com.example.AsmGD1.repository.SanPham;

import com.example.AsmGD1.entity.SanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SanPhamRepository extends JpaRepository<SanPham, UUID> {

    @Query("SELECT sp FROM SanPham sp WHERE LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :searchName, '%')) AND (:trangThai IS NULL OR sp.trangThai = :trangThai)")
    Page<SanPham> findByTenSanPhamContainingIgnoreCaseAndTrangThai(@Param("searchName") String searchName, @Param("trangThai") Boolean trangThai, Pageable pageable);

    List<SanPham> findByTenSanPhamContainingIgnoreCase(String name);

    Page<SanPham> findByTenSanPhamContainingIgnoreCase(String name, Pageable pageable);

    @Query("SELECT sp FROM SanPham sp WHERE sp.trangThai = :trangThai")
    Page<SanPham> findByTrangThai(@Param("trangThai") Boolean trangThai, Pageable pageable);

    // Trong SanPhamRepository (thêm import nếu cần: import java.util.UUID;)
    @Query("SELECT DISTINCT sp FROM SanPham sp " +
            "WHERE (:searchName IS NULL OR LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :searchName, '%')) OR LOWER(sp.maSanPham) LIKE LOWER(CONCAT('%', :searchName, '%'))) " +
            "AND (:trangThai IS NULL OR sp.trangThai = :trangThai) " +
            "AND (:danhMucId IS NULL OR sp.danhMuc.id = :danhMucId) " +
            "AND (:thuongHieuId IS NULL OR EXISTS (SELECT 1 FROM ChiTietSanPham ct WHERE ct.sanPham = sp AND ct.thuongHieu.id = :thuongHieuId)) " +
            "AND (:kieuDangId IS NULL OR EXISTS (SELECT 1 FROM ChiTietSanPham ct WHERE ct.sanPham = sp AND ct.kieuDang.id = :kieuDangId)) " +
            "AND (:chatLieuId IS NULL OR EXISTS (SELECT 1 FROM ChiTietSanPham ct WHERE ct.sanPham = sp AND ct.chatLieu.id = :chatLieuId)) " +
            "AND (:xuatXuId IS NULL OR EXISTS (SELECT 1 FROM ChiTietSanPham ct WHERE ct.sanPham = sp AND ct.xuatXu.id = :xuatXuId)) " +
            "AND (:tayAoId IS NULL OR EXISTS (SELECT 1 FROM ChiTietSanPham ct WHERE ct.sanPham = sp AND ct.tayAo.id = :tayAoId)) " +
            "AND (:coAoId IS NULL OR EXISTS (SELECT 1 FROM ChiTietSanPham ct WHERE ct.sanPham = sp AND ct.coAo.id = :coAoId))")
    Page<SanPham> findByAdvancedFilters(
            @Param("searchName") String searchName,
            @Param("trangThai") Boolean trangThai,
            @Param("danhMucId") UUID danhMucId,
            @Param("thuongHieuId") UUID thuongHieuId,
            @Param("kieuDangId") UUID kieuDangId,
            @Param("chatLieuId") UUID chatLieuId,
            @Param("xuatXuId") UUID xuatXuId,
            @Param("tayAoId") UUID tayAoId,
            @Param("coAoId") UUID coAoId,
            Pageable pageable);

    Page<SanPham> findByTenSanPhamContainingIgnoreCaseOrMaSanPhamContainingIgnoreCase(String ten, String ma, Pageable pageable);

    @Query("SELECT sp FROM SanPham sp WHERE sp.trangThai = true")
    List<SanPham> findAllByTrangThai();

    boolean existsByDanhMucId(UUID danhMucId);

    @Query("""
    SELECT sp FROM SanPham sp
    WHERE sp.id <> :idSanPham
      AND sp.trangThai = true
      AND (
          sp.danhMuc.id = :idDanhMuc
          OR LOWER(sp.tenSanPham) LIKE %:tenSanPham%
      )
    ORDER BY sp.thoiGianTao DESC
    """)
    List<SanPham> findSanPhamLienQuan(
            @Param("idSanPham") UUID idSanPham,
            @Param("idDanhMuc") UUID idDanhMuc,
            @Param("tenSanPham") String tenSanPham,
            Pageable pageable
    );

    boolean existsByMaSanPham(String maSanPham);
    boolean existsByTenSanPham(String tenSanPham);

    // ===========================
    // MỚI: Chỉ lấy sản phẩm có ÍT NHẤT 1 CTSP "rảnh"
    // (rảnh = không có chiến dịch, hoặc chiến dịch đã kết thúc: ngayKetThuc <= :now)
    // Lưu ý so sánh theo ID để tránh IDE báo đỏ thuộc tính.
    // ===========================
    @Query("""
        SELECT sp FROM SanPham sp
        WHERE sp.trangThai = true
          AND EXISTS (
             SELECT 1 FROM ChiTietSanPham ct
             LEFT JOIN ct.chienDichGiamGia cdg
             WHERE ct.sanPham.id = sp.id
               AND (cdg IS NULL OR cdg.ngayKetThuc <= :now)
          )
        """)
    Page<SanPham> findAvailableProducts(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("""
        SELECT sp FROM SanPham sp
        WHERE sp.trangThai = true
          AND (
               LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(sp.maSanPham)  LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
          AND EXISTS (
             SELECT 1 FROM ChiTietSanPham ct
             LEFT JOIN ct.chienDichGiamGia cdg
             WHERE ct.sanPham.id = sp.id
               AND (cdg IS NULL OR cdg.ngayKetThuc <= :now)
          )
        """)
    Page<SanPham> searchAvailableByTenOrMa(@Param("keyword") String keyword,
                                           @Param("now") LocalDateTime now,
                                           Pageable pageable);
}
