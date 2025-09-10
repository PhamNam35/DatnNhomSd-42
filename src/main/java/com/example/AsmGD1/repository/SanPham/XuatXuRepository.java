package com.example.AsmGD1.repository.SanPham;

import com.example.AsmGD1.entity.XuatXu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface XuatXuRepository extends JpaRepository<XuatXu, UUID> {
    @Query("SELECT x FROM XuatXu x WHERE LOWER(x.tenXuatXu) LIKE LOWER(CONCAT('%', :tenXuatXu, '%'))")
    List<XuatXu> findByTenXuatXuContainingIgnoreCase(String tenXuatXu);
    Page<XuatXu> findByTenXuatXuContainingIgnoreCase(String tenXuatXu, Pageable pageable);
}