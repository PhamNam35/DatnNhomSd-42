package com.example.AsmGD1.repository.BanHang;

import com.example.AsmGD1.entity.PhuongThucThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhuongThucThanhToanRepository extends JpaRepository<PhuongThucThanhToan, UUID> {

    Optional<PhuongThucThanhToan> findByTenPhuongThuc(String tenPhuongThuc);

    // Lấy danh sách ID PTTT gắn với 1 phiếu
    @Query(value = """
        SELECT id_phuong_thuc_thanh_toan
        FROM phieu_giam_gia_phuong_thuc_thanh_toan
        WHERE id_phieu_giam_gia = :voucherId
        """, nativeQuery = true)
    List<UUID> findSelectedPtttIdsByVoucherId(@Param("voucherId") UUID voucherId);

    // Lấy full entity PTTT gắn với 1 phiếu
    @Query(value = """
        SELECT pttt.*
        FROM phuong_thuc_thanh_toan pttt
        JOIN phieu_giam_gia_phuong_thuc_thanh_toan l
             ON l.id_phuong_thuc_thanh_toan = pttt.id
        WHERE l.id_phieu_giam_gia = :voucherId
        """, nativeQuery = true)
    List<PhuongThucThanhToan> findSelectedPtttByVoucherId(@Param("voucherId") UUID voucherId);
}
