package com.example.AsmGD1.repository.BanHang;

import com.example.AsmGD1.entity.DonHangPhieuGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
@Repository
public interface DonHangPhieuGiamGiaRepository extends JpaRepository<DonHangPhieuGiamGia, UUID> {
    List<DonHangPhieuGiamGia> findByDonHang_IdOrderByThoiGianApDungAsc(UUID donHangId);

}
