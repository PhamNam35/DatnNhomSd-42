package com.example.AsmGD1.repository.SanPham;

import com.example.AsmGD1.entity.CoAo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CoAoRepository extends JpaRepository<CoAo, UUID> {
    @Query("SELECT c FROM CoAo c WHERE LOWER(c.tenCoAo) LIKE LOWER(CONCAT('%', :tenCoAo, '%'))")
    List<CoAo> findByTenCoAoContainingIgnoreCase(String tenCoAo);

    // Thêm phương thức phân trang
    Page<CoAo> findByTenCoAoContainingIgnoreCase(String tenCoAo, Pageable pageable);
}