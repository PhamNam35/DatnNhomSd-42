package com.example.AsmGD1.repository.SanPham;

import com.example.AsmGD1.entity.MauSac;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MauSacRepository extends JpaRepository<MauSac, UUID> {
    @Query("SELECT m FROM MauSac m WHERE LOWER(m.tenMau) LIKE LOWER(CONCAT('%', :tenMau, '%'))")
    List<MauSac> findByTenMauContainingIgnoreCase(String tenMau);

    // Thêm phương thức phân trang
    Page<MauSac> findByTenMauContainingIgnoreCase(String tenMau, Pageable pageable);
}