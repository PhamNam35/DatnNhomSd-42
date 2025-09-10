package com.example.AsmGD1.repository.WebKhachHang;

import com.example.AsmGD1.dto.KhachHang.SanPhamProjection;
import com.example.AsmGD1.entity.ChiTietSanPham;
import com.example.AsmGD1.entity.DanhMuc;
import com.example.AsmGD1.entity.SanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

@Repository
public interface KhachHangSanPhamRepository extends JpaRepository<SanPham, UUID> {
    @Query("SELECT MAX(ct.gia) FROM ChiTietSanPham ct WHERE ct.sanPham.id = :sanPhamId AND ct.trangThai = true")
    BigDecimal findMaxPriceBySanPhamId(@Param("sanPhamId") UUID sanPhamId);

    // Truy vấn sản phẩm đang hoạt động và có ít nhất một ChiTietSanPham hoạt động
    @Query("SELECT p FROM SanPham p JOIN p.danhMuc d " +
            "WHERE p.trangThai = true " +
            "AND EXISTS (SELECT c FROM ChiTietSanPham c WHERE c.sanPham.id = p.id AND c.trangThai = true)")
    List<SanPham> findActiveProducts();

    // Truy vấn sản phẩm mới (sắp xếp theo thoi_gian_tao giảm dần, lấy tối đa 10 sản phẩm)
    @Query("SELECT p FROM SanPham p JOIN p.danhMuc d " +
            "WHERE p.trangThai = true " +
            "AND EXISTS (SELECT c FROM ChiTietSanPham c WHERE c.sanPham.id = p.id AND c.trangThai = true) " +
            "ORDER BY p.thoiGianTao DESC")
    List<SanPham> findNewProducts();

    // Truy vấn sản phẩm bán chạy (dựa trên tổng số lượng bán từ chi_tiet_don_hang)
    @Query("SELECT p, SUM(ctdh.soLuong) as totalSold " +
            "FROM SanPham p " +
            "JOIN ChiTietSanPham ctsp ON p.id = ctsp.sanPham.id " +
            "JOIN ChiTietDonHang ctdh ON ctsp.id = ctdh.chiTietSanPham.id " +
            "WHERE p.trangThai = true AND ctsp.trangThai = true " +
            "GROUP BY p " +
            "ORDER BY totalSold DESC")
    List<Object[]> findBestSellingProducts();

    @Query("""
SELECT d FROM DanhMuc d
WHERE EXISTS (
  SELECT 1 FROM SanPham p
  JOIN p.chiTietSanPhams ct
  WHERE p.danhMuc.id = d.id
    AND p.trangThai = true
    AND ct.trangThai = true
)
ORDER BY d.tenDanhMuc ASC
""")
    List<DanhMuc> findCategoriesHavingActiveProducts();

    @Query("""
SELECT p FROM SanPham p
JOIN p.chiTietSanPhams ct
WHERE p.danhMuc.id = :categoryId
  AND p.trangThai = true
  AND ct.trangThai = true
GROUP BY p
ORDER BY p.thoiGianTao DESC
""")
    List<SanPham> findActiveProductsByCategory(UUID categoryId);

    // Truy vấn chi tiết sản phẩm
    @Query("SELECT c FROM ChiTietSanPham c " +
            "JOIN c.sanPham p " +
            "JOIN p.danhMuc d " +
            "JOIN c.kichCo k " +
            "JOIN c.mauSac m " +
            "JOIN c.chatLieu cl " +
            "JOIN c.xuatXu x " +
            "JOIN c.tayAo ta " +
            "JOIN c.coAo ca " +
            "JOIN c.kieuDang kd " +
            "JOIN c.thuongHieu th " +
            "WHERE c.trangThai = true")
    List<ChiTietSanPham> findActiveProductDetails();

    // Truy vấn chi tiết sản phẩm theo sanPhamId
    @Query("SELECT c FROM ChiTietSanPham c " +
            "JOIN c.sanPham p " +
            "JOIN p.danhMuc d " +
            "JOIN c.kichCo k " +
            "JOIN c.mauSac m " +
            "JOIN c.chatLieu cl " +
            "JOIN c.xuatXu x " +
            "JOIN c.tayAo ta " +
            "JOIN c.coAo ca " +
            "JOIN c.kieuDang kd " +
            "JOIN c.thuongHieu th " +
            "WHERE p.id = :sanPhamId AND c.trangThai = true")
    List<ChiTietSanPham> findActiveProductDetailsBySanPhamId(UUID sanPhamId);

    // Truy vấn hình ảnh sản phẩm
    @Query("SELECT h.urlHinhAnh FROM HinhAnhSanPham h WHERE h.chiTietSanPham.id = :chiTietSanPhamId ORDER BY h.thuTu")
    List<String> findProductImagesByChiTietSanPhamId(UUID chiTietSanPhamId);

    // Lấy tất cả URL ảnh của mọi biến thể thuộc 1 sản phẩm, sắp theo thứ tự ảnh
    @Query("""
SELECT h.urlHinhAnh
FROM HinhAnhSanPham h
JOIN h.chiTietSanPham ct
WHERE ct.sanPham.id = :sanPhamId
  AND ct.trangThai = true
ORDER BY h.thuTu ASC
""")
    List<String> findProductImagesBySanPhamId(@Param("sanPhamId") UUID sanPhamId);


    // Truy vấn chi tiết sản phẩm theo sanPhamId, sizeId, colorId
    @Query("SELECT c FROM ChiTietSanPham c WHERE c.sanPham.id = :sanPhamId AND c.kichCo.id = :sizeId AND c.mauSac.id = :colorId AND c.trangThai = true")
    ChiTietSanPham findBySanPhamIdAndSizeIdAndColorId(UUID sanPhamId, UUID sizeId, UUID colorId);

    // Truy vấn giá thấp nhất
    @Query("SELECT MIN(c.gia) FROM ChiTietSanPham c WHERE c.sanPham.id = :sanPhamId AND c.trangThai = true")
    BigDecimal findMinPriceBySanPhamId(UUID sanPhamId);

    @Query("SELECT p FROM SanPham p JOIN p.danhMuc d " +
            "WHERE p.trangThai = true " +
            "AND EXISTS (SELECT c FROM ChiTietSanPham c WHERE c.sanPham.id = p.id AND c.trangThai = true) " +
            "AND (LOWER(p.tenSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(p.moTa) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(d.tenDanhMuc) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<SanPham> searchProductsByKeyword(@Param("keyword") String keyword);

    @Query("SELECT p FROM SanPham p JOIN p.danhMuc d " +
            "WHERE p.trangThai = true " +
            "AND EXISTS (SELECT c FROM ChiTietSanPham c WHERE c.sanPham.id = p.id AND c.trangThai = true) " +
            "AND d.id = (SELECT d2.id FROM DanhMuc d2 JOIN d2.sanPhams sp GROUP BY d2.id ORDER BY COUNT(sp.id) DESC LIMIT 1)")
    List<SanPham> findPopularCategoryProducts();

    @Query("""
    SELECT p, 
           MIN(ctsp.gia), 
           MAX(ctsp.gia), 
           COALESCE(SUM(ctdh.soLuong), 0)
    FROM SanPham p 
    JOIN p.chiTietSanPhams ctsp 
    LEFT JOIN ChiTietDonHang ctdh ON ctdh.chiTietSanPham.id = ctsp.id
    WHERE p.trangThai = true AND ctsp.trangThai = true
    GROUP BY p
    ORDER BY p.thoiGianTao DESC
""")
    List<Object[]> findSanPhamMoiWithGiaAndSold();

    @Query("""
    SELECT p, 
           MIN(ctsp.gia), 
           MAX(ctsp.gia), 
           COALESCE(SUM(ctdh.soLuong), 0)
    FROM SanPham p 
    JOIN p.chiTietSanPhams ctsp 
    LEFT JOIN ChiTietDonHang ctdh ON ctdh.chiTietSanPham.id = ctsp.id
    WHERE p.trangThai = true AND ctsp.trangThai = true
    GROUP BY p
    ORDER BY SUM(ctdh.soLuong) DESC
""")
    List<Object[]> findSanPhamBanChayWithGiaAndSold();

    @Query("SELECT h.urlHinhAnh FROM HinhAnhSanPham h " +
            "WHERE h.chiTietSanPham.sanPham.id = :sanPhamId " +
            "AND h.chiTietSanPham.mauSac.id = :mauSacId " +
            "AND h.chiTietSanPham.trangThai = true " +
            "ORDER BY h.thuTu")
    List<String> findProductImagesBySanPhamIdAndMauSacId(@Param("sanPhamId") UUID sanPhamId,
                                                         @Param("mauSacId") UUID mauSacId);
}