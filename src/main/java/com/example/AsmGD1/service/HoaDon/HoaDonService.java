package com.example.AsmGD1.service.HoaDon;

import com.example.AsmGD1.dto.BanHang.GioHangItemDTO;
import com.example.AsmGD1.dto.HoaDonDTO;
import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.BanHang.ChiTietDonHangRepository;
import com.example.AsmGD1.repository.BanHang.DonHangPhieuGiamGiaRepository;
import com.example.AsmGD1.repository.BanHang.DonHangRepository;
import com.example.AsmGD1.repository.HoaDon.HoaDonRepository;
import com.example.AsmGD1.repository.HoaDon.LichSuHoaDonRepository;
import com.example.AsmGD1.repository.HoaDon.LichSuTraHangRepository;
import com.example.AsmGD1.repository.NguoiDung.DiaChiNguoiDungRepository;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.repository.ViThanhToan.LichSuGiaoDichViRepository;
import com.example.AsmGD1.repository.ViThanhToan.ViThanhToanRepository;
import com.example.AsmGD1.repository.WebKhachHang.LichSuDoiSanPhamRepository;
import com.example.AsmGD1.service.GiamGia.PhieuGiamGiaService;
import com.example.AsmGD1.service.ThongBao.ThongBaoService;
import com.example.AsmGD1.service.WebKhachHang.EmailService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;

@Service
public class HoaDonService {

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private DonHangRepository donHangRepository;

    @Autowired
    private LichSuHoaDonRepository lichSuHoaDonRepository;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @Autowired
    private ChiTietDonHangRepository chiTietDonHangRepository;

    @Autowired
    private LichSuTraHangRepository lichSuTraHangRepository;

    @Autowired
    private ViThanhToanRepository viThanhToanRepo;

    @Autowired
    private LichSuGiaoDichViRepository lichSuRepo;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ThongBaoService thongBaoService;

    @Autowired
    private PhieuGiamGiaService phieuGiamGiaService;

    @Autowired
    private LichSuDoiSanPhamRepository lichSuDoiSanPhamRepository;

    @Autowired
    private DonHangPhieuGiamGiaRepository donHangPhieuGiamGiaRepository;
    @Autowired
    private DiaChiNguoiDungRepository diaChiNguoiDungRepository;

    @Transactional
    public DonHang taoDonChenhLechDoiHang(HoaDon hoaDonGoc,
                                          BigDecimal chenhLech,
                                          int soLuongThayThe,
                                          UUID chiTietSanPhamThayTheId,
                                          String ghiChuThem,
                                          PhuongThucThanhToan pttt,      // ✅ mới
                                          boolean daThanhToan) {          // ✅ mới
        if (chenhLech == null || chenhLech.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Chênh lệch phải >= 0 để tạo đơn phụ thu/0đ.");
        }
        if (soLuongThayThe <= 0) {
            throw new IllegalArgumentException("Số lượng thay thế phải > 0.");
        }

        ChiTietSanPham ctspThayThe = chiTietSanPhamRepository.findById(chiTietSanPhamThayTheId)
                .orElseThrow(() -> new RuntimeException("Chi tiết sản phẩm thay thế không tồn tại."));

        // 1) Đơn chênh lệch (ship 0, copy info)
        DonHang newOrder = new DonHang();
        newOrder.setNguoiDung(hoaDonGoc.getNguoiDung());
        newOrder.setMaDonHang("EX" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        newOrder.setTrangThaiThanhToan(daThanhToan);                  // ✅ theo cờ
        newOrder.setPhiVanChuyen(BigDecimal.ZERO);
        newOrder.setPhuongThucThanhToan(
                pttt != null ? pttt : hoaDonGoc.getPhuongThucThanhToan()   // ✅ PTTT ưu tiên từ yêu cầu đổi
        );
        newOrder.setSoTienKhachDua(daThanhToan ? chenhLech : BigDecimal.ZERO); // ✅ nếu đã thu, ghi nhận số tiền
        newOrder.setThoiGianThanhToan(daThanhToan ? LocalDateTime.now() : null);
        newOrder.setThoiGianTao(LocalDateTime.now());
        newOrder.setTienGiam(BigDecimal.ZERO);
        newOrder.setTongTien(chenhLech);                   // có thể = 0
        newOrder.setPhuongThucBanHang("Online");
        newOrder.setDiaChi(hoaDonGoc.getDiaChi());
        newOrder.setDiaChiGiaoHang(
                hoaDonGoc.getDonHang() != null ? hoaDonGoc.getDonHang().getDiaChiGiaoHang() : null
        );
        newOrder.setGhiChu("Đơn chênh lệch đổi hàng từ " + hoaDonGoc.getDonHang().getMaDonHang()
                + (ghiChuThem != null && !ghiChuThem.isBlank() ? (" - " + ghiChuThem) : ""));
        newOrder = donHangRepository.save(newOrder);

        // 2) Dòng hàng = phần chênh lệch/SL (VND làm tròn)
        BigDecimal unitPrice = (soLuongThayThe > 0)
                ? chenhLech.divide(new BigDecimal(soLuongThayThe), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        ChiTietDonHang line = new ChiTietDonHang();
        line.setDonHang(newOrder);
        line.setChiTietSanPham(ctspThayThe);
        line.setTenSanPham(ctspThayThe.getSanPham().getTenSanPham() + " (Phụ thu đổi hàng)");
        line.setSoLuong(soLuongThayThe);
        line.setGia(unitPrice);
        line.setThanhTien(unitPrice.multiply(new BigDecimal(soLuongThayThe)));
        line.setTrangThaiHoanTra(false);
        line.setGhiChu("Phụ thu chênh lệch đổi hàng từ " + hoaDonGoc.getDonHang().getMaDonHang());
        chiTietDonHangRepository.save(line);

        // 3) Hóa đơn
        createHoaDonFromDonHang(newOrder);

        // Override trạng thái hóa đơn mới theo cờ đã thanh toán
        if (daThanhToan) {
            newOrder.setTrangThaiThanhToan(true);
            newOrder.setThoiGianThanhToan(LocalDateTime.now());
            donHangRepository.save(newOrder);
        }
        return newOrder;
    }

    @Transactional
    public void xuLyChenhLechSauDuyet(HoaDon hoaDonGoc,
                                      BigDecimal chenhLech,
                                      int soLuongThayThe,
                                      UUID chiTietSanPhamThayTheId,
                                      String moTa,
                                      PhuongThucThanhToan pttt,   // ✅ mới
                                      boolean daThanhToan) {      // ✅ mới
        if (chenhLech == null) return;

        if (chenhLech.compareTo(BigDecimal.ZERO) >= 0) {
            // Phụ thu hoặc 0đ → tạo đơn/hóa đơn mới (ship=0)
            taoDonChenhLechDoiHang(hoaDonGoc, chenhLech, soLuongThayThe, chiTietSanPhamThayTheId, moTa, pttt, daThanhToan);
            return;
        }

        // chênh lệch âm → hoàn ví nếu phù hợp
        String ptttName = (pttt != null && pttt.getTenPhuongThuc() != null)
                ? pttt.getTenPhuongThuc().trim()
                : (hoaDonGoc.getPhuongThucThanhToan() != null ? hoaDonGoc.getPhuongThucThanhToan().getTenPhuongThuc().trim() : "");

        if (List.of("Ví Thanh Toán", "Ví", "Chuyển khoản").contains(ptttName)) {
            BigDecimal soTienHoan = chenhLech.abs();
            ViThanhToan vi = viThanhToanRepo.findByNguoiDung(hoaDonGoc.getNguoiDung())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ví của người dùng"));

            vi.setSoDu(vi.getSoDu().add(soTienHoan));
            vi.setThoiGianCapNhat(LocalDateTime.now());
            viThanhToanRepo.save(vi);

            LichSuGiaoDichVi ls = new LichSuGiaoDichVi();
            ls.setIdViThanhToan(vi.getId());
            ls.setLoaiGiaoDich("Hoàn tiền chênh lệch đổi hàng");
            ls.setSoTien(soTienHoan);
            ls.setMoTa(moTa != null ? moTa : ("Hoàn chênh lệch đơn " + hoaDonGoc.getDonHang().getMaDonHang()));
            ls.setCreatedAt(LocalDateTime.now());
            ls.setThoiGianGiaoDich(LocalDateTime.now());
            lichSuRepo.save(ls);
        }
    }

    public byte[] generateHoaDonPDF(String id) {
        try {
            UUID uuid = UUID.fromString(id);
            HoaDon hoaDon = hoaDonRepository.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + id));
            DonHang donHang = hoaDon.getDonHang();
            if (donHang == null || donHang.getChiTietDonHangs() == null || donHang.getChiTietDonHangs().isEmpty()) {
                throw new RuntimeException("Không có dữ liệu chi tiết đơn hàng để tạo PDF cho ID: " + id);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, baos);
            document.open();

            BaseFont bf;
            try {
                bf = BaseFont.createFont("/fonts/DejaVuSans.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception e) {
                System.err.println("Không thể tải font DejaVuSans, sử dụng font Helvetica: " + e.getMessage());
                bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED);
            }
            Font fontTitle = new Font(bf, 18, Font.BOLD, BaseColor.BLACK);
            Font fontHeader = new Font(bf, 14, Font.BOLD, BaseColor.DARK_GRAY);
            Font fontNormal = new Font(bf, 12, Font.NORMAL, BaseColor.BLACK);
            Font fontBold = new Font(bf, 12, Font.BOLD, BaseColor.BLACK);
            Font fontFooter = new Font(bf, 10, Font.ITALIC, BaseColor.GRAY);

            try {
                Image logo = Image.getInstance("src/main/resources/static/image/acv-logo.png");
                logo.scaleToFit(100, 100);
                logo.setAlignment(Element.ALIGN_CENTER);
                document.add(logo);
                document.add(Chunk.NEWLINE);
            } catch (Exception e) {
                System.err.println("Không thể tải logo: " + e.getMessage());
            }

            Paragraph storeInfo = new Paragraph();
            storeInfo.setAlignment(Element.ALIGN_CENTER);
            storeInfo.add(new Phrase("CỬA HÀNG ACV STORE\n", fontHeader));
            storeInfo.add(new Phrase("Địa chỉ: Thanh Oai, TP. Hà Nội\n", fontNormal));
            storeInfo.add(new Phrase("Hotline: 0866 716 384 | Email: datn.acv@gmail.com\n", fontNormal));
            document.add(storeInfo);
            document.add(Chunk.NEWLINE);

            Paragraph title = new Paragraph("HÓA ĐƠN BÁN HÀNG", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10f);
            document.add(title);

            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{1, 3});
            infoTable.setSpacingAfter(10f);

            addInfoCell(infoTable, fontBold, fontNormal, "Mã hóa đơn:", donHang.getMaDonHang());
            addInfoCell(infoTable, fontBold, fontNormal, "Ngày tạo:", hoaDon.getNgayTao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            addInfoCell(infoTable, fontBold, fontNormal, "Khách hàng:", hoaDon.getNguoiDung() != null ? hoaDon.getNguoiDung().getHoTen() : "Khách lẻ");
            addInfoCell(infoTable, fontBold, fontNormal, "Số điện thoại:", hoaDon.getNguoiDung() != null ? hoaDon.getNguoiDung().getSoDienThoai() : "Không rõ");

            String tenNhanVien = hoaDon.getNhanVien() != null ? hoaDon.getNhanVien().getHoTen() : "Không rõ";
            addInfoCell(infoTable, fontBold, fontNormal, "Tên nhân viên:", tenNhanVien);

            String address = "Không rõ";
            if ("Tại quầy".equalsIgnoreCase(donHang.getPhuongThucBanHang())) {
                address = hoaDon.getDiaChi() != null && hoaDon.getDiaChi().getChiTietDiaChi() != null
                        ? hoaDon.getDiaChi().getChiTietDiaChi() + ", " +
                        hoaDon.getDiaChi().getPhuongXa() + ", " +
                        hoaDon.getDiaChi().getQuanHuyen() + ", " +
                        hoaDon.getDiaChi().getTinhThanhPho()
                        : "Mua tại quầy";
            } else {
                address = hoaDon.getDiaChi() != null && hoaDon.getDiaChi().getChiTietDiaChi() != null
                        ? hoaDon.getDiaChi().getChiTietDiaChi() + ", " +
                        hoaDon.getDiaChi().getPhuongXa() + ", " +
                        hoaDon.getDiaChi().getQuanHuyen() + ", " +
                        hoaDon.getDiaChi().getTinhThanhPho()
                        : (donHang.getDiaChiGiaoHang() != null ? donHang.getDiaChiGiaoHang() : "Không rõ");
            }
            addInfoCell(infoTable, fontBold, fontNormal, "Địa chỉ:", address);

            document.add(infoTable);
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{0.5f, 1f, 2f, 1f, 1f, 0.8f, 1.5f, 1.5f});
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            addTableHeader(table, fontBold, BaseColor.LIGHT_GRAY, "STT", "Mã sản phẩm", "Sản phẩm", "Màu", "Kích cỡ", "Số lượng", "Đơn giá", "Thành tiền");

            int index = 1;
            for (ChiTietDonHang chiTiet : donHang.getChiTietDonHangs()) {
                if (chiTiet.getTrangThaiHoanTra() == null || !chiTiet.getTrangThaiHoanTra()) {
                    table.addCell(createCell(String.valueOf(index++), fontNormal, Element.ALIGN_CENTER));
                    table.addCell(createCell(chiTiet.getChiTietSanPham().getSanPham().getMaSanPham() != null ? chiTiet.getChiTietSanPham().getSanPham().getMaSanPham() : "N/A", fontNormal, Element.ALIGN_CENTER));
                    table.addCell(createCell(chiTiet.getTenSanPham(), fontNormal, Element.ALIGN_LEFT));
                    table.addCell(createCell(chiTiet.getChiTietSanPham().getMauSac().getTenMau(), fontNormal, Element.ALIGN_CENTER));
                    table.addCell(createCell(chiTiet.getChiTietSanPham().getKichCo().getTen(), fontNormal, Element.ALIGN_CENTER));
                    table.addCell(createCell(String.valueOf(chiTiet.getSoLuong()), fontNormal, Element.ALIGN_CENTER));
                    table.addCell(createCell(formatCurrency(chiTiet.getGia()), fontNormal, Element.ALIGN_RIGHT));
                    table.addCell(createCell(formatCurrency(chiTiet.getThanhTien()), fontNormal, Element.ALIGN_RIGHT));
                }
            }

            if (index == 1) {
                PdfPCell noItemsCell = new PdfPCell(new Phrase("Không có sản phẩm nào (tất cả sản phẩm đã được trả).", fontNormal));
                noItemsCell.setColspan(8);
                noItemsCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                noItemsCell.setPadding(10);
                table.addCell(noItemsCell);
            }

            document.add(table);

            BigDecimal tongTienHang = donHang.getChiTietDonHangs().stream()
                    .filter(chiTiet -> chiTiet.getTrangThaiHoanTra() == null || !chiTiet.getTrangThaiHoanTra())
                    .map(ChiTietDonHang::getThanhTien)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Tính toán giảm giá đơn hàng và giảm giá phí vận chuyển
            List<DonHangPhieuGiamGia> allVouchers = donHangPhieuGiamGiaRepository.findByDonHang_IdOrderByThoiGianApDungAsc(donHang.getId());
            BigDecimal tongGiamOrder = allVouchers.stream()
                    .filter(p -> "ORDER".equalsIgnoreCase(p.getLoaiGiamGia()))
                    .map(DonHangPhieuGiamGia::getGiaTriGiam)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal tongGiamShip = allVouchers.stream()
                    .filter(p -> "SHIPPING".equalsIgnoreCase(p.getLoaiGiamGia()))
                    .map(DonHangPhieuGiamGia::getGiaTriGiam)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalDiscountAmount = tongGiamOrder.add(tongGiamShip);
            String maPhieuGiamGia = "Không sử dụng";
            List<String> maPhieuList = new ArrayList<>();
            BigDecimal tienGiam = hoaDon.getTienGiam() != null ? hoaDon.getTienGiam() : BigDecimal.ZERO;

            if (!allVouchers.isEmpty()) {
                maPhieuList = allVouchers.stream()
                        .map(voucher -> voucher.getPhieuGiamGia().getMa())
                        .collect(Collectors.toList());
                maPhieuGiamGia = String.join(", ", maPhieuList);
            } else if (tienGiam.compareTo(BigDecimal.ZERO) > 0) {
                if (tienGiam.compareTo(new BigDecimal("100")) > 0) {
                    totalDiscountAmount = tienGiam;
                } else {
                    totalDiscountAmount = tongTienHang.multiply(tienGiam)
                            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                }
            }

            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(50);
            summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summaryTable.setSpacingBefore(10f);

            addSummaryCell(summaryTable, fontBold, fontNormal, "Tổng tiền hàng:", formatCurrency(tongTienHang));
            addSummaryCell(summaryTable, fontBold, fontNormal, "Phí vận chuyển:", formatCurrency(donHang.getPhiVanChuyen() != null ? donHang.getPhiVanChuyen() : BigDecimal.ZERO));
            addSummaryCell(summaryTable, fontBold, fontNormal, "Mã phiếu giảm giá:", maPhieuGiamGia);
            addSummaryCell(summaryTable, fontBold, fontNormal, "Giảm giá đơn hàng:", formatCurrency(tongGiamOrder));
            addSummaryCell(summaryTable, fontBold, fontNormal, "Giảm giá phí vận chuyển:", formatCurrency(tongGiamShip));
            addSummaryCell(summaryTable, fontBold, fontNormal, "Tổng giảm giá:", formatCurrency(totalDiscountAmount));
            addSummaryCell(summaryTable, fontBold, fontNormal, "Tổng tiền:", formatCurrency(hoaDon.getTongTien()));

            BigDecimal soTienKhachDua = donHang.getSoTienKhachDua() != null ? donHang.getSoTienKhachDua() : BigDecimal.ZERO;
            BigDecimal tienThoi = soTienKhachDua.compareTo(BigDecimal.ZERO) > 0 ? soTienKhachDua.subtract(hoaDon.getTongTien()) : BigDecimal.ZERO;
            addSummaryCell(summaryTable, fontBold, fontNormal, "Số tiền khách đưa:", formatCurrency(soTienKhachDua));
            addSummaryCell(summaryTable, fontBold, fontNormal, "Tiền thối lại:", formatCurrency(tienThoi));

            document.add(summaryTable);

            Paragraph footer = new Paragraph();
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(20f);
            footer.add(new Phrase("Cảm ơn quý khách đã mua sắm tại ACV Store!\nVui lòng kiểm tra kỹ thông tin hóa đơn.\n", fontFooter));
            footer.add(new Phrase("Quét mã QR dưới đây để thực hiện trả hàng:", fontNormal));
            document.add(footer);

            String returnUrl = "http://localhost:8080/acvstore/tra-hang/" + id;
            try {
                QRCodeWriter qrCodeWriter = new QRCodeWriter();
                BitMatrix bitMatrix = qrCodeWriter.encode(returnUrl, BarcodeFormat.QR_CODE, 150, 150);
                BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
                ByteArrayOutputStream qrBaos = new ByteArrayOutputStream();
                ImageIO.write(qrImage, "PNG", qrBaos);
                Image qrCode = Image.getInstance(qrBaos.toByteArray());
                qrCode.setAlignment(Element.ALIGN_CENTER);
                qrCode.scaleToFit(100, 100);
                document.add(qrCode);
            } catch (Exception e) {
                System.err.println("Không thể tạo mã QR: " + e.getMessage());
                Paragraph qrError = new Paragraph("Không thể tạo mã QR cho trả hàng.", fontFooter);
                qrError.setAlignment(Element.ALIGN_CENTER);
                document.add(qrError);
            }

            document.close();
            return baos.toByteArray();
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("ID hóa đơn không hợp lệ: " + id);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo PDF hóa đơn: " + e.getMessage());
        }
    }

    private void addInfoCell(PdfPTable table, Font fontBold, Font fontNormal, String label, String value) {
        table.addCell(createCell(label, fontBold, Element.ALIGN_LEFT));
        table.addCell(createCell(value, fontNormal, Element.ALIGN_LEFT));
    }

    private void addSummaryCell(PdfPTable table, Font fontBold, Font fontNormal, String label, String value) {
        table.addCell(createCell(label, fontBold, Element.ALIGN_LEFT));
        table.addCell(createCell(value, fontNormal, Element.ALIGN_RIGHT));
    }

    private void addTableHeader(PdfPTable table, Font font, BaseColor bgColor, String... headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(bgColor);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private PdfPCell createCell(String content, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        return cell;
    }

    private String formatCurrency(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0 VNĐ", symbols);
        df.setGroupingSize(3);
        return df.format(amount.setScale(0, RoundingMode.HALF_UP));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(
            value = {ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    public HoaDon save(HoaDon hoaDon) {
        try {
            HoaDon savedHoaDon = hoaDonRepository.saveAndFlush(hoaDon);
            diaChiNguoiDungRepository.save(hoaDon.getDiaChi());
            System.out.println("Lưu hóa đơn thành công, ID: " + savedHoaDon.getId() + ", Trạng thái: " + savedHoaDon.getTrangThai());
            return savedHoaDon;
        } catch (ObjectOptimisticLockingFailureException e) {
            System.err.println("Xung đột đồng thời khi lưu hóa đơn: " + e.getMessage());
            throw new RuntimeException("Xung đột đồng thời khi cập nhật hóa đơn. Vui lòng thử lại.");
        } catch (Exception e) {
            System.err.println("Lỗi khi lưu hóa đơn: " + e.getMessage());
            throw new RuntimeException("Lỗi khi lưu hóa đơn: " + e.getMessage());
        }
    }

    public void deleteHoaDon(UUID id) {
        HoaDon hoaDon = hoaDonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + id));
        hoaDonRepository.delete(hoaDon);
    }

    public void createHoaDonFromDonHang(DonHang donHang) {
        DonHang refreshedDonHang = donHangRepository.findById(donHang.getId())
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại với ID: " + donHang.getId()));

        HoaDon hoaDon = new HoaDon();
        hoaDon.setDonHang(refreshedDonHang);
        hoaDon.setNguoiDung(refreshedDonHang.getNguoiDung());
        hoaDon.setDiaChi(refreshedDonHang.getDiaChi()); // Liên kết với DiaChiNguoiDung của DonHang
        hoaDon.setTongTien(refreshedDonHang.getTongTien());
        hoaDon.setTienGiam(refreshedDonHang.getTienGiam() != null ? refreshedDonHang.getTienGiam() : BigDecimal.ZERO);
        hoaDon.setPhuongThucThanhToan(refreshedDonHang.getPhuongThucThanhToan());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung nhanVien = (NguoiDung) auth.getPrincipal();
            hoaDon.setNhanVien(nhanVien);
        } else {
            hoaDon.setNhanVien(null);
        }

        String trangThai;
        String ghiChu;
        if ("Ví Thanh Toán".equalsIgnoreCase(refreshedDonHang.getPhuongThucThanhToan().getTenPhuongThuc())) {
            refreshedDonHang.setTrangThaiThanhToan(true);
            refreshedDonHang.setThoiGianThanhToan(LocalDateTime.now());
            donHangRepository.save(refreshedDonHang);
            trangThai = "Đã xác nhận Online";
            ghiChu = "Thanh toán bằng ví điện tử thành công";
        } else if ("Tại quầy".equalsIgnoreCase(refreshedDonHang.getPhuongThucBanHang())) {
            trangThai = "Hoàn thành";
            ghiChu = "Hoàn thành (Tại quầy)";
            refreshedDonHang.setTrangThaiThanhToan(true);
            refreshedDonHang.setThoiGianThanhToan(LocalDateTime.now());
            donHangRepository.save(refreshedDonHang);
        } else if ("Online".equalsIgnoreCase(refreshedDonHang.getPhuongThucBanHang())) {
            trangThai = "Chưa xác nhận";
            ghiChu = "Hóa đơn Online được tạo";
        } else {
            trangThai = "Đã xác nhận";
            ghiChu = "Đã xác nhận đơn hàng Giao hàng";
        }

        hoaDon.setTrangThai(trangThai);
        hoaDon.setNgayTao(refreshedDonHang.getThoiGianTao() != null ? refreshedDonHang.getThoiGianTao() : LocalDateTime.now());
        hoaDon.setGhiChu(ghiChu);

        LichSuHoaDon lichSu = new LichSuHoaDon();
        lichSu.setHoaDon(hoaDon);
        lichSu.setTrangThai(trangThai);
        lichSu.setThoiGian(LocalDateTime.now());
        lichSu.setGhiChu(ghiChu);
        hoaDon.getLichSuHoaDons().add(lichSu);

        HoaDon savedHoaDon = hoaDonRepository.saveAndFlush(hoaDon);
    }

    public Page<HoaDon> findAll(String search, String trangThai, String paymentMethod, String salesMethod, Pageable pageable) {
        Sort sort = Sort.by(Sort.Direction.DESC, "ngayTao");
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        return hoaDonRepository.searchByKeywordAndFilters(
                search != null && !search.isEmpty() ? search : null,
                trangThai != null && !trangThai.isEmpty() ? trangThai : null,
                paymentMethod != null && !paymentMethod.isEmpty() ? paymentMethod : null,
                salesMethod != null && !salesMethod.isEmpty() ? salesMethod : null,
                sortedPageable
        );
    }

    public HoaDonDTO getHoaDonDetail(String id) {
        try {
            UUID uuid = UUID.fromString(id);
            HoaDon hoaDon = hoaDonRepository.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại."));

            String currentStatus = getCurrentStatus(hoaDon);
            if (("Đang vận chuyển".equals(currentStatus) || "Hoàn thành".equals(currentStatus) ||
                    "Đã trả hàng".equals(currentStatus) || "Đã trả hàng một phần".equals(currentStatus) ||
                    "Đã đổi hàng".equals(currentStatus)) &&
                    !hoaDon.getTrangThai().equals(currentStatus)) {
                hoaDon.setTrangThai(currentStatus);
                hoaDon.setNgayThanhToan(LocalDateTime.now());
                save(hoaDon);
            }

            HoaDonDTO dto = new HoaDonDTO();
            dto.setId(hoaDon.getId());
            dto.setMaHoaDon(hoaDon.getDonHang().getMaDonHang());
            dto.setTenKhachHang(hoaDon.getNguoiDung() != null ? hoaDon.getNguoiDung().getHoTen(): "Khách lẻ");
            dto.setSoDienThoaiKhachHang(hoaDon.getNguoiDung() != null ? hoaDon.getNguoiDung().getSoDienThoai() : "Không rõ");
            dto.setDiaChi(hoaDon.getDiaChi() != null && hoaDon.getDiaChi().getChiTietDiaChi() != null
                    ? hoaDon.getDiaChi().getChiTietDiaChi() + ", " +
                    hoaDon.getDiaChi().getPhuongXa() + ", " +
                    hoaDon.getDiaChi().getQuanHuyen() + ", " +
                    hoaDon.getDiaChi().getTinhThanhPho()
                    : hoaDon.getGhiChu() != null ? hoaDon.getGhiChu() : "Không rõ");
            dto.setTongTienHang(hoaDon.getTongTien().add(hoaDon.getTienGiam() != null ? hoaDon.getTienGiam() : BigDecimal.ZERO));
            dto.setTongTien(hoaDon.getTongTien());
            dto.setTienGiam(hoaDon.getTienGiam() != null ? hoaDon.getTienGiam() : BigDecimal.ZERO);
            dto.setPhiVanChuyen(hoaDon.getDonHang().getPhiVanChuyen() != null ? hoaDon.getDonHang().getPhiVanChuyen() : BigDecimal.ZERO);
            dto.setPhuongThucThanhToan(hoaDon.getPhuongThucThanhToan() != null ? hoaDon.getPhuongThucThanhToan().getTenPhuongThuc() : "Chưa chọn");
            dto.setTrangThaiThanhToan(currentStatus); // Sử dụng currentStatus thay vì hoaDon.getTrangThai()
            dto.setThoiGianTao(hoaDon.getNgayTao());
            dto.setNgayThanhToan(hoaDon.getNgayThanhToan());
            dto.setGhiChu(hoaDon.getGhiChu());
            dto.setPhuongThucBanHang(hoaDon.getDonHang().getPhuongThucBanHang());

            List<GioHangItemDTO> danhSachSanPham = new ArrayList<>();
            DonHang donHang = donHangRepository.findByMaDonHang(hoaDon.getDonHang().getMaDonHang())
                    .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại."));
            for (ChiTietDonHang chiTiet : donHang.getChiTietDonHangs()) {
                GioHangItemDTO item = new GioHangItemDTO();
                item.setIdChiTietSanPham(chiTiet.getChiTietSanPham().getId());
                item.setTenSanPham(chiTiet.getTenSanPham());
                item.setMauSac(chiTiet.getChiTietSanPham().getMauSac().getTenMau());
                item.setKichCo(chiTiet.getChiTietSanPham().getKichCo().getTen());
                item.setSoLuong(chiTiet.getSoLuong());
                item.setGia(chiTiet.getGia());
                item.setThanhTien(chiTiet.getThanhTien());
                item.setGhiChu(chiTiet.getGhiChu());
                item.setTrangThaiHoanTra(chiTiet.getTrangThaiHoanTra() != null ? chiTiet.getTrangThaiHoanTra() : false);
                danhSachSanPham.add(item);
            }
            dto.setDanhSachSanPham(danhSachSanPham);

            List<HoaDonDTO.LichSuDTO> lichSuDTOs = new ArrayList<>();
            for (LichSuHoaDon lichSu : hoaDon.getLichSuHoaDons()) {
                HoaDonDTO.LichSuDTO lichSuDTO = new HoaDonDTO.LichSuDTO();
                lichSuDTO.setThoiGian(lichSu.getThoiGian());
                lichSuDTO.setTrangThai(lichSu.getTrangThai());
                lichSuDTO.setGhiChu(lichSu.getGhiChu());
                lichSuDTOs.add(lichSuDTO);
            }
            dto.setLichSuHoaDons(lichSuDTOs);

            return dto;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("ID hóa đơn không hợp lệ: " + id);
        }
    }

    public byte[] taoHoaDon(String maDonHang) {
        DonHang donHang = donHangRepository.findByMaDonHang(maDonHang)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại."));
        HoaDon hoaDon = hoaDonRepository.findByDonHang(donHang)
                .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại."));
        return generateHoaDonPDF(hoaDon.getId().toString());
    }

    public Optional<HoaDon> findById(UUID id) {
        return hoaDonRepository.findById(id);
    }

    public void addLichSuHoaDon(HoaDon hoaDon, String trangThai, String ghiChu) {
        LichSuHoaDon lichSu = new LichSuHoaDon();
        lichSu.setHoaDon(hoaDon);
        lichSu.setTrangThai(trangThai);
        lichSu.setThoiGian(LocalDateTime.now());
        lichSu.setGhiChu(ghiChu);
        lichSuHoaDonRepository.save(lichSu);
        hoaDon.getLichSuHoaDons().add(lichSu);
        System.out.println("Đã thêm LichSuHoaDon: " + trangThai);
    }

    @Transactional
    public HoaDon taoHoaDonPhuThuNhieuDong(HoaDon hoaDonGoc, List<LichSuDoiSanPham> items) {
        if (items == null || items.isEmpty()) return null;

        // Group theo PTTT: nếu item không có PTTT riêng -> fallback PTTT của HĐ gốc
        Map<UUID, List<LichSuDoiSanPham>> groups = new LinkedHashMap<>();
        Map<UUID, PhuongThucThanhToan> groupPttt = new HashMap<>();

        for (LichSuDoiSanPham ls : items) {
            PhuongThucThanhToan pttt = (ls.getPhuongThucThanhToan() != null)
                    ? ls.getPhuongThucThanhToan()
                    : hoaDonGoc.getPhuongThucThanhToan();
            UUID key = (pttt != null && pttt.getId() != null) ? pttt.getId() : UUID.randomUUID(); // tránh null key
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(ls);
            groupPttt.putIfAbsent(key, pttt);
        }

        HoaDon hoaDonKetQuaCuoi = null;

        // Duyệt từng nhóm theo PTTT
        for (Map.Entry<UUID, List<LichSuDoiSanPham>> entry : groups.entrySet()) {
            PhuongThucThanhToan ptttGroup = groupPttt.get(entry.getKey());
            List<LichSuDoiSanPham> groupItems = entry.getValue();

            // Kiểm tra nhóm đã thanh toán hết chưa (chỉ khi mọi item trong group = true)
            boolean daThanhToanGroup = groupItems.stream()
                    .allMatch(ls -> Boolean.TRUE.equals(ls.getDaThanhToanChenhLech()));

            // Gom trước các dòng sẽ ghi vào ĐƠN PHỤ THU (≥ 0) để tránh tạo đơn rỗng
            class LineSpec {
                ChiTietSanPham ctThayThe;
                String ten;
                int soLuong;
                BigDecimal unitPrice;
                BigDecimal thanhTien;
                String ghiChu;
            }
            List<LineSpec> lineSpecs = new ArrayList<>();
            BigDecimal tongPhuThu = BigDecimal.ZERO;

            // Xử lý từng yêu cầu trong group
            for (LichSuDoiSanPham ls : groupItems) {
                ChiTietDonHang ctGoc = ls.getChiTietDonHang();
                ChiTietSanPham ctThayThe = ls.getChiTietSanPhamThayThe();

                // Tính chênh lệch: ưu tiên giá trị đã lưu trong Lịch sử; nếu null thì tự tính
                BigDecimal chenhLechItem =
                        (ls.getChenhLechGia() != null)
                                ? ls.getChenhLechGia()
                                : ctThayThe.getGia().multiply(BigDecimal.valueOf(ls.getSoLuong()))
                                .subtract(
                                        (ls.getTongTienHoan() != null)
                                                ? ls.getTongTienHoan()
                                                : ctGoc.getGia().multiply(BigDecimal.valueOf(ls.getSoLuong()))
                                );

                if (chenhLechItem.compareTo(BigDecimal.ZERO) >= 0) {
                    // ✅ Tạo dòng chi tiết cho cả phụ thu (>0) LẪN 0Đ (=0)
                    BigDecimal unitDiff = chenhLechItem
                            .divide(BigDecimal.valueOf(ls.getSoLuong()), 0, RoundingMode.HALF_UP);
                    LineSpec spec = new LineSpec();
                    spec.ctThayThe = ctThayThe;
                    spec.ten = ctThayThe.getSanPham().getTenSanPham() +
                            (chenhLechItem.signum() == 0 ? " (Đổi hàng 0đ)" : " (Phụ thu đổi hàng)");
                    spec.soLuong = ls.getSoLuong();
                    spec.unitPrice = unitDiff; // = 0 nếu chênh lệch 0
                    spec.thanhTien = unitDiff.multiply(BigDecimal.valueOf(ls.getSoLuong())); // = 0 nếu chênh lệch 0
                    spec.ghiChu = "Đổi từ '" + ctGoc.getTenSanPham() + "' → '" +
                            ctThayThe.getSanPham().getTenSanPham() + "', SL " + ls.getSoLuong() +
                            (chenhLechItem.signum() == 0 ? ", chênh lệch 0đ" : "");

                    lineSpecs.add(spec);
                    tongPhuThu = tongPhuThu.add(spec.thanhTien);

                } else {
                    // Âm -> hoàn ví (nếu PTTT nhóm là ví/CK)
                    String ptttName = (ptttGroup != null && ptttGroup.getTenPhuongThuc() != null)
                            ? ptttGroup.getTenPhuongThuc().trim()
                            : "";
                    if (List.of("Ví Thanh Toán", "Ví", "Chuyển khoản").contains(ptttName)) {
                        BigDecimal soTienHoan = chenhLechItem.abs();
                        ViThanhToan vi = viThanhToanRepo.findByNguoiDung(hoaDonGoc.getNguoiDung())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví của người dùng"));

                        vi.setSoDu(vi.getSoDu().add(soTienHoan));
                        vi.setThoiGianCapNhat(LocalDateTime.now());
                        viThanhToanRepo.save(vi);

                        LichSuGiaoDichVi lsVi = new LichSuGiaoDichVi();
                        lsVi.setIdViThanhToan(vi.getId());
                        lsVi.setLoaiGiaoDich("Hoàn tiền chênh lệch đổi hàng");
                        lsVi.setSoTien(soTienHoan);
                        lsVi.setMoTa("Hoàn chênh lệch đơn " + hoaDonGoc.getDonHang().getMaDonHang());
                        lsVi.setCreatedAt(LocalDateTime.now());
                        lsVi.setThoiGianGiaoDich(LocalDateTime.now());
                        lichSuRepo.save(lsVi);
                    }
                }
            }

            // Nếu không có dòng ≥ 0 (chỉ toàn âm → chỉ hoàn ví) thì bỏ qua không tạo đơn/hóa đơn
            if (lineSpecs.isEmpty()) {
                continue;
            }

            // 1) Tạo DonHang mới cho group
            DonHang newOrder = new DonHang();
            newOrder.setNguoiDung(hoaDonGoc.getNguoiDung());
            newOrder.setMaDonHang("EX" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            newOrder.setTrangThaiThanhToan(false);
            newOrder.setPhiVanChuyen(BigDecimal.ZERO);
            newOrder.setPhuongThucThanhToan(ptttGroup);
            newOrder.setSoTienKhachDua(BigDecimal.ZERO);
            newOrder.setThoiGianTao(LocalDateTime.now());
            newOrder.setTienGiam(BigDecimal.ZERO);
            newOrder.setTongTien(BigDecimal.ZERO);
            newOrder.setPhuongThucBanHang("Online");
            newOrder.setDiaChi(hoaDonGoc.getDiaChi());
            newOrder.setDiaChiGiaoHang(
                    hoaDonGoc.getDonHang() != null ? hoaDonGoc.getDonHang().getDiaChiGiaoHang() : null
            );
            newOrder.setGhiChu("Đơn chênh lệch đổi hàng (gộp) từ " + hoaDonGoc.getDonHang().getMaDonHang());
            newOrder = donHangRepository.save(newOrder);

            // 2) Ghi các dòng phụ thu/0đ
            for (LineSpec s : lineSpecs) {
                ChiTietDonHang line = new ChiTietDonHang();
                line.setDonHang(newOrder);
                line.setChiTietSanPham(s.ctThayThe);
                line.setTenSanPham(s.ten);
                line.setSoLuong(s.soLuong);
                line.setGia(s.unitPrice);         // 0 nếu chênh lệch 0
                line.setThanhTien(s.thanhTien);   // 0 nếu chênh lệch 0
                line.setTrangThaiHoanTra(false);
                line.setGhiChu(s.ghiChu);
                chiTietDonHangRepository.save(line);
            }

            // 3) Cập nhật tổng tiền + trạng thái đơn theo đã thanh toán
            newOrder.setTongTien(tongPhuThu);
            if (daThanhToanGroup) {
                newOrder.setTrangThaiThanhToan(true);
                newOrder.setSoTienKhachDua(tongPhuThu);
                newOrder.setThoiGianThanhToan(LocalDateTime.now());
            }
            donHangRepository.save(newOrder);

            // 4) Tạo hóa đơn cho đơn mới
            createHoaDonFromDonHang(newOrder);

            // 5) Lấy hóa đơn vừa tạo (để trả về)
            hoaDonKetQuaCuoi = hoaDonRepository.findByDonHang(newOrder).orElse(null);
        }

        return hoaDonKetQuaCuoi;
    }


    public String getCurrentStatus(HoaDon hoaDon) {
        // Kiểm tra trạng thái hủy đơn hàng
        if ("Hủy đơn hàng".equals(hoaDon.getTrangThai())) {
            return "Hủy đơn hàng";
        }

        // Kiểm tra trạng thái đổi hàng dựa trên trạng thái hóa đơn hoặc lịch sử đổi hàng
        if ("Đã đổi hàng".equals(hoaDon.getTrangThai()) ||
                hoaDon.getLichSuHoaDons().stream().anyMatch(ls -> "Đã đổi hàng".equals(ls.getTrangThai())) ||
                lichSuDoiSanPhamRepository.existsByHoaDonIdAndTrangThai(hoaDon.getId(), "Đã xác nhận")) {
            return "Đã đổi hàng";
        }

        // Kiểm tra trạng thái chờ xử lý đổi hàng
        if ("Chờ xử lý đổi hàng".equals(hoaDon.getTrangThai()) ||
                hoaDon.getLichSuHoaDons().stream().anyMatch(ls -> "Chờ xử lý đổi hàng".equals(ls.getTrangThai())) ||
                lichSuDoiSanPhamRepository.existsByHoaDonIdAndTrangThai(hoaDon.getId(), "Chờ xử lý")) {
            return "Chờ xử lý đổi hàng";
        }

        // Kiểm tra trạng thái hoàn thành
        if ("Hoàn thành".equals(hoaDon.getTrangThai()) ||
                hoaDon.getLichSuHoaDons().stream().anyMatch(ls -> "Hoàn thành".equals(ls.getTrangThai()))) {
            return "Hoàn thành";
        }

        // Kiểm tra trạng thái trả hàng
        List<ChiTietDonHang> chiTietDonHangs = hoaDon.getDonHang().getChiTietDonHangs();
        long totalItems = chiTietDonHangs.size();
        long returnedItems = chiTietDonHangs.stream()
                .filter(item -> Boolean.TRUE.equals(item.getTrangThaiHoanTra()))
                .count();

        if (returnedItems > 0 && returnedItems == totalItems) {
            return "Đã trả hàng";
        } else if (returnedItems > 0) {
            return "Đã trả hàng một phần";
        }

        // Kiểm tra phương thức bán hàng tại quầy
        if ("Tại quầy".equalsIgnoreCase(hoaDon.getDonHang().getPhuongThucBanHang())) {
            return "Hoàn thành";
        }

        // Kiểm tra trạng thái khác dựa trên lịch sử hóa đơn
        if (hoaDon.getLichSuHoaDons().stream().anyMatch(ls -> "Vận chuyển thành công".equals(ls.getTrangThai()))) {
            return "Vận chuyển thành công";
        } else if (hoaDon.getLichSuHoaDons().stream().anyMatch(ls -> "Đang vận chuyển".equals(ls.getTrangThai()))) {
            return "Đang vận chuyển";
        } else if (hoaDon.getLichSuHoaDons().stream().anyMatch(ls -> "Đang xử lý Online".equals(ls.getTrangThai()))) {
            return "Đang xử lý Online";
        } else if (hoaDon.getLichSuHoaDons().stream().anyMatch(ls -> "Đã xác nhận Online".equals(ls.getTrangThai()))) {
            return "Đã xác nhận Online";
        } else if (hoaDon.getLichSuHoaDons().stream().anyMatch(ls -> "Đã xác nhận".equals(ls.getTrangThai()))) {
            return "Đã xác nhận";
        }

        return "Chưa xác nhận";
    }

    @Transactional
    public void processExchange(UUID hoaDonId, List<UUID> chiTietDonHangIds, List<UUID> newChiTietSanPhamIds, String lyDoDoiHang) {
        HoaDon hoaDon = hoaDonRepository.findById(hoaDonId)
                .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + hoaDonId));

        if ("Đã đổi hàng".equals(hoaDon.getTrangThai())
                || lichSuDoiSanPhamRepository.existsByHoaDonIdAndTrangThai(hoaDonId, "Đã xác nhận")) {
            throw new IllegalStateException("Hóa đơn này đã đổi hàng, không thể tạo yêu cầu đổi thêm.");
        }

        // Kiểm tra trạng thái hợp lệ để đổi hàng
        if (!List.of("Hoàn thành", "Vận chuyển thành công", "Đã trả hàng một phần").contains(hoaDon.getTrangThai())) {
            throw new IllegalStateException("Hóa đơn phải ở trạng thái 'Hoàn thành', 'Vận chuyển thành công' hoặc 'Đã trả hàng một phần' để thực hiện đổi hàng.");
        }

        if (chiTietDonHangIds == null || chiTietDonHangIds.isEmpty() || newChiTietSanPhamIds == null || newChiTietSanPhamIds.isEmpty()) {
            throw new IllegalArgumentException("Danh sách sản phẩm trả hoặc sản phẩm mới không hợp lệ.");
        }

        if (chiTietDonHangIds.size() != newChiTietSanPhamIds.size()) {
            throw new IllegalArgumentException("Số lượng sản phẩm trả phải bằng số lượng sản phẩm đổi mới.");
        }

        BigDecimal tongTienHoan = BigDecimal.ZERO;
        BigDecimal tongTienMoi = BigDecimal.ZERO;

        try {
            // Xử lý sản phẩm trả
            for (UUID chiTietId : chiTietDonHangIds) {
                ChiTietDonHang chiTiet = chiTietDonHangRepository.findById(chiTietId)
                        .orElseThrow(() -> new RuntimeException("Chi tiết đơn hàng không tồn tại với ID: " + chiTietId));

                if (Boolean.TRUE.equals(chiTiet.getTrangThaiHoanTra())) {
                    throw new RuntimeException("Sản phẩm đã được trả trước đó: " + chiTiet.getTenSanPham());
                }

                chiTiet.setTrangThaiHoanTra(true);
                chiTiet.setLyDoTraHang(lyDoDoiHang);
                chiTietDonHangRepository.save(chiTiet);

                ChiTietSanPham chiTietSanPham = chiTietSanPhamRepository.findById(chiTiet.getChiTietSanPham().getId())
                        .orElseThrow(() -> new RuntimeException("Chi tiết sản phẩm không tồn tại."));
                int soLuongTra = chiTiet.getSoLuong();
                chiTietSanPham.setSoLuongTonKho(chiTietSanPham.getSoLuongTonKho() + soLuongTra);
                chiTietSanPhamRepository.save(chiTietSanPham);

                LichSuTraHang lichSu = new LichSuTraHang();
                lichSu.setHoaDon(hoaDon);
                lichSu.setChiTietDonHang(chiTiet);
                lichSu.setSoLuong(chiTiet.getSoLuong());
                lichSu.setTongTienHoan(chiTiet.getThanhTien());
                lichSu.setLyDoTraHang(lyDoDoiHang);
                lichSu.setThoiGianTra(LocalDateTime.now());
                lichSu.setTrangThai("Đã trả");
                lichSuTraHangRepository.save(lichSu);

                tongTienHoan = tongTienHoan.add(chiTiet.getThanhTien());
            }

            // Thêm sản phẩm mới
            List<ChiTietDonHang> newChiTietDonHangs = new ArrayList<>();
            for (UUID newChiTietSanPhamId : newChiTietSanPhamIds) {
                ChiTietSanPham newChiTietSanPham = chiTietSanPhamRepository.findById(newChiTietSanPhamId)
                        .orElseThrow(() -> new RuntimeException("Chi tiết sản phẩm mới không tồn tại với ID: " + newChiTietSanPhamId));

                if (newChiTietSanPham.getSoLuongTonKho() <= 0) {
                    throw new RuntimeException("Sản phẩm mới không còn hàng trong kho: " + newChiTietSanPham.getSanPham().getTenSanPham());
                }

                // Giả sử mỗi sản phẩm đổi có số lượng là 1 (có thể điều chỉnh theo yêu cầu)
                int soLuongMoi = 1;
                newChiTietSanPham.setSoLuongTonKho(newChiTietSanPham.getSoLuongTonKho() - soLuongMoi);
                chiTietSanPhamRepository.save(newChiTietSanPham);

                ChiTietDonHang newChiTiet = new ChiTietDonHang();
                newChiTiet.setDonHang(hoaDon.getDonHang());
                newChiTiet.setChiTietSanPham(newChiTietSanPham);
                newChiTiet.setTenSanPham(newChiTietSanPham.getSanPham().getTenSanPham());
                newChiTiet.setSoLuong(soLuongMoi);
                newChiTiet.setGia(newChiTietSanPham.getGia());
                newChiTiet.setThanhTien(newChiTietSanPham.getGia().multiply(new BigDecimal(soLuongMoi)));
                newChiTiet.setTrangThaiHoanTra(false);
                chiTietDonHangRepository.save(newChiTiet);

                newChiTietDonHangs.add(newChiTiet);
                tongTienMoi = tongTienMoi.add(newChiTiet.getThanhTien());
            }

            // Cập nhật tổng tiền hóa đơn
            hoaDon.setTongTien(hoaDon.getTongTien().subtract(tongTienHoan).add(tongTienMoi));

            // Xác định trạng thái mới
            String trangThaiMoi = "Đã đổi hàng";
            String ghiChu = "Đổi hàng: " + lyDoDoiHang + ". Tổng tiền trả: " + formatCurrency(tongTienHoan) + ". Tổng tiền sản phẩm mới: " + formatCurrency(tongTienMoi);

            // Cập nhật trạng thái hóa đơn
            updateStatus(hoaDonId, trangThaiMoi, ghiChu, true);

            // Nếu có chênh lệch giá, xử lý hoàn tiền hoặc thu thêm
            BigDecimal chenhLech = tongTienMoi.subtract(tongTienHoan);
            if (chenhLech.compareTo(BigDecimal.ZERO) != 0 && hoaDon.getPhuongThucThanhToan() != null &&
                    List.of("Ví Thanh Toán", "Ví", "Chuyển khoản").contains(hoaDon.getPhuongThucThanhToan().getTenPhuongThuc().trim())) {
                NguoiDung nguoiDung = hoaDon.getNguoiDung();
                ViThanhToan viThanhToan = viThanhToanRepo.findByNguoiDung(nguoiDung)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy ví của người dùng"));

                String moTaGiaoDich;
                if (chenhLech.compareTo(BigDecimal.ZERO) > 0) {
                    // Khách cần trả thêm
                    if (viThanhToan.getSoDu().compareTo(chenhLech) < 0) {
                        throw new RuntimeException("Số dư ví không đủ để thanh toán chênh lệch: " + formatCurrency(chenhLech));
                    }
                    viThanhToan.setSoDu(viThanhToan.getSoDu().subtract(chenhLech));
                    moTaGiaoDich = "Thanh toán chênh lệch đổi hàng cho hóa đơn: " + hoaDon.getDonHang().getMaDonHang();
                } else {
                    // Hoàn tiền cho khách
                    viThanhToan.setSoDu(viThanhToan.getSoDu().add(chenhLech.abs()));
                    moTaGiaoDich = "Hoàn tiền chênh lệch đổi hàng cho hóa đơn: " + hoaDon.getDonHang().getMaDonHang();
                }

                viThanhToan.setThoiGianCapNhat(LocalDateTime.now());
                viThanhToanRepo.save(viThanhToan);

                LichSuGiaoDichVi lichSu = new LichSuGiaoDichVi();
                lichSu.setIdViThanhToan(viThanhToan.getId());
                lichSu.setLoaiGiaoDich(chenhLech.compareTo(BigDecimal.ZERO) > 0 ? "Thanh toán chênh lệch" : "Hoàn tiền chênh lệch");
                lichSu.setSoTien(chenhLech.abs());
                lichSu.setMoTa(moTaGiaoDich);
                lichSu.setCreatedAt(LocalDateTime.now());
                lichSu.setThoiGianGiaoDich(LocalDateTime.now());
                lichSuRepo.save(lichSu);
            }
        } catch (Exception e) {
            System.err.println("Lỗi trong processExchange: " + e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void cancelOrder(UUID hoaDonId, String ghiChu) {
        HoaDon hoaDon = hoaDonRepository.findById(hoaDonId)
                .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + hoaDonId));

        String currentStatus = hoaDon.getTrangThai();
        if (!List.of("Chưa xác nhận", "Đã xác nhận", "Đã xác nhận Online", "Đang xử lý Online", "Đang vận chuyển").contains(currentStatus)) {
            throw new IllegalStateException("Hóa đơn không thể hủy khi ở trạng thái: " + currentStatus);
        }

        String pttt = hoaDon.getPhuongThucThanhToan() != null
                ? hoaDon.getPhuongThucThanhToan().getTenPhuongThuc().trim()
                : "";

        if ("Ví Thanh Toán".equalsIgnoreCase(pttt)
                || "Ví".equalsIgnoreCase(pttt)
                || "Chuyển khoản".equalsIgnoreCase(pttt)) {
            BigDecimal refundAmount = hoaDon.getTongTien();
            NguoiDung nguoiDung = hoaDon.getNguoiDung();
            ViThanhToan viThanhToan = viThanhToanRepo.findByNguoiDung(nguoiDung)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ví của người dùng"));

            viThanhToan.setSoDu(viThanhToan.getSoDu().add(refundAmount));
            viThanhToan.setThoiGianCapNhat(LocalDateTime.now());
            viThanhToanRepo.save(viThanhToan);

            LichSuGiaoDichVi lichSu = new LichSuGiaoDichVi();
            lichSu.setIdViThanhToan(viThanhToan.getId());
            lichSu.setLoaiGiaoDich("Hoàn tiền");
            lichSu.setSoTien(refundAmount);
            lichSu.setMoTa("Hoàn tiền do hủy đơn hàng: " + hoaDon.getDonHang().getMaDonHang());
            lichSu.setCreatedAt(LocalDateTime.now());
            lichSu.setThoiGianGiaoDich(LocalDateTime.now());
            lichSuRepo.save(lichSu);
        }

        updateStatus(hoaDonId, "Hủy đơn hàng", ghiChu, true);

        for (ChiTietDonHang chiTiet : hoaDon.getDonHang().getChiTietDonHangs()) {
            chiTietSanPhamRepository.updateStock(chiTiet.getChiTietSanPham().getId(), chiTiet.getSoLuong());
        }
    }

    @Transactional
    public void processReturn(UUID hoaDonId, List<UUID> chiTietDonHangIds, String lyDoTraHang) {
        HoaDon hoaDon = hoaDonRepository.findById(hoaDonId)
                .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + hoaDonId));
        if (!"Hoàn thành".equals(hoaDon.getTrangThai()) && !"Vận chuyển thành công".equals(hoaDon.getTrangThai())
                && !"Đã trả hàng một phần".equals(hoaDon.getTrangThai())) {
            throw new IllegalStateException("Hóa đơn phải ở trạng thái 'Hoàn thành', 'Vận chuyển thành công' hoặc 'Đã trả hàng một phần' để thực hiện trả hàng.");
        }

        if (chiTietDonHangIds == null || chiTietDonHangIds.isEmpty()) {
            throw new IllegalArgumentException("Danh sách sản phẩm trả không hợp lệ.");
        }

        BigDecimal tongTienHangTra = BigDecimal.ZERO; // Tổng tiền hàng trả (giá gốc)
        BigDecimal tongTienGocHoaDon = BigDecimal.ZERO; // Tổng tiền hàng gốc của hóa đơn

        try {
            // Tính tổng tiền hàng gốc của hóa đơn (để tính tỷ lệ)
            for (ChiTietDonHang chiTiet : hoaDon.getDonHang().getChiTietDonHangs()) {
                tongTienGocHoaDon = tongTienGocHoaDon.add(chiTiet.getThanhTien());
            }

            for (UUID chiTietId : chiTietDonHangIds) {
                ChiTietDonHang chiTiet = chiTietDonHangRepository.findById(chiTietId)
                        .orElseThrow(() -> new RuntimeException("Chi tiết đơn hàng không tồn tại với ID: " + chiTietId));

                if (Boolean.TRUE.equals(chiTiet.getTrangThaiHoanTra())) {
                    throw new RuntimeException("Sản phẩm đã được trả trước đó: " + chiTiet.getTenSanPham());
                }

                chiTiet.setTrangThaiHoanTra(true);
                chiTiet.setLyDoTraHang(lyDoTraHang);
                chiTietDonHangRepository.save(chiTiet);

                ChiTietSanPham chiTietSanPham = chiTietSanPhamRepository.findById(chiTiet.getChiTietSanPham().getId())
                        .orElseThrow(() -> new RuntimeException("Chi tiết sản phẩm không tồn tại."));
                int soLuongTra = chiTiet.getSoLuong();
                chiTietSanPham.setSoLuongTonKho(chiTietSanPham.getSoLuongTonKho() + soLuongTra);
                chiTietSanPhamRepository.save(chiTietSanPham);

                // Cộng dồn tổng tiền hàng trả
                tongTienHangTra = tongTienHangTra.add(chiTiet.getThanhTien());

                LichSuTraHang lichSu = new LichSuTraHang();
                lichSu.setHoaDon(hoaDon);
                lichSu.setChiTietDonHang(chiTiet);
                lichSu.setSoLuong(chiTiet.getSoLuong());
                lichSu.setTongTienHoan(chiTiet.getThanhTien());
                lichSu.setLyDoTraHang(lyDoTraHang);
                lichSu.setThoiGianTra(LocalDateTime.now());
                lichSu.setTrangThai("Đã trả");
                lichSuTraHangRepository.save(lichSu);
            }

            // Tính tỷ lệ hoàn trả
            BigDecimal tyLeHoanTra = BigDecimal.ZERO;
            if (tongTienGocHoaDon.compareTo(BigDecimal.ZERO) > 0) {
                tyLeHoanTra = tongTienHangTra.divide(tongTienGocHoaDon, 4, RoundingMode.HALF_UP);
            }

            // Tính giảm giá tương ứng với sản phẩm hoàn trả
            BigDecimal tienGiamHoaDon = hoaDon.getTienGiam() != null ? hoaDon.getTienGiam() : BigDecimal.ZERO;
            BigDecimal giamGiaHoanTra = tienGiamHoaDon.multiply(tyLeHoanTra).setScale(0, RoundingMode.HALF_UP);

            // Tổng tiền hoàn thực tế = Tiền hàng trả - Giảm giá tương ứng
            BigDecimal tongTienHoanThucTe = tongTienHangTra.subtract(giamGiaHoanTra);

            // Đảm bảo không âm
            if (tongTienHoanThucTe.compareTo(BigDecimal.ZERO) < 0) {
                tongTienHoanThucTe = BigDecimal.ZERO;
            }

            List<ChiTietDonHang> chiTietDonHangs = hoaDon.getDonHang().getChiTietDonHangs();
            long totalItems = chiTietDonHangs.size();
            long returnedItems = chiTietDonHangs.stream()
                    .filter(item -> Boolean.TRUE.equals(item.getTrangThaiHoanTra()))
                    .count();

            // Cập nhật tổng tiền hóa đơn (trừ đi tiền hoàn thực tế)
            hoaDon.setTongTien(hoaDon.getTongTien().subtract(tongTienHoanThucTe));

            String trangThaiTraHang = returnedItems == totalItems ? "Đã trả hàng" : "Đã trả hàng một phần";
            String ghiChu = "Lý do trả hàng: " + lyDoTraHang +
                    ". Tổng tiền hàng trả: " + formatCurrency(tongTienHangTra) +
                    ". Giảm giá tương ứng: " + formatCurrency(giamGiaHoanTra) +
                    ". Tổng tiền hoàn trả thực tế: " + formatCurrency(tongTienHoanThucTe);

            updateStatus(hoaDonId, trangThaiTraHang, ghiChu, true);

            // Xử lý hoàn tiền vào ví (nếu có)
            if (tongTienHoanThucTe.compareTo(BigDecimal.ZERO) > 0) {
                String pttt = hoaDon.getPhuongThucThanhToan() != null
                        ? hoaDon.getPhuongThucThanhToan().getTenPhuongThuc().trim()
                        : "";

                if (List.of("Ví Thanh Toán", "Ví", "Chuyển khoản").contains(pttt)) {
                    NguoiDung nguoiDung = hoaDon.getNguoiDung();
                    if (nguoiDung != null) {
                        ViThanhToan viThanhToan = viThanhToanRepo.findByNguoiDung(nguoiDung)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví của người dùng"));

                        viThanhToan.setSoDu(viThanhToan.getSoDu().add(tongTienHoanThucTe));
                        viThanhToan.setThoiGianCapNhat(LocalDateTime.now());
                        viThanhToanRepo.save(viThanhToan);

                        LichSuGiaoDichVi lichSu = new LichSuGiaoDichVi();
                        lichSu.setIdViThanhToan(viThanhToan.getId());
                        lichSu.setLoaiGiaoDich("Hoàn tiền");
                        lichSu.setSoTien(tongTienHoanThucTe);
                        lichSu.setMoTa("Hoàn tiền trả hàng cho hóa đơn: " + hoaDon.getDonHang().getMaDonHang());
                        lichSu.setCreatedAt(LocalDateTime.now());
                        lichSu.setThoiGianGiaoDich(LocalDateTime.now());
                        lichSuRepo.save(lichSu);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Lỗi trong processReturn: " + e.getMessage());
            throw e; // Ném lại ngoại lệ để rollback giao dịch
        }
    }

    @Transactional
    public void processReturnKH(UUID hoaDonId, List<UUID> chiTietDonHangIds, String lyDoTraHang) {
        HoaDon hoaDon = hoaDonRepository.findById(hoaDonId)
                .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + hoaDonId));
        if (!List.of("Hoàn thành", "Vận chuyển thành công", "Đã trả hàng một phần").contains(hoaDon.getTrangThai())) {
            throw new IllegalStateException("Hóa đơn phải ở trạng thái 'Hoàn thành', 'Vận chuyển thành công' hoặc 'Đã trả hàng một phần' để thực hiện trả hàng.");
        }

        if (chiTietDonHangIds == null || chiTietDonHangIds.isEmpty()) {
            throw new IllegalArgumentException("Danh sách sản phẩm trả không hợp lệ.");
        }

        BigDecimal tongTienHangTra = BigDecimal.ZERO;
        BigDecimal tongTienGocHoaDon = BigDecimal.ZERO;

        try {
            // Tính tổng tiền hàng gốc của hóa đơn
            for (ChiTietDonHang chiTiet : hoaDon.getDonHang().getChiTietDonHangs()) {
                tongTienGocHoaDon = tongTienGocHoaDon.add(chiTiet.getThanhTien());
            }

            for (UUID chiTietId : chiTietDonHangIds) {
                ChiTietDonHang chiTiet = chiTietDonHangRepository.findById(chiTietId)
                        .orElseThrow(() -> new RuntimeException("Chi tiết đơn hàng không tồn tại với ID: " + chiTietId));

                if (Boolean.TRUE.equals(chiTiet.getTrangThaiHoanTra())) {
                    throw new RuntimeException("Sản phẩm đã được trả trước đó: " + chiTiet.getTenSanPham());
                }

                chiTiet.setTrangThaiHoanTra(true);
                chiTiet.setLyDoTraHang(lyDoTraHang);
                chiTietDonHangRepository.save(chiTiet);

//                ChiTietSanPham chiTietSanPham = chiTietSanPhamRepository.findById(chiTiet.getChiTietSanPham().getId())
//                        .orElseThrow(() -> new RuntimeException("Chi tiết sản phẩm không tồn tại."));
//                int soLuongTra = chiTiet.getSoLuong();
//                chiTietSanPham.setSoLuongTonKho(chiTietSanPham.getSoLuongTonKho() + soLuongTra);
//                chiTietSanPhamRepository.save(chiTietSanPham);

                // Tạo yêu cầu trả hàng với trạng thái "Chờ xác nhận"
                LichSuTraHang lichSu = new LichSuTraHang();
                lichSu.setHoaDon(hoaDon);
                lichSu.setChiTietDonHang(chiTiet);
                lichSu.setSoLuong(chiTiet.getSoLuong());
                lichSu.setTongTienHoan(chiTiet.getThanhTien());
                lichSu.setLyDoTraHang(lyDoTraHang);
                lichSu.setThoiGianTra(LocalDateTime.now());
                lichSu.setTrangThai("Chờ xác nhận");
                lichSuTraHangRepository.save(lichSu);

                tongTienHangTra = tongTienHangTra.add(chiTiet.getThanhTien());
            }

            // Tính tỷ lệ hoàn trả
            BigDecimal tyLeHoanTra = BigDecimal.ZERO;
            if (tongTienGocHoaDon.compareTo(BigDecimal.ZERO) > 0) {
                tyLeHoanTra = tongTienHangTra.divide(tongTienGocHoaDon, 4, RoundingMode.HALF_UP);
            }

            // Tính giảm giá tương ứng
            BigDecimal tienGiamHoaDon = hoaDon.getTienGiam() != null ? hoaDon.getTienGiam() : BigDecimal.ZERO;
            BigDecimal giamGiaHoanTra = tienGiamHoaDon.multiply(tyLeHoanTra).setScale(0, RoundingMode.HALF_UP);

            // Tổng tiền hoàn thực tế
            BigDecimal tongTienHoanThucTe = tongTienHangTra.subtract(giamGiaHoanTra);
            if (tongTienHoanThucTe.compareTo(BigDecimal.ZERO) < 0) {
                tongTienHoanThucTe = BigDecimal.ZERO;
            }

            // Cập nhật tổng tiền hóa đơn (chưa trừ ngay, chờ admin xác nhận)
            List<ChiTietDonHang> chiTietDonHangs = hoaDon.getDonHang().getChiTietDonHangs();
            long totalItems = chiTietDonHangs.size();
            long returnedItems = chiTietDonHangs.stream()
                    .filter(item -> Boolean.TRUE.equals(item.getTrangThaiHoanTra()))
                    .count();

            String trangThaiTraHang = returnedItems == totalItems ? "Đã trả hàng" : "Đã trả hàng một phần";
            String ghiChu = "Yêu cầu trả hàng: " + lyDoTraHang +
                    ". Tổng tiền hàng trả: " + formatCurrency(tongTienHangTra) +
                    ". Giảm giá tương ứng: " + formatCurrency(giamGiaHoanTra) +
                    ". Tổng tiền hoàn trả thực tế: " + formatCurrency(tongTienHoanThucTe);

            // Cập nhật trạng thái hóa đơn
            updateStatus(hoaDonId, trangThaiTraHang, ghiChu, true);

            // Gửi thông báo cho khách hàng
            thongBaoService.taoThongBaoHeThong(
                    hoaDon.getDonHang().getNguoiDung().getTenDangNhap(),
                    "Yêu cầu trả hàng đã được gửi",
                    "Yêu cầu trả hàng cho đơn hàng " + hoaDon.getDonHang().getMaDonHang() + " đã được gửi và đang chờ xác nhận.",
                    hoaDon.getDonHang()
            );

        } catch (Exception e) {
            System.err.println("Lỗi trong processReturn: " + e.getMessage());
            throw e;
        }
    }

    public List<ChiTietDonHang> getReturnableItems(UUID hoaDonId) {
        HoaDon hoaDon = hoaDonRepository.findById(hoaDonId)
                .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại."));
        return hoaDon.getDonHang().getChiTietDonHangs().stream()
                .filter(item -> item.getTrangThaiHoanTra() == null || !item.getTrangThaiHoanTra())
                .collect(Collectors.toList());
    }

    private void validateTransition(String oldStatus, String newStatus, String salesMethod) {
        String o = oldStatus == null ? "Chưa xác nhận" : oldStatus;

        // ✅ CHO PHÉP TRẢ HÀNG (mọi luồng)
        // Từ: "Hoàn thành", "Vận chuyển thành công" hoặc đã "Đã trả hàng một phần"
        // Sang: "Đã trả hàng" hoặc "Đã trả hàng một phần"
        if ("Đã trả hàng".equals(newStatus) || "Đã trả hàng một phần".equals(newStatus)) {
            if (List.of("Hoàn thành", "Vận chuyển thành công", "Đã trả hàng một phần").contains(o)) {
                return; // hợp lệ -> không kiểm tra các rule khác nữa
            }
            throw new IllegalStateException("Không thể chuyển sang '" + newStatus + "' từ trạng thái: " + o);
        }

        // Giữ nguyên các rule hiện có bên dưới
        if ("Hủy đơn hàng".equals(newStatus)) {
            if (List.of("Chưa xác nhận", "Đã xác nhận", "Đã xác nhận Online", "Đang xử lý Online", "Đang vận chuyển").contains(o)) {
                return;
            }
            throw new IllegalStateException("Không thể hủy từ trạng thái: " + o);
        }

        if ("Đã đổi hàng".equals(newStatus)) {
            if (List.of("Hoàn thành", "Vận chuyển thành công", "Đã trả hàng một phần").contains(o)) {
                return;
            }
            throw new IllegalStateException("Không thể chuyển sang 'Đã đổi hàng' từ trạng thái: " + o);
        }

        if ("Tại quầy".equalsIgnoreCase(salesMethod)) {
            if (("Chưa xác nhận".equals(o) && "Hoàn thành".equals(newStatus)) || "Hoàn thành".equals(newStatus)) return;
            if ("Hoàn thành".equals(o) && List.of("Đã trả hàng", "Đã trả hàng một phần").contains(newStatus)) return;
            throw new IllegalStateException("Luồng Tại quầy không hỗ trợ chuyển từ " + o + " -> " + newStatus);
        }

        if ("Giao hàng".equalsIgnoreCase(salesMethod)) {
            if ("Chưa xác nhận".equals(o) && "Đã xác nhận".equals(newStatus)) return;
            if ("Đã xác nhận".equals(o) && "Đang vận chuyển".equals(newStatus)) return;
            if ("Đang vận chuyển".equals(o) && "Vận chuyển thành công".equals(newStatus)) return;
            if ("Vận chuyển thành công".equals(o) && "Hoàn thành".equals(newStatus)) return;
            throw new IllegalStateException("Luồng Giao hàng không hỗ trợ chuyển từ " + o + " -> " + newStatus);
        }

        if ("Online".equalsIgnoreCase(salesMethod)) {
            if ("Chưa xác nhận".equals(o) && "Đã xác nhận Online".equals(newStatus)) return;
            if ("Đã xác nhận Online".equals(o) && "Đang xử lý Online".equals(newStatus)) return;
            if ("Đang xử lý Online".equals(o) && "Đang vận chuyển".equals(newStatus)) return;
            if ("Đang vận chuyển".equals(o) && "Vận chuyển thành công".equals(newStatus)) return;
            if ("Vận chuyển thành công".equals(o) && "Hoàn thành".equals(newStatus)) return;
            throw new IllegalStateException("Luồng Online không hỗ trợ chuyển từ " + o + " -> " + newStatus);
        }

        throw new IllegalStateException("Phương thức bán hàng không hợp lệ: " + salesMethod);
    }

    private String mapHoaDonToDonHangStatus(String invoiceStatus) {
        return switch (invoiceStatus) {
            case "Chưa xác nhận" -> "cho_xac_nhan";
            case "Đã xác nhận", "Đã xác nhận Online" -> "da_xac_nhan";
            case "Đang xử lý Online" -> "dang_xu_ly";
            case "Đang vận chuyển" -> "dang_giao";
            case "Vận chuyển thành công" -> "da_giao";
            case "Hoàn thành" -> "hoan_thanh";
            case "Hủy đơn hàng" -> "huy";
            case "Đã đổi hàng" -> "da_doi_hang";
            case "Đã trả hàng" -> "da_tra_hang";
            case "Đã trả hàng một phần" -> "da_tra_hang_mot_phan";
            default -> throw new IllegalArgumentException("Trạng thái hóa đơn không hợp lệ: " + invoiceStatus);
        };
    }

    @Transactional
    public HoaDon updateStatus(UUID hoaDonId, String newStatus, String ghiChu, boolean updateDonHangToo) {
        HoaDon hd = hoaDonRepository.findById(hoaDonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
        validateTransition(hd.getTrangThai(), newStatus, hd.getDonHang().getPhuongThucBanHang());

        hd.setTrangThai(newStatus);
        hd.setGhiChu(ghiChu);
        if (List.of("Hoàn thành", "Hủy đơn hàng", "Vận chuyển thành công", "Đã đổi hàng").contains(newStatus)) {
            hd.setNgayThanhToan(LocalDateTime.now());
        }

        LichSuHoaDon ls = new LichSuHoaDon();
        ls.setHoaDon(hd);
        ls.setTrangThai(newStatus);
        ls.setGhiChu(ghiChu);
        ls.setThoiGian(LocalDateTime.now());
        lichSuHoaDonRepository.save(ls);
        hd.getLichSuHoaDons().add(ls);

        if (updateDonHangToo) {
            DonHang dh = hd.getDonHang();
            if (dh != null) {
                String s = mapHoaDonToDonHangStatus(newStatus);
                dh.setTrangThai(s);
                if ("hoan_thanh".equals(s) || "da_doi_hang".equals(s)) {
                    dh.setTrangThaiThanhToan(true);
                    dh.setThoiGianThanhToan(LocalDateTime.now());
                }
                donHangRepository.save(dh);
            }
        }

        HoaDon savedHoaDon = hoaDonRepository.saveAndFlush(hd);
        try {
            DonHang dh = savedHoaDon.getDonHang();
            String ma = dh.getMaDonHang();
            String tieuDe, noiDung;

            switch (newStatus) {
                case "Đã xác nhận":
                case "Đã xác nhận Online":
                    tieuDe = "Đơn hàng của bạn đã được xác nhận";
                    noiDung = "Đơn " + ma + " của bạn đã được xác nhận. " + (ghiChu != null ? ghiChu : "");
                    break;
                case "Đang xử lý Online":
                    tieuDe = "Đơn hàng của bạn đang xử lý";
                    noiDung = "Đơn " + ma + " đang được xử lý. " + (ghiChu != null ? ghiChu : "");
                    break;
                case "Đang vận chuyển":
                    tieuDe = "Đơn hàng của bạn đang vận chuyển";
                    noiDung = "Đơn " + ma + " đang trên đường giao. " + (ghiChu != null ? ghiChu : "");
                    break;
                case "Vận chuyển thành công":
                    tieuDe = "Đơn hàng của bạn đã vận chuyển thành công";
                    noiDung = "Đơn " + ma + " đã giao thành công. " + (ghiChu != null ? ghiChu : "");
                    break;
                case "Hoàn thành":
                    tieuDe = "Bạn đã hoàn thành đơn hàng";
                    noiDung = "Đơn " + ma + " đã hoàn thành. Cảm ơn bạn! " + (ghiChu != null ? ghiChu : "");
                    break;
                case "Hủy đơn hàng":
                    tieuDe = "Đơn hàng đã bị hủy";
                    noiDung = "Đơn " + ma + " đã bị hủy. " + (ghiChu != null ? ghiChu : "");
                    break;
                case "Đã đổi hàng":
                    tieuDe = "Đơn hàng của bạn đã được đổi hàng";
                    noiDung = "Đơn " + ma + " đã được đổi hàng thành công. " + (ghiChu != null ? ghiChu : "");
                    break;
                case "Chờ xử lý đổi hàng":
                    tieuDe = "Yêu cầu đổi hàng của bạn đang chờ xử lý";
                    noiDung = "Đơn " + ma + " đã gửi yêu cầu đổi hàng. " + (ghiChu != null ? ghiChu : "");
                    break;
                default:
                    tieuDe = "Cập nhật đơn hàng";
                    noiDung = "Đơn " + ma + " cập nhật: " + newStatus + ". " + (ghiChu != null ? ghiChu : "");
            }

            thongBaoService.thongBaoCapNhatTrangThai(dh.getId(), tieuDe, noiDung);
        } catch (Exception ignore) {}

        NguoiDung nguoiDung = hd.getNguoiDung();
        if (nguoiDung != null && nguoiDung.getEmail() != null && !nguoiDung.getEmail().isEmpty()) {
            String emailSubject = "Cập nhật trạng thái đơn hàng - ACV Store";
            String emailContent = "<html>" +
                    "<body style='font-family: Arial, sans-serif; color: #333; background-color: #f4f4f4; margin: 0; padding: 0;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px; background-color: #fff;'>" +
                    "<h2 style='color: #0000FF; text-align: center;'>ACV Store Xin Chào</h2>" +
                    "<h2 style='color: #153054; text-align: center;'>Cập nhật trạng thái đơn hàng</h2>" +
                    "<p style='text-align: center;'>Xin chào " + (hd.getDiaChi() != null ? hd.getDiaChi().getNguoiNhan() : nguoiDung.getHoTen()) + ",</p>" +
                    "<p style='text-align: center;'>Đơn hàng của bạn với mã <strong>" + hd.getDonHang().getMaDonHang() + "</strong> đã được cập nhật sang trạng thái: <strong>" + newStatus + "</strong>.</p>" +
                    "<p style='text-align: center;'><strong>Chi tiết:</strong> " + ghiChu + "</p>" +
                    "<p style='text-align: center; margin-top: 20px;'>Cảm ơn bạn đã mua sắm tại ACV Store!</p>" +
                    "<p style='text-align: center; margin-top: 20px;'>Trân trọng,<br>Đội ngũ ACV Store</p>" +
                    "<a href='http://localhost:8080/dsdon-mua/chi-tiet/" + hd.getId() + "' style='display: block; padding: 10px 20px; background: #153054; color: white; text-decoration: none; text-align: center; border-radius: 5px; margin-top: 20px; margin-left: auto; margin-right: auto; width: fit-content;'>Xem chi tiết đơn hàng</a>" +
                    "</div>" +
                    "</body>" +
                    "</html>";
            emailService.sendEmail(nguoiDung.getEmail(), emailSubject, emailContent);
        }

        return savedHoaDon;
    }
}