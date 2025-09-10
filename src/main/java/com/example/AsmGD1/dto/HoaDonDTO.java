    package com.example.AsmGD1.dto;

    import com.example.AsmGD1.dto.BanHang.GioHangItemDTO;
    import lombok.Data;

    import java.math.BigDecimal;
    import java.time.LocalDateTime;
    import java.util.ArrayList;
    import java.util.List;
    import java.util.UUID;

    @Data
    public class HoaDonDTO {
        private UUID id;
        private String maHoaDon;
        private String tenKhachHang;
        private String soDienThoaiKhachHang;
        private String diaChi; // Thêm trường diaChi
        private String tenNhanVien; // Thêm trường này
        private BigDecimal tongTienHang;
        private BigDecimal tienGiam;
        private BigDecimal tongTien;
        private String phuongThucThanhToan;
        private String trangThaiThanhToan;
        private LocalDateTime thoiGianTao;
        private LocalDateTime ngayThanhToan; // Thêm trường này
        private String ghiChu; // Thay diaChiGiaoHang bằng ghiChu
        private List<GioHangItemDTO> danhSachSanPham;
        private BigDecimal phiVanChuyen; // Thêm trường này
        private String phuongThucBanHang; // Thêm trường này
        private List<LichSuDTO> lichSuHoaDons = new ArrayList<>();
        private BigDecimal giamPhiVanChuyen; // Thêm trường này
        private String phieuGiamGia; // Thêm trường này
        private BigDecimal tongGiamShip; // Thay giamPhiVanChuyen thành tongGiamShip để nhất quán
        private BigDecimal tongGiamOrder; // Thêm trường này


        @Data
        public static class LichSuDTO {
            private LocalDateTime thoiGian;
            private String trangThai;
            private String ghiChu;
        }
    }
