package com.example.AsmGD1.dto.KhachHang.GiaoHangNhanh;

import lombok.Data;

@Data
public class DiaChiResponse {
    private String id; // Add ID field
    private String tinhThanhPho;
    private String quanHuyen;
    private String phuongXa;
    private String chiTietDiaChi;
    private String nguoiNhan;
    private String soDienThoaiNguoiNhan;
    private Boolean macDinh;

    public String getFullAddress() {
        return chiTietDiaChi + ", " + phuongXa + ", " + quanHuyen + ", " + tinhThanhPho;
    }
}