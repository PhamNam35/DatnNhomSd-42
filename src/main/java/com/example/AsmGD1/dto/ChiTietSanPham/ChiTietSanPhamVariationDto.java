package com.example.AsmGD1.dto.ChiTietSanPham;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class ChiTietSanPhamVariationDto {
    private UUID colorId;
    private UUID sizeId;
    private BigDecimal price;
    private Integer stockQuantity;
    private Boolean status;
    private List<MultipartFile> imageFiles;
}