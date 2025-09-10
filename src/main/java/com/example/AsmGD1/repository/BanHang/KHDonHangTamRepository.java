package com.example.AsmGD1.repository.BanHang;


import com.example.AsmGD1.entity.DonHangTam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface KHDonHangTamRepository extends JpaRepository<DonHangTam, UUID> {
    Optional<DonHangTam> findByMaDonHangTam(String maDonHangTam);
}