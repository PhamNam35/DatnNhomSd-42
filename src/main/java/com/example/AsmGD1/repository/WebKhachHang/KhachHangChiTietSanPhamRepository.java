package com.example.AsmGD1.repository.WebKhachHang;


import com.example.AsmGD1.entity.ChiTietSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KhachHangChiTietSanPhamRepository extends JpaRepository<ChiTietSanPham, UUID> {
    @Query("SELECT c FROM ChiTietSanPham c WHERE c.sanPham.id = :sanPhamId AND c.kichCo.id = :sizeId AND c.mauSac.id = :colorId")
    Optional<ChiTietSanPham> findBySanPhamIdAndSizeIdAndColorId(UUID sanPhamId, UUID sizeId, UUID colorId);
}

