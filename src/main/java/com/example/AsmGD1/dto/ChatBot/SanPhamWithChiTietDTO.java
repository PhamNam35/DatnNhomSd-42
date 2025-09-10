package com.example.AsmGD1.dto.ChatBot;


import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SanPhamWithChiTietDTO {
    private UUID id;
    private String maSanPham;
    private String tenSanPham;
    private String tenDanhMuc;
    private String urlHinhAnh;
    private long tongSoLuong;
    private List<ChiTietSanPhamDTO> chiTietSanPhams;

    private List<HoaDonSanPhamDTO> hoaDonSanPhams;
}
