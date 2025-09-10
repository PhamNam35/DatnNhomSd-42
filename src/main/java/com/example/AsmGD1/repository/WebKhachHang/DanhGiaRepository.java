package com.example.AsmGD1.repository.WebKhachHang;

import com.example.AsmGD1.entity.DanhGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface DanhGiaRepository extends JpaRepository<DanhGia, UUID> {
    List<DanhGia> findByChiTietSanPham_SanPham_IdOrderByThoiGianDanhGiaDesc(UUID sanPhamId);

    boolean existsByHoaDonIdAndChiTietSanPhamIdAndNguoiDungId(UUID hoaDonId, UUID chiTietSanPhamId, UUID nguoiDungId);
    boolean existsByHoaDonIdAndNguoiDungId(UUID hoaDonId, UUID nguoiDungId);
    @Query("SELECT AVG(d.xepHang) FROM DanhGia d WHERE d.chiTietSanPham.id = :chiTietSanPhamId AND d.trangThai = true")
    Double findAverageRatingByChiTietSanPhamId(UUID chiTietSanPhamId);
}
