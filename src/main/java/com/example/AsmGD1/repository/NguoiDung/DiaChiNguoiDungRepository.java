package com.example.AsmGD1.repository.NguoiDung;

import com.example.AsmGD1.entity.DiaChiNguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiaChiNguoiDungRepository extends JpaRepository<DiaChiNguoiDung, UUID> {

    List<DiaChiNguoiDung> findByNguoiDung_Id(UUID nguoiDungId);

    Optional<DiaChiNguoiDung> findByNguoiDung_IdAndMacDinhTrue(UUID nguoiDungId);

    @Modifying
    @Query("UPDATE DiaChiNguoiDung d SET d.macDinh = false WHERE d.nguoiDung.id = :nguoiDungId")
    void removeDefaultFlag(UUID nguoiDungId);

    @Query("SELECT d FROM DiaChiNguoiDung d WHERE d.nguoiDung.id = :nguoiDungId AND d.macDinh = false")
    List<DiaChiNguoiDung> findByNguoiDung_IdAndMacDinhFalse(UUID nguoiDungId);

    @Modifying
    @Query("DELETE FROM DiaChiNguoiDung d WHERE d.nguoiDung.id = :nguoiDungId")
    void deleteByNguoiDungId(UUID nguoiDungId);
}
