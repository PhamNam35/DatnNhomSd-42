package com.example.AsmGD1.service.PhuongThucThanhToan;

import com.example.AsmGD1.entity.PhuongThucThanhToan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhuongThucThanhToanService {
    List<PhuongThucThanhToan> findAll();
    Optional<PhuongThucThanhToan> findById(UUID id);
}