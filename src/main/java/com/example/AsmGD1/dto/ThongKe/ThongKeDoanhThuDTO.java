package com.example.AsmGD1.dto.ThongKe;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ThongKeDoanhThuDTO {
    private BigDecimal doanhThuNgay;
    private BigDecimal doanhThuThang;
    private BigDecimal doanhThuNam;
    private Double tangTruongNgay;
    private Double tangTruongThang;
    private Double tangTruongNam;
    private Integer soDonHangNgay;
    private Integer soDonHangThang;
    private Integer soSanPhamThang;
    private Double tangTruongSanPhamThang;
}
