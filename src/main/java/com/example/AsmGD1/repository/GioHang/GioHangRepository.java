package com.example.AsmGD1.repository.GioHang;

import com.example.AsmGD1.entity.GioHang;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface GioHangRepository extends JpaRepository<GioHang, UUID> {
    GioHang findByNguoiDungId(UUID nguoiDungId);
    boolean existsByNguoiDungId(UUID nguoiDungId);
}