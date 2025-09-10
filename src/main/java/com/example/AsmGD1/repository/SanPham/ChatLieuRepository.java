package com.example.AsmGD1.repository.SanPham;

import com.example.AsmGD1.entity.ChatLieu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatLieuRepository extends JpaRepository<ChatLieu, UUID> {
    @Query("SELECT c FROM ChatLieu c WHERE LOWER(c.tenChatLieu) LIKE LOWER(CONCAT('%', :tenChatLieu, '%'))")
    List<ChatLieu> findByTenChatLieuContainingIgnoreCase(String tenChatLieu);

    // Thêm phương thức phân trang
    Page<ChatLieu> findByTenChatLieuContainingIgnoreCase(String tenChatLieu, Pageable pageable);
}