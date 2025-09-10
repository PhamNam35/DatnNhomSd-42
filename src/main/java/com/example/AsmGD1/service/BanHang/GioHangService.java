package com.example.AsmGD1.service.BanHang;

import com.example.AsmGD1.dto.BanHang.GioHangDTO;
import com.example.AsmGD1.dto.BanHang.GioHangItemDTO;
import com.example.AsmGD1.entity.ChiTietSanPham;
import com.example.AsmGD1.entity.ChienDichGiamGia;
import com.example.AsmGD1.entity.DonHangTam;
import com.example.AsmGD1.entity.PhieuGiamGia;
import com.example.AsmGD1.repository.BanHang.DonHangTamRepository;
import com.example.AsmGD1.repository.GiamGia.PhieuGiamGiaRepository;
import com.example.AsmGD1.service.SanPham.ChiTietSanPhamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GioHangService {
    @Autowired
    private ChiTietSanPhamService chiTietSanPhamService;

    @Autowired
    private PhieuGiamGiaRepository phieuGiamGiaRepository;

    @Autowired
    private DonHangTamRepository donHangTamRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String CART_SESSION_KEY_PREFIX = "CART_";
    private static final String APPLIED_DISCOUNT_SESSION_KEY = "APPLIED_DISCOUNT_";
    private static final String APPLIED_VOUCHER_ID_SESSION_KEY = "APPLIED_VOUCHER_ID_";

    public String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0 VNĐ";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0 VNĐ", symbols);
        return df.format(amount);
    }

    private String getCartSessionKey(String tabId) {
        return CART_SESSION_KEY_PREFIX + tabId;
    }

    private String getDiscountSessionKey(String tabId) {
        return APPLIED_DISCOUNT_SESSION_KEY + tabId;
    }

    private String getVoucherIdSessionKey(String tabId) {
        return APPLIED_VOUCHER_ID_SESSION_KEY + tabId;
    }

    private HttpSession getSession() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attr.getRequest().getSession();
    }

    // Tính giá sau giảm dựa trên chiến dịch giảm giá
    private BigDecimal tinhGiaSauGiam(ChiTietSanPham chiTiet) {
        BigDecimal gia = chiTiet.getGia();
        ChienDichGiamGia chienDich = chiTiet.getChienDichGiamGia();
        if (chienDich != null && "ONGOING".equals(chienDich.getStatus()) && (chienDich.getSoLuong() == null || chienDich.getSoLuong() > 0)) {
            BigDecimal phanTramGiam = chienDich.getPhanTramGiam();
            if (phanTramGiam != null) {
                BigDecimal giamGia = gia.multiply(phanTramGiam).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                return gia.subtract(giamGia);
            }
        }
        return gia;
    }

    public List<GioHangItemDTO> getCurrentCart(String tabId) {
        HttpSession session = getSession();
        List<GioHangItemDTO> cart = (List<GioHangItemDTO>) session.getAttribute(getCartSessionKey(tabId));
        if (cart == null) {
            cart = new ArrayList<>();
            session.setAttribute(getCartSessionKey(tabId), cart);
        }
        return cart;
    }

    private void saveCart(String tabId, List<GioHangItemDTO> cart) {
        HttpSession session = getSession();
        session.setAttribute(getCartSessionKey(tabId), cart);
    }

    private String layAnhDauTien(ChiTietSanPham chiTiet) {
        if (chiTiet.getHinhAnhSanPhams() != null && !chiTiet.getHinhAnhSanPhams().isEmpty()) {
            return chiTiet.getHinhAnhSanPhams().get(0).getUrlHinhAnh(); // Assumes HinhAnhSanPham has getUrl()
        }
        return "https://via.placeholder.com/50";
    }


    public GioHangDTO themVaoGioHang(UUID productDetailId, int quantity, BigDecimal shippingFee, String tabId) {
        System.out.println("--- themVaoGioHang: Bắt đầu, tabId: " + tabId + " ---");
        List<GioHangItemDTO> currentCart = getCurrentCart(tabId);
        GioHangItemDTO existingItem = currentCart.stream()
                .filter(item -> item.getIdChiTietSanPham().equals(productDetailId))
                .findFirst()
                .orElse(null);

        ChiTietSanPham chiTiet = chiTietSanPhamService.findById(productDetailId);
        if (chiTiet == null) {
            System.err.println("LỖI: Không tìm thấy biến thể sản phẩm với ID: " + productDetailId);
            throw new RuntimeException("Không tìm thấy biến thể sản phẩm với ID: " + productDetailId);
        }

        BigDecimal giaSauGiam = tinhGiaSauGiam(chiTiet); // Tính giá sau giảm

        if (existingItem != null) {
            int newQuantity = existingItem.getSoLuong() + quantity;
            if (newQuantity > chiTiet.getSoLuongTonKho()) {
                System.err.println("LỖI: Số lượng tồn copa stock không đủ. Còn: " + chiTiet.getSoLuongTonKho() + ", Yêu cầu: " + newQuantity);
                throw new RuntimeException("Số lượng tồn kho không đủ sau khi cộng dồn: chỉ còn " + chiTiet.getSoLuongTonKho());
            }
            existingItem.setSoLuong(newQuantity);
            existingItem.setThanhTien(giaSauGiam.multiply(BigDecimal.valueOf(newQuantity)));
        } else {
            if (quantity > chiTiet.getSoLuongTonKho()) {
                System.err.println("LỖI: Số lượng tồn kho không đủ. Còn: " + chiTiet.getSoLuongTonKho() + ", Yêu cầu: " + quantity);
                throw new RuntimeException("Số lượng tồn kho không đủ: chỉ còn " + chiTiet.getSoLuongTonKho());
            }
            GioHangItemDTO newItem = new GioHangItemDTO();
            newItem.setIdChiTietSanPham(productDetailId);
            newItem.setTenSanPham(chiTiet.getSanPham() != null ? chiTiet.getSanPham().getTenSanPham() : "Sản phẩm không rõ");
            newItem.setMauSac(chiTiet.getMauSac() != null ? chiTiet.getMauSac().getTenMau() : "Không rõ");
            newItem.setKichCo(chiTiet.getKichCo() != null ? chiTiet.getKichCo().getTen() : "Không rõ");
            newItem.setSoLuong(quantity);
            newItem.setGia(giaSauGiam); // Sử dụng giá đã giảm
            newItem.setThanhTien(giaSauGiam.multiply(BigDecimal.valueOf(quantity)));
            newItem.setHinhAnh(layAnhDauTien(chiTiet) != null ? layAnhDauTien(chiTiet) : "https://via.placeholder.com/50");
            newItem.setAvailableStock(chiTiet.getSoLuongTonKho()); // Thêm thông tin tồn kho
            currentCart.add(newItem);
        }

        saveCart(tabId, currentCart);
        getSession().removeAttribute(getDiscountSessionKey(tabId));
        getSession().removeAttribute(getVoucherIdSessionKey(tabId));
        System.out.println("--- themVaoGioHang: Kết thúc, giỏ hàng đã thay đổi, giảm giá bị reset ---");
        return layGioHang(shippingFee, tabId);
    }

    public void capNhatTongGioHang(GioHangDTO gioHangDTO, List<GioHangItemDTO> danhSachSanPham, BigDecimal shippingFee, String tabId) {
        gioHangDTO.setDanhSachSanPham(new ArrayList<>(danhSachSanPham));
        gioHangDTO.setTabId(tabId);

        BigDecimal tongTienHang = danhSachSanPham.stream()
                .map(GioHangItemDTO::getThanhTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        gioHangDTO.setTongTienHang(tongTienHang);

        BigDecimal giamGia = (BigDecimal) getSession().getAttribute(getDiscountSessionKey(tabId));
        UUID idPhieuGiamGia = (UUID) getSession().getAttribute(getVoucherIdSessionKey(tabId));

        if (giamGia == null) {
            giamGia = BigDecimal.ZERO;
        }

        gioHangDTO.setGiamGia(giamGia);
        gioHangDTO.setIdPhieuGiamGia(idPhieuGiamGia);
        gioHangDTO.setDaApDungPhieuGiamGia(idPhieuGiamGia != null);

        BigDecimal tong = tongTienHang.subtract(giamGia).add(shippingFee != null ? shippingFee : BigDecimal.ZERO);
        gioHangDTO.setTong(tong);
    }

    public GioHangDTO layGioHang(BigDecimal shippingFee, String tabId) {
        System.out.println("--- layGioHang: Bắt đầu, tabId: " + tabId + " ---");
        List<GioHangItemDTO> currentCart = getCurrentCart(tabId);
        GioHangDTO gioHangDTO = new GioHangDTO();

        List<GioHangItemDTO> updatedCartItems = currentCart.stream().map(item -> {
            ChiTietSanPham chiTiet = chiTietSanPhamService.findById(item.getIdChiTietSanPham());
            if (chiTiet != null) {
                BigDecimal giaSauGiam = tinhGiaSauGiam(chiTiet); // Tính giá sau giảm
                item.setGia(giaSauGiam); // Cập nhật giá đã giảm
                item.setThanhTien(giaSauGiam.multiply(BigDecimal.valueOf(item.getSoLuong())));
                item.setAvailableStock(chiTiet.getSoLuongTonKho());
            }
            return item;
        }).collect(Collectors.toList());

        capNhatTongGioHang(gioHangDTO, updatedCartItems, shippingFee, tabId);

        System.out.println("Debug - layGioHang: TongTienHang: " + gioHangDTO.getTongTienHang());
        System.out.println("Debug - layGioHang: GiamGia: " + gioHangDTO.getGiamGia());
        System.out.println("Debug - layGioHang: Tong: " + gioHangDTO.getTong());
        System.out.println("Debug - layGioHang: IdPhieuGiamGia: " + gioHangDTO.getIdPhieuGiamGia());

        return gioHangDTO;
    }

    public GioHangDTO capNhatGioHang(UUID productDetailId, int quantity, BigDecimal shippingFee, String tabId) {
        System.out.println("--- capNhatGioHang: Bắt đầu, tabId: " + tabId + " ---");
        List<GioHangItemDTO> currentCart = getCurrentCart(tabId);
        GioHangItemDTO existingItem = currentCart.stream()
                .filter(item -> item.getIdChiTietSanPham().equals(productDetailId))
                .findFirst()
                .orElse(null);

        ChiTietSanPham chiTiet = chiTietSanPhamService.findById(productDetailId);
        if (chiTiet == null) {
            System.err.println("LỖI: Không tìm thấy biến thể sản phẩm với ID: " + productDetailId);
            throw new RuntimeException("Không tìm thấy biến thể sản phẩm với ID: " + productDetailId);
        }

        BigDecimal giaSauGiam = tinhGiaSauGiam(chiTiet); // Tính giá sau giảm

        if (quantity > chiTiet.getSoLuongTonKho()) {
            throw new RuntimeException("Số lượng tồn kho không đủ: chỉ còn " + chiTiet.getSoLuongTonKho());
        }

        if (existingItem != null) {
            existingItem.setSoLuong(quantity);
            existingItem.setGia(giaSauGiam); // Cập nhật giá đã giảm
            existingItem.setThanhTien(giaSauGiam.multiply(BigDecimal.valueOf(quantity)));
            existingItem.setAvailableStock(chiTiet.getSoLuongTonKho()); // Cập nhật tồn kho
        } else {
            System.out.println("CẢNH BÁO: Sản phẩm " + productDetailId + " không có trong giỏ, đang thêm mới khi cập nhật.");
            GioHangItemDTO newItem = new GioHangItemDTO();
            newItem.setIdChiTietSanPham(productDetailId);
            newItem.setTenSanPham(chiTiet.getSanPham() != null ? chiTiet.getSanPham().getTenSanPham() : "Sản phẩm không rõ");
            newItem.setMauSac(chiTiet.getMauSac() != null ? chiTiet.getMauSac().getTenMau() : "Không rõ");
            newItem.setKichCo(chiTiet.getKichCo() != null ? chiTiet.getKichCo().getTen() : "Không rõ");
            newItem.setSoLuong(quantity);
            newItem.setGia(giaSauGiam); // Sử dụng giá đã giảm
            newItem.setThanhTien(giaSauGiam.multiply(BigDecimal.valueOf(quantity)));
            newItem.setHinhAnh(layAnhDauTien(chiTiet) != null ? layAnhDauTien(chiTiet) : "https://via.placeholder.com/50");
            newItem.setAvailableStock(chiTiet.getSoLuongTonKho());
            currentCart.add(newItem);
        }

        saveCart(tabId, currentCart);
        getSession().removeAttribute(getDiscountSessionKey(tabId));
        getSession().removeAttribute(getVoucherIdSessionKey(tabId));
        System.out.println("--- capNhatGioHang: Kết thúc, giỏ hàng đã thay đổi, giảm giá bị reset ---");
        return layGioHang(shippingFee, tabId);
    }

    public GioHangDTO xoaKhoiGioHang(UUID productDetailId, BigDecimal shippingFee, String tabId) {
        System.out.println("--- xoaKhoiGioHang: Bắt đầu, tabId: " + tabId + " ---");
        List<GioHangItemDTO> currentCart = getCurrentCart(tabId);
        boolean removed = currentCart.removeIf(item -> item.getIdChiTietSanPham().equals(productDetailId));

        if (removed) {
            saveCart(tabId, currentCart);
            getSession().removeAttribute(getDiscountSessionKey(tabId));
            getSession().removeAttribute(getVoucherIdSessionKey(tabId));
            System.out.println("--- xoaKhoiGioHang: Kết thúc, sản phẩm đã xóa, giảm giá bị reset ---");
        } else {
            System.out.println("--- xoaKhoiGioHang: Không tìm thấy sản phẩm để xóa ---");
        }

        return layGioHang(shippingFee, tabId);
    }

    public GioHangDTO xoaTatCaGioHang(String tabId) {
        System.out.println("--- xoaTatCaGioHang: Bắt đầu, tabId: " + tabId + " ---");
        List<GioHangItemDTO> currentCart = getCurrentCart(tabId);
        currentCart.clear();
        saveCart(tabId, currentCart);
        getSession().removeAttribute(getDiscountSessionKey(tabId));
        getSession().removeAttribute(getVoucherIdSessionKey(tabId));
        System.out.println("--- xoaTatCaGioHang: Kết thúc, giỏ hàng đã xóa, giảm giá bị reset ---");
        return layGioHang(BigDecimal.ZERO, tabId);
    }

    public GioHangDTO apDungPhieuGiamGia(UUID voucherId, BigDecimal shippingFee, GioHangDTO gioHang, String tabId) {
        System.out.println("--- apDungPhieuGiamGia: Bắt đầu, voucherId: " + voucherId + ", shippingFee: " + shippingFee + ", tabId: " + tabId + " ---");

        BigDecimal tongTienHang = gioHang.getTongTienHang();
        System.out.println("Debug - Tổng tiền hàng từ giỏ: " + tongTienHang);

        BigDecimal giamGia = BigDecimal.ZERO;
        UUID appliedVoucherId = null;

        if (voucherId != null) {
            PhieuGiamGia phieu = phieuGiamGiaRepository.findById(voucherId)
                    .orElseThrow(() -> new RuntimeException("Phiếu giảm giá không tồn tại."));

            LocalDate today = LocalDate.now();
            System.out.println("Debug - Voucher info: " + phieu.getMa() + ", NgayBatDau: " + phieu.getNgayBatDau() +
                    ", NgayKetThuc: " + phieu.getNgayKetThuc() + ", SoLuong: " + phieu.getSoLuong() +
                    ", GiaTriGiamToiThieu: " + phieu.getGiaTriGiamToiThieu() + ", Loai: " + phieu.getLoai());

            // Kiểm tra tính hợp lệ của voucher
            LocalDateTime now = LocalDateTime.now();
            boolean isValidDate = (phieu.getNgayBatDau() == null || !phieu.getNgayBatDau().isAfter(now)) &&
                    (phieu.getNgayKetThuc() == null || !phieu.getNgayKetThuc().isBefore(now));

            boolean hasQuantity = phieu.getSoLuong() == null || phieu.getSoLuong() > 0;
            boolean meetsMinimum = phieu.getGiaTriGiamToiThieu() == null ||
                    tongTienHang.compareTo(phieu.getGiaTriGiamToiThieu()) >= 0;

            System.out.println("Debug - Voucher validation: isValidDate=" + isValidDate +
                    ", hasQuantity=" + hasQuantity + ", meetsMinimum=" + meetsMinimum);

            if (!isValidDate) {
                throw new RuntimeException("Phiếu giảm giá đã hết hạn hoặc chưa đến thời gian sử dụng.");
            }

            if (!hasQuantity) {
                throw new RuntimeException("Phiếu giảm giá đã hết lượt sử dụng.");
            }

            if (!meetsMinimum) {
                String minAmountFormatted = formatCurrency(phieu.getGiaTriGiamToiThieu());
                throw new RuntimeException("Đơn hàng chưa đạt giá trị tối thiểu " + minAmountFormatted + " để sử dụng phiếu giảm giá này.");
            }

            // Tính toán giảm giá dựa trên loại voucher
            if ("Phần trăm".equals(phieu.getLoai()) || "PERCENT".equals(phieu.getLoai())) {
                giamGia = tongTienHang.multiply(phieu.getGiaTriGiam())
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                if (phieu.getGiaTriGiamToiDa() != null && giamGia.compareTo(phieu.getGiaTriGiamToiDa()) > 0) {
                    giamGia = phieu.getGiaTriGiamToiDa();
                }
                appliedVoucherId = voucherId;
                System.out.println("Debug - Áp dụng giảm giá %: " + giamGia);
            } else if ("Tiền mặt".equals(phieu.getLoai()) || "CASH".equals(phieu.getLoai())) {
                giamGia = phieu.getGiaTriGiam();
                if (giamGia.compareTo(tongTienHang) > 0) {
                    giamGia = tongTienHang;
                }
                appliedVoucherId = voucherId;
                System.out.println("Debug - Áp dụng giảm giá tiền mặt: " + giamGia);
            } else {
                System.out.println("Debug - Loại voucher không được hỗ trợ: " + phieu.getLoai());
                throw new RuntimeException("Loại phiếu giảm giá không được hỗ trợ.");
            }
        } else {
            System.out.println("Debug - Không có voucherId, không áp dụng giảm giá.");
        }

        // Lưu thông tin giảm giá vào session
        getSession().setAttribute(getDiscountSessionKey(tabId), giamGia);
        getSession().setAttribute(getVoucherIdSessionKey(tabId), appliedVoucherId);
        System.out.println("Debug - Giảm giá lưu session: " + giamGia + ", Voucher ID: " + appliedVoucherId);

        // Cập nhật lại tổng giỏ hàng
        capNhatTongGioHang(gioHang, gioHang.getDanhSachSanPham(), shippingFee, tabId);

        System.out.println("--- apDungPhieuGiamGia: Kết thúc, Tổng tiền sau giảm: " + gioHang.getTong() + " ---");
        return gioHang;
    }

    public void saveCartToDonHangTam(GioHangDTO gioHangDTO, String tabId, UUID khachHangId, String soDienThoai) {
        try {
            DonHangTam donHangTam = donHangTamRepository.findByTabId(tabId)
                    .orElse(new DonHangTam());

            donHangTam.setId(donHangTam.getId() != null ? donHangTam.getId() : UUID.randomUUID());
            donHangTam.setKhachHang(khachHangId);
            donHangTam.setMaDonHangTam("TEMP_" + System.currentTimeMillis());
            donHangTam.setTong(gioHangDTO.getTong());
            donHangTam.setThoiGianTao(LocalDateTime.now());
            donHangTam.setDanhSachSanPham(objectMapper.writeValueAsString(gioHangDTO.getDanhSachSanPham()));
            donHangTam.setPhuongThucThanhToan(gioHangDTO.getPhuongThucThanhToan());
            donHangTam.setPhuongThucBanHang(gioHangDTO.getPhuongThucBanHang());
            donHangTam.setPhiVanChuyen(gioHangDTO.getPhiVanChuyen());
            donHangTam.setSoDienThoaiKhachHang(soDienThoai);
            donHangTam.setPhieuGiamGia(gioHangDTO.getIdPhieuGiamGia());
            donHangTam.setTabId(tabId);

            donHangTamRepository.save(donHangTam);
        } catch (Exception e) {
            System.err.println("Lỗi khi lưu đơn hàng tạm: " + e.getMessage());
            throw new RuntimeException("Lỗi khi lưu đơn hàng tạm");
        }
    }
}
