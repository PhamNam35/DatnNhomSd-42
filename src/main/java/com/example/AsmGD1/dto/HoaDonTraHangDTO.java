package com.example.AsmGD1.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class HoaDonTraHangDTO {
    private String maDonHang;
    private String hoTenKhachHang;
    private List<String> tenSanPhams; // Danh sách tên sản phẩm
    private int tongSoLuong; // Tổng số lượng trả
    private BigDecimal tongTienHoan; // Tổng tiền hoàn
    private String formattedTongHoan; // Tiền hoàn định dạng
    private String lyDoTraHang; // Lý do trả hàng (gộp hoặc lấy lý do đầu tiên)
    private String trangThai; // Trạng thái
    private LocalDateTime thoiGianTra; // Thời gian yêu cầu trả
    private UUID id; // ID của yêu cầu trả hàng (có thể lấy ID của yêu cầu đầu tiên)

    // Constructor
    public HoaDonTraHangDTO(String maDonHang, String hoTenKhachHang, List<String> tenSanPhams,
                            int tongSoLuong, BigDecimal tongTienHoan, String formattedTongHoan,
                            String lyDoTraHang, String trangThai, LocalDateTime thoiGianTra, UUID id) {
        this.maDonHang = maDonHang;
        this.hoTenKhachHang = hoTenKhachHang;
        this.tenSanPhams = tenSanPhams;
        this.tongSoLuong = tongSoLuong;
        this.tongTienHoan = tongTienHoan;
        this.formattedTongHoan = formattedTongHoan;
        this.lyDoTraHang = lyDoTraHang;
        this.trangThai = trangThai;
        this.thoiGianTra = thoiGianTra;
        this.id = id;
    }

    // Getters and setters
    public String getMaDonHang() { return maDonHang; }
    public void setMaDonHang(String maDonHang) { this.maDonHang = maDonHang; }
    public String getHoTenKhachHang() { return hoTenKhachHang; }
    public void setHoTenKhachHang(String hoTenKhachHang) { this.hoTenKhachHang = hoTenKhachHang; }
    public List<String> getTenSanPhams() { return tenSanPhams; }
    public void setTenSanPhams(List<String> tenSanPhams) { this.tenSanPhams = tenSanPhams; }
    public int getTongSoLuong() { return tongSoLuong; }
    public void setTongSoLuong(int tongSoLuong) { this.tongSoLuong = tongSoLuong; }
    public BigDecimal getTongTienHoan() { return tongTienHoan; }
    public void setTongTienHoan(BigDecimal tongTienHoan) { this.tongTienHoan = tongTienHoan; }
    public String getFormattedTongHoan() { return formattedTongHoan; }
    public void setFormattedTongHoan(String formattedTongHoan) { this.formattedTongHoan = formattedTongHoan; }
    public String getLyDoTraHang() { return lyDoTraHang; }
    public void setLyDoTraHang(String lyDoTraHang) { this.lyDoTraHang = lyDoTraHang; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public LocalDateTime getThoiGianTra() { return thoiGianTra; }
    public void setThoiGianTra(LocalDateTime thoiGianTra) { this.thoiGianTra = thoiGianTra; }
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
}