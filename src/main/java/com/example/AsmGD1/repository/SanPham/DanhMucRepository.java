package com.example.AsmGD1.repository.SanPham;

import com.example.AsmGD1.entity.DanhMuc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DanhMucRepository extends JpaRepository<DanhMuc, UUID> {
    @Query("SELECT d FROM DanhMuc d WHERE LOWER(d.tenDanhMuc) LIKE LOWER(CONCAT('%', :tenDanhMuc, '%'))")
    List<DanhMuc> findByTenDanhMucContainingIgnoreCase(String tenDanhMuc);

    // Thêm phương thức phân trang
    Page<DanhMuc> findByTenDanhMucContainingIgnoreCase(String tenDanhMuc, Pageable pageable);
}