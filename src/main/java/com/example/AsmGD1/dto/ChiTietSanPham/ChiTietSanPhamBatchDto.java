package com.example.AsmGD1.dto.ChiTietSanPham;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class ChiTietSanPhamBatchDto {
    private UUID productId;
    private UUID originId;
    private UUID materialId;
    private UUID styleId;
    private UUID sleeveId;
    private UUID collarId;
    private UUID brandId;
    private String gender;
    private List<ChiTietSanPhamVariationDto> variations;
    private Map<UUID, List<MultipartFile>> colorImages; // Thêm trường mới để lưu ảnh theo màu sắc
}