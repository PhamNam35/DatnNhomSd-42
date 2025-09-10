package com.example.AsmGD1.dto.BanHang;

import com.example.AsmGD1.entity.DonHangTam;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class DonHangTamDTO {
    private UUID id;
    private String tabId;
    private String maDonHangTam;
    private String tenKhachHang;
    private String soDienThoaiKhachHang;
    private UUID khachHangId;
    private BigDecimal tong;
    private LocalDateTime thoiGianTao;
    private String phuongThucThanhToan;
    private String phuongThucBanHang;
    private BigDecimal phiVanChuyen;
    private UUID idPhieuGiamGia;
    private List<GioHangItemDTO> danhSachSanPham;

    @JsonProperty("danh_sach_item")
    private String danhSachItem;

    public DonHangTamDTO() {
        this.danhSachSanPham = new ArrayList<>(); // Khởi tạo danh sách sản phẩm mặc định
    }

    public DonHangTamDTO(DonHangTam entity, ObjectMapper objectMapper) {
        this.id = entity.getId();
        this.tabId = entity.getTabId();
        this.maDonHangTam = entity.getMaDonHangTam();
        this.khachHangId = entity.getKhachHang();
        this.tenKhachHang = (entity.getKhachHang() != null && entity.getNguoiDung() != null) ? entity.getNguoiDung().getHoTen() : "Không rõ";
        this.soDienThoaiKhachHang = entity.getSoDienThoaiKhachHang();
        this.tong = entity.getTong() != null ? entity.getTong() : BigDecimal.ZERO;
        this.thoiGianTao = entity.getThoiGianTao();
        this.phuongThucThanhToan = entity.getPhuongThucThanhToan() != null ? entity.getPhuongThucThanhToan() : "Chưa chọn";
        this.phuongThucBanHang = entity.getPhuongThucBanHang() != null ? entity.getPhuongThucBanHang() : "Chưa chọn";
        this.phiVanChuyen = entity.getPhiVanChuyen() != null ? entity.getPhiVanChuyen() : BigDecimal.ZERO;
        this.idPhieuGiamGia = entity.getPhieuGiamGia();
        this.danhSachItem = entity.getDanhSachSanPham();
        parseDanhSachSanPham(objectMapper); // Gọi parse ngay khi khởi tạo
    }

    public void parseDanhSachSanPham(ObjectMapper objectMapper) {
        if (danhSachItem != null && !danhSachItem.isEmpty()) {
            try {
                this.danhSachSanPham = objectMapper.readValue(danhSachItem, new TypeReference<List<GioHangItemDTO>>(){});
            } catch (JsonProcessingException e) {
                System.err.println("Lỗi parse JSON danh sách sản phẩm: " + e.getMessage() + ", Dữ liệu: " + danhSachItem);
                this.danhSachSanPham = new ArrayList<>(); // Gán rỗng nếu lỗi
            }
        } else {
            this.danhSachSanPham = new ArrayList<>();
        }
    }
}