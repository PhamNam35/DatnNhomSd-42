package com.example.AsmGD1.repository.BanHang;

import com.example.AsmGD1.entity.ChiTietDonHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChiTietDonHangRepository extends JpaRepository<ChiTietDonHang, UUID> {
    List<ChiTietDonHang> findByDonHangId(UUID donHangId);

    @Query("SELECT c FROM ChiTietDonHang c WHERE c.donHang.id = :donHangId AND c.trangThaiHoanTra = false")
    List<ChiTietDonHang> findByDonHangIdAndTrangThaiHoanTraFalse(UUID donHangId);

    @Query("SELECT c FROM ChiTietDonHang c JOIN c.chiTietSanPham ctsp WHERE ctsp.sanPham.id = :idSanPham")
    List<ChiTietDonHang> findBySanPhamId(@Param("idSanPham") UUID idSanPham);

}