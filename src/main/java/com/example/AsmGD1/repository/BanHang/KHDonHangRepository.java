package com.example.AsmGD1.repository.BanHang;

import com.example.AsmGD1.entity.DonHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface KHDonHangRepository extends JpaRepository<DonHang, UUID> {
}