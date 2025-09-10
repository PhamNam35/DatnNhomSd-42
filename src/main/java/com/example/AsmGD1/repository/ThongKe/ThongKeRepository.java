package com.example.AsmGD1.repository.ThongKe;

import com.example.AsmGD1.dto.ThongKe.SanPhamBanChayDTO;
import com.example.AsmGD1.dto.ThongKe.SanPhamTonKhoThapDTO;
import com.example.AsmGD1.entity.ThongKe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ThongKeRepository extends JpaRepository<ThongKe, UUID> {

    // ========================= DOANH THU TỔNG (HT ∪ ĐĐH ∪ TRMP) =========================
    @Query(value = """
WITH ht AS (  -- Hoàn thành
  SELECT DISTINCT hd.id_don_hang AS id_dh
  FROM hoa_don hd
  WHERE COALESCE(hd.ngay_thanh_toan, hd.ngay_tao) >= :start
    AND COALESCE(hd.ngay_thanh_toan, hd.ngay_tao) <  :end
    AND UPPER(LTRIM(RTRIM(hd.trang_thai)) COLLATE Vietnamese_100_CI_AI_SC_UTF8) LIKE N'HOÀN THÀNH%'
),
ddh AS (      -- Đã đổi hàng
  SELECT DISTINCT hd.id_don_hang AS id_dh
  FROM hoa_don hd
  WHERE COALESCE(hd.ngay_thanh_toan, hd.ngay_tao) >= :start
    AND COALESCE(hd.ngay_thanh_toan, hd.ngay_tao) <  :end
    AND UPPER(LTRIM(RTRIM(hd.trang_thai)) COLLATE Vietnamese_100_CI_AI_SC_UTF8) LIKE N'ĐÃ ĐỔI HÀNG%'
),
trmp AS (     -- Đã trả hàng một phần  ✅ THÊM
  SELECT DISTINCT hd.id_don_hang AS id_dh
  FROM hoa_don hd
  WHERE COALESCE(hd.ngay_thanh_toan, hd.ngay_tao) >= :start
    AND COALESCE(hd.ngay_thanh_toan, hd.ngay_tao) <  :end
    AND UPPER(LTRIM(RTRIM(hd.trang_thai)) COLLATE Vietnamese_100_CI_AI_SC_UTF8) LIKE N'ĐÃ TRẢ HÀNG MỘT PHẦN%'
),
eligible AS ( -- gộp
  SELECT id_dh FROM ht
  UNION SELECT id_dh FROM ddh
  UNION SELECT id_dh FROM trmp
),
lines_all AS ( -- tổng hàng ban đầu (đã gồm giảm theo chiến dịch nếu ct.thanh_tien có)
  SELECT ct.id_don_hang AS id_dh,
         SUM(COALESCE(ct.thanh_tien,
             CAST(ct.so_luong AS decimal(18,6)) * CAST(ct.gia AS decimal(18,6))
         )) AS merch_all
  FROM chi_tiet_don_hang ct
  GROUP BY ct.id_don_hang
),
lines_kept AS ( -- phần còn giữ (không trả)
  SELECT ct.id_don_hang AS id_dh,
         SUM(COALESCE(ct.thanh_tien,
             CAST(ct.so_luong AS decimal(18,6)) * CAST(ct.gia AS decimal(18,6))
         )) AS merch_kept
  FROM chi_tiet_don_hang ct
  WHERE (ct.trang_thai_hoan_tra = 0 OR ct.trang_thai_hoan_tra IS NULL)
  GROUP BY ct.id_don_hang
),
order_disc AS ( -- tổng ORDER voucher của đơn
  SELECT v.id_don_hang AS id_dh,
         SUM(CAST(v.gia_tri_giam AS decimal(18,6))) AS disc_total
  FROM don_hang_phieu_giam_gia v
  WHERE UPPER(v.loai_giam_gia) = N'ORDER'
  GROUP BY v.id_don_hang
),
per_order AS ( -- tổng hợp theo đơn
  SELECT e.id_dh,
         COALESCE(k.merch_kept, 0) AS merch_kept,
         COALESCE(a.merch_all, 0)  AS merch_all,
         COALESCE(d.disc_total, 0) AS disc_total
  FROM eligible e
  LEFT JOIN lines_kept k ON k.id_dh = e.id_dh
  LEFT JOIN lines_all  a ON a.id_dh = e.id_dh
  LEFT JOIN order_disc d ON d.id_dh = e.id_dh
)
SELECT COALESCE(SUM(
  CASE WHEN p.merch_kept <= 0 THEN 0
       ELSE GREATEST(
              p.merch_kept - (p.disc_total * (CAST(p.merch_kept AS decimal(18,6))
                                  / NULLIF(CAST(p.merch_all AS decimal(18,6)),0))), 0)
  END
), 0)
FROM per_order p
""", nativeQuery = true)
    BigDecimal tinhDoanhThuTheoHoaDon(@Param("start") LocalDateTime start,
                                      @Param("end")   LocalDateTime end);

    // ========================= (CÁC HÀM KHÁC KHÔNG LIÊN QUAN DOANH THU) =========================

    @Query(value = """
        SELECT CAST(COUNT(DISTINCT hd.id) AS int)
        FROM hoa_don hd
        JOIN lich_su_hoa_don ls ON ls.id_hoa_don = hd.id
        WHERE ls.thoi_gian >= :start
          AND ls.thoi_gian <  :end
          AND UPPER(REPLACE(REPLACE(ls.trang_thai COLLATE Vietnamese_100_CI_AI_SC_UTF8, N'_', N''), N' ', N'')) = N'HOANTHANH'
    """, nativeQuery = true)
    Integer demHoaDonTheoKhoangThoiGian(@Param("start") LocalDateTime start,
                                        @Param("end")   LocalDateTime end);

    @Query(value = """
SELECT COALESCE(SUM(ct.so_luong), 0)
FROM chi_tiet_don_hang ct
JOIN don_hang dh ON dh.id = ct.id_don_hang
JOIN hoa_don  hd ON hd.id_don_hang = dh.id
JOIN lich_su_hoa_don ls ON ls.id_hoa_don = hd.id
WHERE ls.thoi_gian >= :start
  AND ls.thoi_gian <  :end
  AND UPPER(REPLACE(REPLACE(ls.trang_thai COLLATE Vietnamese_100_CI_AI_SC_UTF8, N'_', N''), N' ', N'')) = N'HOANTHANH'
  AND (ct.trang_thai_hoan_tra = 0 OR ct.trang_thai_hoan_tra IS NULL)
""", nativeQuery = true)
    Integer demSanPhamTheoHoaDon(@Param("start") LocalDateTime start,
                                 @Param("end")   LocalDateTime end);

    @Query("""
    SELECT new com.example.AsmGD1.dto.ThongKe.SanPhamBanChayDTO(
        ctp.id,
        sp.id,
        sp.tenSanPham,
        COALESCE(ms.tenMau, 'Không xác định'),
        COALESCE(kc.ten, 'Không xác định'),
        CASE WHEN SUM(ct.soLuong) > 0
             THEN COALESCE(SUM(ct.thanhTien), 0) / SUM(ct.soLuong)
             ELSE 0 END,
        SUM(ct.soLuong)
    )
    FROM ChiTietDonHang ct
    JOIN ct.chiTietSanPham ctp
    JOIN ctp.sanPham sp
    LEFT JOIN ctp.mauSac ms
    LEFT JOIN ctp.kichCo kc
    JOIN ct.donHang dh
    JOIN HoaDon hd ON hd.donHang = dh
    JOIN LichSuHoaDon ls ON ls.hoaDon = hd
    WHERE ls.trangThai = 'Hoàn thành'
      AND ls.thoiGian >= :start
      AND ls.thoiGian <  :end
      AND (ct.trangThaiHoanTra IS NULL OR ct.trangThaiHoanTra = false)
    GROUP BY ctp.id, sp.id, sp.tenSanPham, ms.tenMau, kc.ten
    ORDER BY SUM(ct.soLuong) DESC
""")
    Page<SanPhamBanChayDTO> laySanPhamBanChayTheoHoaDon(@Param("start") LocalDateTime start,
                                                        @Param("end")   LocalDateTime end,
                                                        Pageable pageable);

    @Query("""
        SELECT new com.example.AsmGD1.dto.ThongKe.SanPhamTonKhoThapDTO(
            NULL,
            pd.id,
            p.id,
            p.tenSanPham,
            COALESCE(ms.tenMau, 'Không xác định'),
            COALESCE(kc.ten, 'Không xác định'),
            COALESCE(pd.gia, 0),
            pd.soLuongTonKho
        )
        FROM ChiTietSanPham pd
        JOIN pd.sanPham p
        LEFT JOIN pd.mauSac ms
        LEFT JOIN pd.kichCo kc
        WHERE pd.soLuongTonKho < :threshold
        ORDER BY pd.soLuongTonKho ASC, p.tenSanPham ASC
    """)
    Page<SanPhamTonKhoThapDTO> laySanPhamTonKhoThap(@Param("threshold") int threshold, Pageable pageable);

    // ========================= NHÃN NGÀY (HT ∪ ĐĐH ∪ TRMP) =========================
    @Query(value = """
SELECT DISTINCT CAST(COALESCE(hd.ngay_thanh_toan, hd.ngay_tao) AS date) AS d
FROM hoa_don hd
WHERE COALESCE(hd.ngay_thanh_toan, hd.ngay_tao) >= :start
  AND COALESCE(hd.ngay_thanh_toan, hd.ngay_tao) <  :end
  AND (
    UPPER(LTRIM(RTRIM(hd.trang_thai)) COLLATE Vietnamese_100_CI_AI_SC_UTF8) LIKE N'HOÀN THÀNH%'
    OR UPPER(LTRIM(RTRIM(hd.trang_thai)) COLLATE Vietnamese_100_CI_AI_SC_UTF8) LIKE N'ĐÃ ĐỔI HÀNG%'
    OR UPPER(LTRIM(RTRIM(hd.trang_thai)) COLLATE Vietnamese_100_CI_AI_SC_UTF8) LIKE N'ĐÃ TRẢ HÀNG MỘT PHẦN%'  -- ✅ THÊM
  )
ORDER BY d
""", nativeQuery = true)
    List<LocalDate> layNhanBieuDoTheoHoaDon(@Param("start") LocalDateTime start,
                                            @Param("end")   LocalDateTime end);

    @Query(value = """
        SELECT CAST(COUNT(DISTINCT hd.id) AS int)
        FROM hoa_don hd
        JOIN lich_su_hoa_don ls ON ls.id_hoa_don = hd.id
        WHERE CAST(ls.thoi_gian AS date) = :date
          AND UPPER(REPLACE(REPLACE(ls.trang_thai COLLATE Vietnamese_100_CI_AI_SC_UTF8, N'_', N''), N' ', N'')) = N'HOANTHANH'
    """, nativeQuery = true)
    Integer laySoHoaDonTheoNgay(@Param("date") LocalDate date);

    @Query(value = """
SELECT COALESCE(SUM(ct.so_luong), 0)
FROM chi_tiet_don_hang ct
JOIN don_hang dh ON dh.id = ct.id_don_hang
JOIN hoa_don  hd ON hd.id_don_hang = dh.id
JOIN lich_su_hoa_don ls ON ls.id_hoa_don = hd.id
WHERE CAST(ls.thoi_gian AS date) = :date
  AND UPPER(REPLACE(REPLACE(ls.trang_thai COLLATE Vietnamese_100_CI_AI_SC_UTF8, N'_', N''), N' ', N'')) = N'HOANTHANH'
  AND (ct.trang_thai_hoan_tra = 0 OR ct.trang_thai_hoan_tra IS NULL)
""", nativeQuery = true)
    Integer laySoSanPhamTheoNgay(@Param("date") LocalDate date);

    @Query("""
        SELECT ls.trangThai, COUNT(ls)
        FROM LichSuHoaDon ls
        JOIN ls.hoaDon hd
        WHERE ls.thoiGian BETWEEN :start AND :end
          AND ls.trangThai IS NOT NULL
        GROUP BY ls.trangThai
    """)
    List<Object[]> thongKePhanTramTatCaTrangThaiDonHang(@Param("start") LocalDateTime start,
                                                        @Param("end")   LocalDateTime end);

    @Query(value = """
        SELECT 
          CAST(ls.thoi_gian AS date) AS d,
          CAST(COUNT(DISTINCT hd.id) AS int) AS c,
          SUM(CAST(COUNT(DISTINCT hd.id) AS int)) OVER() AS total
        FROM hoa_don hd
        JOIN lich_su_hoa_don ls ON ls.id_hoa_don = hd.id
        WHERE ls.thoi_gian >= :start
          AND ls.thoi_gian <  :end
          AND UPPER(REPLACE(REPLACE(ls.trang_thai COLLATE Vietnamese_100_CI_AI_SC_UTF8, N'_', N''), N' ', N'')) = N'HOANTHANH'
        GROUP BY CAST(ls.thoi_gian AS date)
        ORDER BY d
    """, nativeQuery = true)
    List<Object[]> thongKeSoHoaDonTheoNgay(@Param("start") LocalDateTime start,
                                           @Param("end")   LocalDateTime end);

    // ========================= DOANH THU THEO NGÀY (HT ∪ ĐĐH ∪ TRMP) =========================
    @Query(value = """
WITH eligible AS (
  SELECT dh.id AS id_dh,
         CAST(COALESCE(hd.ngay_thanh_toan, hd.ngay_tao) AS date) AS d
  FROM hoa_don hd
  JOIN don_hang dh ON dh.id = hd.id_don_hang
  WHERE COALESCE(hd.ngay_thanh_toan, hd.ngay_tao) >= :start
    AND COALESCE(hd.ngay_thanh_toan, hd.ngay_tao) <  :end
    AND (
      UPPER(LTRIM(RTRIM(hd.trang_thai)) COLLATE Vietnamese_100_CI_AI_SC_UTF8) LIKE N'HOÀN THÀNH%'
      OR UPPER(LTRIM(RTRIM(hd.trang_thai)) COLLATE Vietnamese_100_CI_AI_SC_UTF8) LIKE N'ĐÃ ĐỔI HÀNG%'
      OR UPPER(LTRIM(RTRIM(hd.trang_thai)) COLLATE Vietnamese_100_CI_AI_SC_UTF8) LIKE N'ĐÃ TRẢ HÀNG MỘT PHẦN%'  -- ✅ THÊM
    )
),
lines_all AS (
  SELECT ct.id_don_hang AS id_dh,
         SUM(COALESCE(ct.thanh_tien,
             CAST(ct.so_luong AS decimal(18,6)) * CAST(ct.gia AS decimal(18,6))
         )) AS merch_all
  FROM chi_tiet_don_hang ct
  GROUP BY ct.id_don_hang
),
lines_kept AS (
  SELECT ct.id_don_hang AS id_dh,
         SUM(COALESCE(ct.thanh_tien,
             CAST(ct.so_luong AS decimal(18,6)) * CAST(ct.gia AS decimal(18,6))
         )) AS merch_kept
  FROM chi_tiet_don_hang ct
  WHERE (ct.trang_thai_hoan_tra = 0 OR ct.trang_thai_hoan_tra IS NULL)
  GROUP BY ct.id_don_hang
),
order_disc AS (
  SELECT v.id_don_hang AS id_dh,
         SUM(CAST(v.gia_tri_giam AS decimal(18,6))) AS disc_total
  FROM don_hang_phieu_giam_gia v
  WHERE UPPER(v.loai_giam_gia) = N'ORDER'
  GROUP BY v.id_don_hang
),
per_order AS (
  SELECT e.d,
         COALESCE(k.merch_kept, 0) AS merch_kept,
         COALESCE(a.merch_all, 0)  AS merch_all,
         COALESCE(d.disc_total, 0) AS disc_total
  FROM eligible e
  LEFT JOIN lines_kept k ON k.id_dh = e.id_dh
  LEFT JOIN lines_all  a ON a.id_dh = e.id_dh
  LEFT JOIN order_disc d ON d.id_dh = e.id_dh
),
per_order_rev AS (
  SELECT d,
         CAST(GREATEST(
           CASE WHEN merch_kept <= 0 THEN 0
                ELSE merch_kept - (disc_total * (CAST(merch_kept AS decimal(18,6))
                           / NULLIF(CAST(merch_all AS decimal(18,6)),0)))
           END, 0) AS decimal(18,2)) AS rev
  FROM per_order
)
SELECT d, SUM(rev) AS s
FROM per_order_rev
GROUP BY d
ORDER BY d
""", nativeQuery = true)
    List<Object[]> thongKeDoanhThuTheoNgay(@Param("start") LocalDateTime start,
                                           @Param("end")   LocalDateTime end);

}
