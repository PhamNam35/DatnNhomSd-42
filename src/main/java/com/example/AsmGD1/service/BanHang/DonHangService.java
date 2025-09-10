package com.example.AsmGD1.service.BanHang;

import com.example.AsmGD1.dto.BanHang.*;
import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.BanHang.*;
import com.example.AsmGD1.repository.GiamGia.PhieuGiamGiaCuaNguoiDungRepository;
import com.example.AsmGD1.repository.GiamGia.PhieuGiamGiaRepository;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.repository.ThongKe.ThongKeRepository;
import com.example.AsmGD1.service.GiamGia.PhieuGiamGiaService;
import com.example.AsmGD1.service.HoaDon.HoaDonService;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.ChiTietSanPhamService;
import com.example.AsmGD1.service.ThongBao.ThongBaoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.LockModeType;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DonHangService {
    @Autowired
    private DonHangRepository donHangRepository;

    @Autowired
    private ChiTietDonHangRepository chiTietDonHangRepository;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private PhieuGiamGiaCuaNguoiDungRepository phieuGiamGiaCuaNguoiDungRepository;

    @Autowired
    private PhuongThucThanhToanRepository phuongThucThanhToanRepository;

    @Autowired
    private DonHangTamRepository donHangTamRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PhieuGiamGiaRepository phieuGiamGiaRepository;

    @Autowired
    private HoaDonService hoaDonService;

    @Autowired
    private NguoiDungService nguoiDungService;

    @Autowired
    private ThongKeRepository thongKeRepository;

    @Autowired
    private ThongBaoService thongBaoService;

    @Autowired
    private ChiTietSanPhamService chiTietSanPhamService;
    @Autowired
    private PhieuGiamGiaService phieuGiamGiaService;

    @Autowired
    private DonHangPhieuGiamGiaRepository donHangPhieuGiamGiaRepository;
    private BigDecimal getShipBaseFromSession(String tabId) {
        try {
            HttpSession ss = layPhien();
            return (BigDecimal) Optional
                    .ofNullable(ss.getAttribute("SHIP_FEE_" + tabId))
                    .orElse(BigDecimal.ZERO);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Retryable(
            value = { org.springframework.orm.ObjectOptimisticLockingFailureException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    public KetQuaDonHangDTO taoDonHang(DonHangDTO donHangDTO) {
        log.info("Bắt đầu tạo đơn hàng với SĐT: {}", donHangDTO.getSoDienThoaiKhachHang());

        String soDienThoai = donHangDTO.getSoDienThoaiKhachHang();
        String hoTen = donHangDTO.getTenKhachHang();
        if (soDienThoai == null || soDienThoai.trim().isEmpty()) {
            throw new RuntimeException("Số điện thoại khách hàng không được để trống.");
        }

        if ("Giao hàng".equals(donHangDTO.getPhuongThucBanHang()) &&
                (donHangDTO.getDiaChiGiaoHang() == null || donHangDTO.getDiaChiGiaoHang().trim().isEmpty()) &&
                !"Khách lẻ".equals(hoTen)) {
            throw new RuntimeException("Địa chỉ giao hàng không được để trống khi chọn phương thức giao hàng.");
        }

        NguoiDung khachHang = nguoiDungRepository.findBySoDienThoai(soDienThoai)
                .orElseThrow(() -> new RuntimeException("Khách hàng không tồn tại với số điện thoại: " + soDienThoai));

        // Lấy voucher đang treo trong session nếu FE không gửi
        HttpSession ss = layPhien();
        UUID orderVoucherIdInSession = (UUID) ss.getAttribute("ORDER_VOUCHER_ID_" + donHangDTO.getTabId());
        UUID shipVoucherIdInSession  = (UUID) ss.getAttribute("SHIP_VOUCHER_ID_" + donHangDTO.getTabId());
        if (donHangDTO.getIdPhieuGiamGia() == null)  donHangDTO.setIdPhieuGiamGia(orderVoucherIdInSession);
        if (donHangDTO.getIdPhieuFreeship() == null) donHangDTO.setIdPhieuFreeship(shipVoucherIdInSession);

        DonHang donHang = new DonHang();
        donHang.setNguoiDung(khachHang);
        donHang.setMaDonHang(taoMaDonHang());

        // PTTT, PT bán hàng, địa chỉ
        donHang.setPhuongThucThanhToan(
                phuongThucThanhToanRepository.findById(UUID.fromString(donHangDTO.getPhuongThucThanhToan())).orElse(null));
        donHang.setPhuongThucBanHang(
                donHangDTO.getPhuongThucBanHang() == null ? "Tại quầy" : donHangDTO.getPhuongThucBanHang());
        donHang.setDiaChiGiaoHang(donHangDTO.getDiaChiGiaoHang());
        donHang.setThoiGianTao(LocalDateTime.now());

        boolean isDelivery = "Giao hàng".equalsIgnoreCase(donHang.getPhuongThucBanHang());

        // ===== LẤY PHÍ SHIP GỐC =====
        // Ưu tiên phí FE gửi (PhiVanChuyen), nếu không có thì fallback session
        BigDecimal shipBase = getShipBaseFromSession(donHangDTO.getTabId());
// Nếu session chưa có (0) thì mới lấy theo FE gửi (đơn KHÔNG áp mã vẫn có phí ship)
        if (shipBase == null || shipBase.compareTo(BigDecimal.ZERO) <= 0) {
            shipBase = Optional.ofNullable(donHangDTO.getPhiVanChuyen()).orElse(BigDecimal.ZERO);
        }


        // Nếu không phải giao hàng thì phí ship = 0
        if (!isDelivery) {
            shipBase = BigDecimal.ZERO;
        }

        // Nếu FE không set PTBH nhưng có shipBase > 0, auto set "Giao hàng"
        if ((donHang.getPhuongThucBanHang() == null || donHang.getPhuongThucBanHang().isBlank())
                && shipBase.compareTo(BigDecimal.ZERO) > 0) {
            donHang.setPhuongThucBanHang("Giao hàng");
            isDelivery = true;
        }

        BigDecimal tongTien = BigDecimal.ZERO; // subtotal
        Map<UUID, Integer> soLuongTonKho = new HashMap<>();

        if (donHangDTO.getDanhSachSanPham() == null || donHangDTO.getDanhSachSanPham().isEmpty()) {
            throw new RuntimeException("Danh sách sản phẩm không được để trống.");
        }

        // Khóa & kiểm kho
        List<ChiTietSanPham> chiTietSanPhams = new ArrayList<>();
        for (GioHangItemDTO item : donHangDTO.getDanhSachSanPham()) {
            ChiTietSanPham chiTiet = chiTietSanPhamRepository
                    .findById(item.getIdChiTietSanPham(), LockModeType.PESSIMISTIC_WRITE)
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại với ID: " + item.getIdChiTietSanPham()));
            if (chiTiet.getSoLuongTonKho() < item.getSoLuong()) {
                throw new RuntimeException("Số lượng tồn kho không đủ cho sản phẩm: " + chiTiet.getSanPham().getTenSanPham());
            }
            chiTietSanPhams.add(chiTiet);
        }

        for (int i = 0; i < donHangDTO.getDanhSachSanPham().size(); i++) {
            GioHangItemDTO item = donHangDTO.getDanhSachSanPham().get(i);
            ChiTietSanPham chiTiet = chiTietSanPhams.get(i);

            ChiTietDonHang ctdh = new ChiTietDonHang();
            ctdh.setChiTietSanPham(chiTiet);
            ctdh.setSoLuong(item.getSoLuong());
            ctdh.setGia(item.getGia());
            ctdh.setTenSanPham(chiTiet.getSanPham().getTenSanPham());
            ctdh.setThanhTien(item.getThanhTien());
            ctdh.setGhiChu(item.getMauSac() + ", " + item.getKichCo());
            donHang.addChiTietDonHang(ctdh);

            ChiTietSanPham updated = chiTietSanPhamService.updateStockAndStatus(chiTiet.getId(), -item.getSoLuong());
            tongTien = tongTien.add(item.getThanhTien());
            soLuongTonKho.put(chiTiet.getId(), updated.getSoLuongTonKho());
        }

        // ===== ÁP DỤNG VOUCHER (ORDER + SHIPPING) =====
        BigDecimal subtotal = tongTien;
        final BigDecimal subtotalFinal = subtotal; // dùng trong lambda

        BigDecimal giamDon  = BigDecimal.ZERO;
        BigDecimal giamShip = BigDecimal.ZERO;

        PhieuGiamGia orderVoucher = null;
        PhieuGiamGia shipVoucher  = null;

        if (donHangDTO.getIdPhieuGiamGia() != null) {
            orderVoucher = phieuGiamGiaRepository.findById(donHangDTO.getIdPhieuGiamGia())
                    .orElseThrow(() -> new RuntimeException("Phiếu giảm giá (ORDER) không tồn tại."));
        }
        if (donHangDTO.getIdPhieuFreeship() != null) {
            shipVoucher = phieuGiamGiaRepository.findById(donHangDTO.getIdPhieuFreeship())
                    .orElseThrow(() -> new RuntimeException("Phiếu freeship (SHIPPING) không tồn tại."));
        }

        // *** đóng băng các biến dùng trong lambda ***
        final UUID paymentMethodIdFinal = donHang.getPhuongThucThanhToan() != null
                ? donHang.getPhuongThucThanhToan().getId() : null;
        final boolean isDeliveryFinal = isDelivery;

        // validate helper
        java.util.function.Consumer<PhieuGiamGia> validate = (v) -> {
            if (v == null) return;
            if (!"Đang diễn ra".equals(phieuGiamGiaService.tinhTrang(v))) {
                throw new RuntimeException("Phiếu " + v.getMa() + " không còn hiệu lực.");
            }
            if (paymentMethodIdFinal != null && v.getPhuongThucThanhToans() != null && !v.getPhuongThucThanhToans().isEmpty()) {
                boolean ok = v.getPhuongThucThanhToans().stream().anyMatch(pm -> paymentMethodIdFinal.equals(pm.getId()));
                if (!ok) throw new RuntimeException("Phiếu " + v.getMa() + " không áp dụng cho phương thức thanh toán đã chọn.");
            }
            if (!"SHIPPING".equalsIgnoreCase(v.getPhamViApDung())) {
                if (v.getGiaTriGiamToiThieu() != null && subtotalFinal.compareTo(v.getGiaTriGiamToiThieu()) < 0) {
                    throw new RuntimeException("Chưa đạt đơn tối thiểu để dùng phiếu " + v.getMa());
                }
            } else {
                if (!isDeliveryFinal) {
                    throw new RuntimeException("Freeship chỉ áp dụng cho đơn giao hàng.");
                }
            }
        };

        validate.accept(orderVoucher);
        validate.accept(shipVoucher);

        // Tính giảm
        if (orderVoucher != null && !"SHIPPING".equalsIgnoreCase(orderVoucher.getPhamViApDung())) {
            giamDon = phieuGiamGiaService.tinhTienGiamGia(orderVoucher, subtotal);
        }
        if (shipVoucher != null && "SHIPPING".equalsIgnoreCase(shipVoucher.getPhamViApDung())) {
            giamShip = phieuGiamGiaService.tinhGiamPhiShip(shipVoucher, shipBase, subtotal);
            if (giamShip == null) giamShip = BigDecimal.ZERO;
            if (giamShip.compareTo(shipBase) > 0) giamShip = shipBase; // clamp
        }

        // Tổng giảm & tổng tiền
        BigDecimal phiShipSauGiam = shipBase.subtract(giamShip).max(BigDecimal.ZERO);
        BigDecimal tongGiam       = giamDon.add(giamShip);
        BigDecimal tongTienDonHang = subtotal.subtract(giamDon).add(phiShipSauGiam).max(BigDecimal.ZERO);

        // LƯU vào ĐƠN HÀNG — luôn set dù có voucher hay không
        donHang.setPhiVanChuyen(phiShipSauGiam);
        donHang.setTienGiam(tongGiam);
        donHang.setTongTien(tongTienDonHang);

        // Tiền khách đưa & trạng thái
        BigDecimal tienKhachDua = Optional.ofNullable(donHangDTO.getSoTienKhachDua()).orElse(BigDecimal.ZERO);
        donHang.setSoTienKhachDua(tienKhachDua);
        donHang.setTrangThaiThanhToan(false);

        if ("Tại quầy".equalsIgnoreCase(donHang.getPhuongThucBanHang())) {
            if (donHang.getPhuongThucThanhToan() != null &&
                    "Tiền mặt".equals(donHang.getPhuongThucThanhToan().getTenPhuongThuc())) {
                if (tienKhachDua.compareTo(tongTienDonHang) < 0) {
                    throw new RuntimeException("Số tiền khách đưa không đủ.");
                }
                donHang.setTrangThaiThanhToan(true);
                donHang.setThoiGianThanhToan(LocalDateTime.now());
                donHang.setTrangThai("hoan_thanh");
            } else if (donHang.getPhuongThucThanhToan() != null) {
                donHang.setTrangThaiThanhToan(true);
                donHang.setThoiGianThanhToan(LocalDateTime.now());
                donHang.setTrangThai("hoan_thanh");
            } else {
                donHang.setTrangThaiThanhToan(false);
                donHang.setTrangThai("cho_xac_nhan");
            }
        } else {
            donHang.setTrangThaiThanhToan(false);
            donHang.setTrangThai("cho_xac_nhan");
        }

        // Gắn chi tiết phiếu đã áp dụng vào DonHang
        donHang.setDsPhieu(new ArrayList<>());
        if (orderVoucher != null && giamDon.compareTo(BigDecimal.ZERO) > 0) {
            DonHangPhieuGiamGia dhp = new DonHangPhieuGiamGia();
            dhp.setDonHang(donHang);
            dhp.setPhieuGiamGia(orderVoucher);
            dhp.setLoaiGiamGia("ORDER");
            dhp.setGiaTriGiam(giamDon);
            dhp.setThoiGianApDung(LocalDateTime.now());
            donHang.getDsPhieu().add(dhp);
        }
        if (shipVoucher != null && giamShip.compareTo(BigDecimal.ZERO) > 0) {
            DonHangPhieuGiamGia dhp = new DonHangPhieuGiamGia();
            dhp.setDonHang(donHang);
            dhp.setPhieuGiamGia(shipVoucher);
            dhp.setLoaiGiamGia("SHIPPING");
            dhp.setGiaTriGiam(giamShip);
            dhp.setThoiGianApDung(LocalDateTime.now());
            donHang.getDsPhieu().add(dhp);
        }

        // Lưu đơn
        donHang = donHangRepository.save(donHang);

        // Thông báo ngắn cho đơn giao hàng (nếu cần)
        if (isDeliveryFinal) {
            try {
                thongBaoService.taoThongBaoHeThong("admin", "Đơn giao hàng",
                        "Mã đơn: " + donHang.getMaDonHang());
            } catch (Exception ignore) {}
        }

        // Trừ lượt/số lượng voucher
        java.util.function.Consumer<PhieuGiamGia> deductUsage = (v) -> {
            if (v == null) return;

            if ("ca_nhan".equalsIgnoreCase(v.getKieuPhieu())) {
                PhieuGiamGiaCuaNguoiDung caNhan = phieuGiamGiaCuaNguoiDungRepository
                        .findByPhieuGiamGia_IdAndNguoiDung_Id(v.getId(), khachHang.getId()).orElse(null);
                if (caNhan == null || caNhan.getSoLuotConLai() == null || caNhan.getSoLuotConLai() <= 0) {
                    throw new RuntimeException("Phiếu cá nhân " + v.getMa() + " đã hết lượt sử dụng.");
                }
                caNhan.setSoLuotConLai(caNhan.getSoLuotConLai() - 1);
                phieuGiamGiaCuaNguoiDungRepository.save(caNhan);
            }

            if (v.getSoLuong() == null || v.getSoLuong() <= 0) {
                throw new RuntimeException("Phiếu " + v.getMa() + " đã hết số lượng.");
            }
            v.setSoLuong(v.getSoLuong() - 1);
            phieuGiamGiaRepository.save(v);
        };
        deductUsage.accept(orderVoucher);
        deductUsage.accept(shipVoucher);

        // Nếu đã thanh toán -> ghi thống kê & thông báo
        if (donHang.getTrangThaiThanhToan()) {
            thongBaoService.taoThongBaoHeThong(
                    "admin,employee",
                    "Thanh toán tại quầy",
                    "Khách hàng " + khachHang.getHoTen()
                            + " đã thanh toán tại quầy. Mã đơn: " + donHang.getMaDonHang()
                            + ". Tổng tiền: " + dinhDangTien(donHang.getTongTien())
            );

            for (ChiTietDonHang ctdh : donHang.getChiTietDonHangs()) {
                ChiTietSanPham ct = ctdh.getChiTietSanPham();
                ThongKe tk = new ThongKe();
                tk.setId(UUID.randomUUID());
                tk.setNgayThanhToan(donHang.getThoiGianThanhToan().toLocalDate());
                tk.setIdChiTietDonHang(ctdh.getId());
                tk.setIdChiTietSanPham(ct.getId());
                tk.setIdSanPham(ct.getSanPham().getId());
                tk.setTenSanPham(ct.getSanPham().getTenSanPham());
                tk.setKichCo(ct.getKichCo().getTen());
                tk.setMauSac(ct.getMauSac().getTenMau());
                tk.setSoLuongDaBan(ctdh.getSoLuong());
                tk.setDoanhThu(ctdh.getThanhTien());
                tk.setSoLuongTonKho(ct.getSoLuongTonKho());
                tk.setImageUrl(ct.getSanPham().getUrlHinhAnh());
                thongKeRepository.save(tk);
            }
        }

        // Tạo hóa đơn (record)
        hoaDonService.createHoaDonFromDonHang(donHang);

        KetQuaDonHangDTO ketQua = new KetQuaDonHangDTO();
        ketQua.setMaDonHang(donHang.getMaDonHang());
        ketQua.setSoLuongTonKho(soLuongTonKho);
        ketQua.setChangeAmount(tienKhachDua.subtract(tongTienDonHang));

        // Dọn session theo tab
        try {
            final String tabKey = donHangDTO.getTabId();
            ss.removeAttribute("tempStockChanges");
            ss.removeAttribute("pendingOrder_" + tabKey);
            ss.removeAttribute("ORDER_VOUCHER_ID_" + tabKey);
            ss.removeAttribute("ORDER_DISCOUNT_" + tabKey);
            ss.removeAttribute("SHIP_VOUCHER_ID_" + tabKey);
            ss.removeAttribute("SHIP_DISCOUNT_" + tabKey);
            ss.removeAttribute("SHIP_FEE_" + tabKey);
        } catch (Exception ignore) {}

        log.info("Tạo đơn hàng thành công với mã: {}", donHang.getMaDonHang());
        return ketQua;
    }



    private String dinhDangTien(BigDecimal soTien) {
        return String.format("%,.0f", soTien).replace(",", ".") + " VND";
    }
    // Các phương thức khác giữ nguyên
    @Transactional
    public DonHangDTO giuDonHang(DonHangDTO donHangDTO) {
        log.info("Giữ đơn hàng với SĐT: {}", donHangDTO.getSoDienThoaiKhachHang());
        try {
            DonHangTam donHangTam = new DonHangTam();
            donHangTam.setId(UUID.randomUUID());
            donHangTam.setMaDonHangTam("KH" + System.currentTimeMillis());
            donHangTam.setTabId(donHangDTO.getTabId());

            String soDienThoai = donHangDTO.getSoDienThoaiKhachHang();
            if (soDienThoai == null || soDienThoai.trim().isEmpty()) {
                throw new RuntimeException("Số điện thoại khách hàng không được để trống.");
            }

            NguoiDung khachHang = nguoiDungRepository.findBySoDienThoai(soDienThoai)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với số điện thoại: " + soDienThoai));

            donHangTam.setKhachHang(khachHang.getId());
            donHangTam.setSoDienThoaiKhachHang(donHangDTO.getSoDienThoaiKhachHang());

            BigDecimal tongTien = BigDecimal.ZERO; // subtotal
            if (donHangDTO.getDanhSachSanPham() != null && !donHangDTO.getDanhSachSanPham().isEmpty()) {
                List<GioHangItemDTO> enhancedItems = new ArrayList<>();
                for (GioHangItemDTO item : donHangDTO.getDanhSachSanPham()) {
                    if (item.getTenSanPham() == null || item.getMauSac() == null || item.getKichCo() == null) {
                        ChiTietSanPham chiTiet = chiTietSanPhamRepository.findById(item.getIdChiTietSanPham())
                                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại với ID: " + item.getIdChiTietSanPham()));

                        item.setTenSanPham(chiTiet.getSanPham().getTenSanPham());
                        item.setMauSac(chiTiet.getMauSac().getTenMau());
                        item.setKichCo(chiTiet.getKichCo().getTen());
                    }
                    enhancedItems.add(item);
                    tongTien = tongTien.add(item.getThanhTien());
                }
                donHangDTO.setDanhSachSanPham(enhancedItems);
            } else {
                throw new RuntimeException("Danh sách sản phẩm không được để trống.");
            }

            BigDecimal phiVanChuyen = donHangDTO.getPhiVanChuyen() != null ? donHangDTO.getPhiVanChuyen() : BigDecimal.ZERO;

            // ======= TÍNH GIẢM TẠM: ORDER + SHIPPING =======
            BigDecimal giamGia = BigDecimal.ZERO;
            BigDecimal giamShip = BigDecimal.ZERO;

            // ORDER voucher
            if (donHangDTO.getIdPhieuGiamGia() != null) {
                PhieuGiamGia v = phieuGiamGiaRepository.findById(donHangDTO.getIdPhieuGiamGia())
                        .orElseThrow(() -> new RuntimeException("Phiếu ORDER không tồn tại."));
                giamGia = phieuGiamGiaService.tinhTienGiamGia(v, tongTien).min(tongTien);
            }

            // SHIPPING voucher (freeship)
            if (donHangDTO.getIdPhieuFreeship() != null) {
                PhieuGiamGia vf = phieuGiamGiaRepository.findById(donHangDTO.getIdPhieuFreeship())
                        .orElseThrow(() -> new RuntimeException("Phiếu SHIPPING không tồn tại."));
                if ("Giao hàng".equalsIgnoreCase(donHangDTO.getPhuongThucBanHang()) && phiVanChuyen.compareTo(BigDecimal.ZERO) > 0) {
                    giamShip = phieuGiamGiaService.tinhGiamPhiShip(vf, phiVanChuyen, tongTien);
                }
            }

            // tổng tạm
            donHangTam.setTong(tongTien.add(phiVanChuyen).subtract(giamGia).subtract(giamShip));
            donHangTam.setPhiVanChuyen(phiVanChuyen);

            if (donHangDTO.getDanhSachSanPham() != null) {
                try {
                    String danhSachSanPhamJson = objectMapper.writeValueAsString(donHangDTO.getDanhSachSanPham());
                    donHangTam.setDanhSachSanPham(danhSachSanPhamJson);
                } catch (Exception e) {
                    throw new RuntimeException("Lỗi khi chuyển đổi danh sách sản phẩm thành JSON: " + e.getMessage());
                }
            }

            donHangTam.setPhuongThucThanhToan(donHangDTO.getPhuongThucThanhToan() != null ? donHangDTO.getPhuongThucThanhToan().toString() : null);
            donHangTam.setPhuongThucBanHang(donHangDTO.getPhuongThucBanHang());
            donHangTam.setPhieuGiamGia(donHangDTO.getIdPhieuGiamGia());
            // Nếu DonHangTam có cột cho freeship thì mở dòng dưới:
            // donHangTam.setPhieuFreeship(donHangDTO.getIdPhieuFreeship());
            donHangTam.setThoiGianTao(LocalDateTime.now());

            donHangTamRepository.save(donHangTam);

            HttpSession session = layPhien();
            Map<UUID, Map<String, Object>> tempStockChanges = (Map<UUID, Map<String, Object>>) session.getAttribute("tempStockChanges");
            if (tempStockChanges != null) {
                session.removeAttribute("tempStockChanges");
            }

            log.info("Giữ đơn hàng thành công với mã: {}", donHangTam.getMaDonHangTam());
            return donHangDTO;
        } catch (Exception e) {
            log.error("Lỗi khi giữ đơn hàng: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể giữ đơn hàng: " + e.getMessage());
        }
    }


    public List<DonHangTamDTO> layDanhSachDonHangTam() {
        log.info("Lấy danh sách đơn hàng tạm");
        List<DonHangTam> heldOrders = donHangTamRepository.findAll();
        return heldOrders.stream().map(order -> {
            DonHangTamDTO dto = new DonHangTamDTO();
            dto.setId(order.getId());
            if (order.getKhachHang() != null) {
                NguoiDung nguoiDung = nguoiDungService.findById(order.getKhachHang());
                dto.setTenKhachHang(nguoiDung != null ? nguoiDung.getHoTen() : "Không rõ");
            } else {
                dto.setTenKhachHang("Không rõ");
            }
            dto.setTong(order.getTong() != null ? order.getTong() : BigDecimal.ZERO);
            dto.setThoiGianTao(order.getThoiGianTao());
            dto.setPhuongThucThanhToan(order.getPhuongThucThanhToan() != null ? order.getPhuongThucThanhToan() : "Chưa chọn");
            dto.setPhuongThucBanHang(order.getPhuongThucBanHang() != null ? order.getPhuongThucBanHang() : "Chưa chọn");
            dto.setPhiVanChuyen(order.getPhiVanChuyen() != null ? order.getPhiVanChuyen() : BigDecimal.ZERO);
            dto.setIdPhieuGiamGia(order.getPhieuGiamGia());
            dto.setDanhSachItem(order.getDanhSachSanPham());
            dto.parseDanhSachSanPham(objectMapper);
            return dto;
        }).collect(Collectors.toList());
    }


    @Transactional
    public void xoaDonHangTam(UUID idDonHang) {
        log.info("Xóa đơn hàng tạm với ID: {}", idDonHang);
        DonHangTam donHangTam = donHangTamRepository.findById(idDonHang)
                .orElseThrow(() -> new RuntimeException("Đơn hàng tạm không tồn tại."));
        donHangTamRepository.delete(donHangTam);
        log.info("Xóa đơn hàng tạm thành công");
    }

    private String taoMaDonHang() {
        return "DH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public HttpSession layPhien() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attr.getRequest().getSession();
    }
    public Map<String, Map<String, Integer>> thongKeChiTietTheoPhuongThucVaTrangThai() {
        List<Object[]> results = donHangRepository.thongKeTheoPhuongThucVaTrangThai();
        Map<String, Map<String, Integer>> data = new LinkedHashMap<>();

        for (Object[] row : results) {
            String phuongThuc = (String) row[0];
            String trangThai = (String) row[1]; // Dùng String thay vì enum
            Long count = (Long) row[2];

            data.computeIfAbsent(phuongThuc, k -> new LinkedHashMap<>())
                    .put(trangThai, count.intValue());
        }

        return data;
    }
    public Map<String, Map<String, Integer>> thongKeChiTietTheoPhuongThucVaTrangThai(LocalDateTime start, LocalDateTime end) {
        List<DonHang> donHangs = donHangRepository.findByThoiGianTaoBetween(start, end);

        Map<String, Map<String, Integer>> result = new LinkedHashMap<>();

        for (DonHang dh : donHangs) {
            String phuongThuc = dh.getPhuongThucBanHang();
            String trangThai = dh.getTrangThai();

            result.putIfAbsent(phuongThuc, new LinkedHashMap<>());
            Map<String, Integer> trangThaiMap = result.get(phuongThuc);
            trangThaiMap.put(trangThai, trangThaiMap.getOrDefault(trangThai, 0) + 1);
        }

        return result;
    }
    public Map<String, Integer> demDonHangTheoPhuongThuc(LocalDateTime tuNgay, LocalDateTime denNgay) {
        List<Object[]> ketQua = donHangRepository.demTheoPhuongThuc(tuNgay, denNgay);
        Map<String, Integer> map = new HashMap<>();
        for (Object[] obj : ketQua) {
            String phuongThuc = (String) obj[0];
            Long soLuong = (Long) obj[1];
            map.put(phuongThuc, soLuong.intValue());
        }
        return map;
    }


}
