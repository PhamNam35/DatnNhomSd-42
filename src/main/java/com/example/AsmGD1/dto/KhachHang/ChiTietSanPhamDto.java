package com.example.AsmGD1.dto.KhachHang;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class ChiTietSanPhamDto {
    private UUID id;
    private UUID sanPhamId;
    private String tenSanPham;
    private String maSanPham;
    private String moTa;
    private String urlHinhAnh;
    private BigDecimal gia;
    private int soLuongTonKho;
    private String gioiTinh;
    private boolean trangThai;
    private UUID danhMucId;
    private String tenDanhMuc;
    private List<MauSacDto> mauSacList;
    private List<KichCoDto> kichCoList;
    private ChatLieuDto chatLieu;
    private XuatXuDto xuatXu;
    private ThuongHieuDto thuongHieu;
    private KieuDangDto kieuDang;
    private TayAoDto tayAo;
    private CoAoDto coAo;
    private List<String> hinhAnhList; // URLs từ bảng hinh_anh_san_pham

    private List<SizeColorCombination> validCombinations;

    // Thêm các trường hỗ trợ giảm giá
    private String discount; // Phần trăm giảm giá (e.g., "10%")
    private BigDecimal oldPrice; // Giá gốc
    private BigDecimal discountPercentage; // Phần trăm giảm giá
    private String discountCampaignName; // Tên chiến dịch giảm giá

    private List<KichCoDto> kichCoDTOList; // Sử dụng KichCoDto thay vì KichCo
    private List<MauSacDto  > mauSacDTOList; // Sử dụng MauSacDto thay vì MauSac

    @Data
    public static class SizeColorCombination {
        private UUID sizeId;
        private UUID colorId;
    }

}
