package com.example.AsmGD1.dto.ViThanhToan;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class LichSuGiaoDichViDTO {
    private UUID id;
    private UUID idViThanhToan;
    private UUID idDonHang;
    private String loaiGiaoDich;
    private BigDecimal soTien;
    private LocalDateTime thoiGianGiaoDich;
    private String moTa;
    private String maGiaoDich;
    private LocalDateTime createdAt;
    private String anhBangChung;
}