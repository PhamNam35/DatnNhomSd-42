package com.example.AsmGD1.dto.SanPham;

import com.example.AsmGD1.entity.KichCo;
import com.example.AsmGD1.entity.MauSac;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import java.time.LocalDateTime; // THÊM nếu chưa có

@Data
public class SanPhamDto {
    private UUID id;
    private String tenSanPham;
    private String maSanPham;
    private String moTa;
    private String urlHinhAnh;
    private Boolean trangThai;
    private UUID danhMucId;
    private String tenDanhMuc;
    private LocalDateTime thoiGianTao; // 👈 THÊM dòng này



    private BigDecimal discountedPrice; // Giá sau giảm (nếu có)
    private String discountCampaignName; // Tên chiến dịch giảm giá (nếu có)
    private BigDecimal discountPercentage; // Phần trăm giảm giá (nếu có)
    // Thêm các trường cho flash sale
    private String price;
    private String oldPrice;
    private String discount;
    private String sold;
    private Integer progress;
    private long tongSoLuong;
    private List<KichCo> kichCoList; // Thêm danh sách kích cỡ
    private List<MauSac> mauSacList; // Thêm danh sách màu sắc


}



