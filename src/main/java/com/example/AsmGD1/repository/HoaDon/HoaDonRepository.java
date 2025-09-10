package com.example.AsmGD1.repository.HoaDon;

import com.example.AsmGD1.entity.DonHang;
import com.example.AsmGD1.entity.HoaDon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, UUID> {

    List<HoaDon> findByTrangThai(String trangThai);

    List<HoaDon> findByDonHang_NguoiDungIdAndDonHang_MaDonHangContainingIgnoreCase(UUID nguoiDungId, String maDonHang);
    Optional<HoaDon> findByDonHangId(UUID donHangId);

    Optional<HoaDon> findByDonHang_MaDonHang(String maDonHang);

    HoaDon findByDonHang_Id(UUID idDonHang);


    @Query("SELECT h FROM HoaDon h JOIN h.donHang d JOIN h.nguoiDung n " +
            "LEFT JOIN h.lichSuHoaDons ls " +
            "WHERE (:keyword IS NULL OR UPPER(d.maDonHang) LIKE UPPER(CONCAT('%', :keyword, '%')) " +
            "OR UPPER(n.hoTen) LIKE UPPER(CONCAT('%', :keyword, '%')) " +
            "OR UPPER(n.soDienThoai) LIKE UPPER(CONCAT('%', :keyword, '%'))) " +
            "AND (:trangThai IS NULL OR ls.trangThai = :trangThai) " +
            "AND (:paymentMethod IS NULL OR h.phuongThucThanhToan.tenPhuongThuc = :paymentMethod) " +
            "AND (:salesMethod IS NULL OR d.phuongThucBanHang = :salesMethod) " +
            "AND (ls.thoiGian = (SELECT MAX(ls2.thoiGian) FROM LichSuHoaDon ls2 WHERE ls2.hoaDon = h) OR ls.thoiGian IS NULL)")
    Page<HoaDon> searchByKeywordAndFilters(@Param("keyword") String keyword,
                                           @Param("trangThai") String trangThai,
                                           @Param("paymentMethod") String paymentMethod,
                                           @Param("salesMethod") String salesMethod,
                                           Pageable pageable);

    Optional<HoaDon> findByDonHang(DonHang donHang);

    @Query("SELECT h FROM HoaDon h WHERE UPPER(h.donHang.maDonHang) LIKE UPPER(CONCAT('%', :maDonHang, '%'))")
    Page<HoaDon> findByDonHangMaDonHangContainingIgnoreCase(@Param("value") String maDonHang, Pageable pageable);

    Optional<HoaDon> findByDonHangMaDonHang(String maDonHang);
    List<HoaDon> findByDonHang_NguoiDungId(UUID nguoiDungId);

    List<HoaDon> findByDonHang_NguoiDungIdAndTrangThai(UUID nguoiDungId, String trangThai);

    // Thêm phương thức hỗ trợ phân trang
    Page<HoaDon> findByDonHang_NguoiDungId(UUID nguoiDungId, Pageable pageable);

    // Thêm phương thức hỗ trợ phân trang với điều kiện trạng thái
    Page<HoaDon> findByDonHang_NguoiDungIdAndTrangThai(UUID nguoiDungId, String trangThai, Pageable pageable);
}