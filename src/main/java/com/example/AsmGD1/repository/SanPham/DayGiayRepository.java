package com.example.AsmGD1.repository.SanPham;

import com.example.AsmGD1.entity.DayGiay;
import com.example.AsmGD1.entity.XuatXu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DayGiayRepository extends JpaRepository<DayGiay, UUID> {
    @Query("SELECT x FROM DayGiay x WHERE LOWER(x.tenDayGiay) LIKE LOWER(CONCAT('%', :tenDayGiay, '%'))")
    List<DayGiay> findByTenDayGiayContainingIgnoreCase(String tenDayGiay);
    Page<DayGiay> findByTenDayGiayContainingIgnoreCase(String tenDayGiay, Pageable pageable);
}