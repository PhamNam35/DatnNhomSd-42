package com.example.AsmGD1.dto.KhachHang;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface SanPhamProjection {
    UUID getId();
    String getTenSanPham();
    String getMaSanPham();
    String getMoTa();
    String getUrlHinhAnh();
    Boolean getTrangThai();
    UUID getDanhMucId();
    String getTenDanhMuc();
    LocalDateTime getThoiGianTao();
    BigDecimal getMinGia();
    BigDecimal getMaxGia();
    Long getTongSoLuong();
    Long getSold(); // chỉ dùng cho "bán chạy", có thể null ở "mới nhất"
}
