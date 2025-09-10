package com.example.AsmGD1.dto.BanHang;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class GioHangDTO {
    private List<GioHangItemDTO> danhSachSanPham;
    private BigDecimal tongTienHang;
    private BigDecimal giamGia;             // giảm đơn (ORDER)
    private BigDecimal tong;
    private boolean daXoaPhieuGiamGia;
    private String giamGiaDaXoa;
    private boolean daApDungPhieuGiamGia;
    private BigDecimal phiVanChuyen;        // phí ship SAU GIẢM (để ngược-compat)

    // 🔥 NEW: tách phí vận chuyển
    private BigDecimal phiVanChuyenGoc = BigDecimal.ZERO;   // phí GHN ban đầu (chưa áp freeship)
    private BigDecimal giamGiaVanChuyen = BigDecimal.ZERO;  // số tiền giảm phí ship (freeship)

    private UUID khachHangDaChon;
    private UUID idPhieuGiamGia;            // voucher ORDER (giữ nguyên)
    private String phuongThucBanHang;
    private String phuongThucThanhToan;
    private String soDienThoaiKhachHang;
    private String tabId;
    private BigDecimal soTienKhachDua;
    private BigDecimal changeAmount;
}
