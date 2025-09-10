package com.example.AsmGD1.repository.SanPham;

import com.example.AsmGD1.entity.TayAo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TayAoRepository extends JpaRepository<TayAo, UUID> {
    @Query("SELECT t FROM TayAo t WHERE LOWER(t.tenTayAo) LIKE LOWER(CONCAT('%', :tenTayAo, '%'))")
    List<TayAo> findByTenTayAoContainingIgnoreCase(String tenTayAo);

    // Thêm phương thức phân trang
    Page<TayAo> findByTenTayAoContainingIgnoreCase(String tenTayAo, Pageable pageable);
}