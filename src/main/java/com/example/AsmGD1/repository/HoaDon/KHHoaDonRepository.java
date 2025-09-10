package com.example.AsmGD1.repository.HoaDon;

import com.example.AsmGD1.entity.HoaDon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface KHHoaDonRepository extends JpaRepository<HoaDon, UUID> {
}
