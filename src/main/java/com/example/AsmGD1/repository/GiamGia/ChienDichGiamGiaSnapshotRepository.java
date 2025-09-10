package com.example.AsmGD1.repository.GiamGia;

import com.example.AsmGD1.entity.ChienDichGiamGiaSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChienDichGiamGiaSnapshotRepository extends JpaRepository<ChienDichGiamGiaSnapshot, UUID> {
    boolean existsByCampaign_Id(UUID campaignId);
    Optional<ChienDichGiamGiaSnapshot> findByCampaign_Id(UUID campaignId);
}
