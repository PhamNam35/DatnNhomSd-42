package com.example.AsmGD1.dto.ChiTietSanPham;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ChiTietSanPhamUpdateDto {
    private UUID id;
    private UUID productId;
    private UUID colorId;
    private UUID sizeId;
    private UUID originId;
    private UUID materialId;
    private UUID styleId;
    private UUID sleeveId;
    private UUID collarId;
    private UUID brandId;
    private BigDecimal price;
    private Integer stockQuantity;
    private String gender;
    private Boolean status; // Trường trạng thái đồng bộ với trangThai
    private MultipartFile[] imageFiles;
}