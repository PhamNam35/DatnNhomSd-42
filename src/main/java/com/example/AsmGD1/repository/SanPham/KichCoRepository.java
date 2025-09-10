package com.example.AsmGD1.repository.SanPham;

import com.example.AsmGD1.entity.KichCo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KichCoRepository extends JpaRepository<KichCo, UUID> {
    @Query("SELECT k FROM KichCo k WHERE LOWER(k.ten) LIKE LOWER(CONCAT('%', :ten, '%'))")
    List<KichCo> findByTenContainingIgnoreCase(String ten);

    // Thêm phương thức phân trang
    Page<KichCo> findByTenContainingIgnoreCase(String ten, Pageable pageable);
}