package com.example.AsmGD1.repository.ThongBao;

import com.example.AsmGD1.entity.ThongBaoNhom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface KHThongBaoNhomRepository extends JpaRepository<ThongBaoNhom, UUID> {
}
