package com.example.AsmGD1.repository.SanPham;

import com.example.AsmGD1.entity.KieuDang;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KieuDangRepository extends JpaRepository<KieuDang, UUID> {
    @Query("SELECT k FROM KieuDang k WHERE LOWER(k.tenKieuDang) LIKE LOWER(CONCAT('%', :tenKieuDang, '%'))")
    List<KieuDang> findByTenKieuDangContainingIgnoreCase(String tenKieuDang);

    // Thêm phương thức phân trang
    Page<KieuDang> findByTenKieuDangContainingIgnoreCase(String tenKieuDang, Pageable pageable);
}