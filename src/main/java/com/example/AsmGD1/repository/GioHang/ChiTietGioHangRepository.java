package com.example.AsmGD1.repository.GioHang;

import com.example.AsmGD1.entity.ChiTietGioHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChiTietGioHangRepository extends JpaRepository<ChiTietGioHang, UUID> {
    List<ChiTietGioHang> findByGioHangId(UUID gioHangId);

    boolean existsByGioHangIdAndChiTietSanPhamId(UUID gioHangId, UUID chiTietSanPhamId);

    void deleteByGioHangIdAndChiTietSanPhamId(UUID gioHangId, UUID chiTietSanPhamId);

    Optional<ChiTietGioHang> findByGioHangIdAndChiTietSanPhamId(UUID gioHangId, UUID chiTietSanPhamId);
    void deleteByGioHang_Id(UUID gioHangId);

    @Query("SELECT c FROM ChiTietGioHang c JOIN FETCH c.chiTietSanPham cs JOIN FETCH cs.hinhAnhSanPhams WHERE c.gioHang.id = :gioHangId")
    List<ChiTietGioHang> findByGioHangIdWithHinhAnh(UUID gioHangId);

}