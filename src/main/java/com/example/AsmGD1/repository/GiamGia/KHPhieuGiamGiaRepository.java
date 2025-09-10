package com.example.AsmGD1.repository.GiamGia;

import com.example.AsmGD1.entity.PhieuGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface KHPhieuGiamGiaRepository extends JpaRepository<PhieuGiamGia, UUID> {
    Optional<PhieuGiamGia> findByMa(String ma);

}
