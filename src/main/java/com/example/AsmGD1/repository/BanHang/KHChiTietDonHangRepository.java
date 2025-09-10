package com.example.AsmGD1.repository.BanHang;

import com.example.AsmGD1.entity.ChiTietDonHang;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface KHChiTietDonHangRepository extends JpaRepository<ChiTietDonHang, UUID> {
}
