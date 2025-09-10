package com.example.AsmGD1.dto.GiamGia;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;
@Data
public class SnapshotDetailItem {
    private UUID chiTietId;
    private UUID sanPhamId;
    private String maSanPham;
    private String tenSanPham;
    private String mau;   // nếu không có thì để null
    private String size;  // nếu không có thì để null
    private BigDecimal gia; // nếu không muốn lưu giá thì để null
    private Integer ton;
}
