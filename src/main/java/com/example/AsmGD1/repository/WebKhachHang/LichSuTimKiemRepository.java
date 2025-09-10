package com.example.AsmGD1.repository.WebKhachHang;

import com.example.AsmGD1.entity.LichSuTimKiem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface LichSuTimKiemRepository extends JpaRepository<LichSuTimKiem, UUID> {
    @Query("SELECT l FROM LichSuTimKiem l WHERE l.nguoiDung.id = :nguoiDungId ORDER BY l.thoiGianTimKiem DESC")
    List<LichSuTimKiem> findByNguoiDungId(UUID nguoiDungId);
    List<LichSuTimKiem> findByNguoiDungIdOrderByThoiGianTimKiemDesc(UUID nguoiDungId);
    void deleteByNguoiDungId(UUID nguoiDungId);

    @Modifying
    @Transactional
    @Query("DELETE FROM LichSuTimKiem l WHERE l.tuKhoa = :tuKhoa AND l.nguoiDung.id = :nguoiDungId")
    void deleteByTuKhoaAndNguoiDungId(@Param("tuKhoa") String tuKhoa, @Param("nguoiDungId") UUID nguoiDungId);
}