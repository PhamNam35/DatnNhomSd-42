package com.example.AsmGD1.repository.ThongBao;

import com.example.AsmGD1.entity.ChiTietThongBaoNhom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface KHChiTietThongBaoNhomRepository extends JpaRepository<ChiTietThongBaoNhom, UUID> {
}