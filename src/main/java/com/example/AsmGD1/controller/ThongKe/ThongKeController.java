package com.example.AsmGD1.controller.ThongKe;

import com.example.AsmGD1.dto.ThongKe.SanPhamBanChayDTO;
import com.example.AsmGD1.dto.ThongKe.SanPhamTonKhoThapDTO;
import com.example.AsmGD1.dto.ThongKe.ThongKeDoanhThuDTO;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.BanHang.DonHangService;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.ThongKe.ThongKeExcelExporter;
import com.example.AsmGD1.service.ThongKe.ThongKeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/polyshoe/thong-ke")
public class ThongKeController {

    @Autowired
    private DonHangService donHangService;

    @Autowired
    private ThongKeService thongKeDichVu;

    @Autowired
    private NguoiDungService nguoiDungService;

    @Autowired
    private ThongKeExcelExporter thongKeExcelExporter;

    @GetMapping
    public String layThongKe(
            @RequestParam(defaultValue = "month") String boLoc,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngayBatDau,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngayKetThuc,
            @RequestParam(defaultValue = "0") int topSellingPage,
            @RequestParam(defaultValue = "0") int lowStockPage,
            Model model) {

        LocalDate homNay = LocalDate.now();
        String trangThaiBoLoc;

        switch (boLoc) {
            case "day" -> {
                ngayBatDau = homNay; ngayKetThuc = homNay;
                trangThaiBoLoc = "Hôm nay: " + homNay;
            }
            case "7days" -> {
                ngayBatDau = homNay.minusDays(6); ngayKetThuc = homNay;
                trangThaiBoLoc = "7 ngày: " + ngayBatDau + " đến " + ngayKetThuc;
            }
            case "month" -> {
                ngayBatDau = homNay.withDayOfMonth(1); ngayKetThuc = homNay;
                trangThaiBoLoc = "Tháng: " + homNay.getMonthValue() + "/" + homNay.getYear();
            }
            case "year" -> {
                ngayBatDau = homNay.withDayOfYear(1); ngayKetThuc = homNay;
                trangThaiBoLoc = "Năm: " + homNay.getYear();
            }
            case "custom_range" -> {
                if (ngayBatDau == null || ngayKetThuc == null) {
                    model.addAttribute("error", "Vui lòng chọn ngày bắt đầu và kết thúc.");
                    return "WebQuanLy/thong-ke";
                }
                if (ngayBatDau.isAfter(ngayKetThuc)) {
                    model.addAttribute("error", "Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc.");
                    return "WebQuanLy/thong-ke";
                }
                trangThaiBoLoc = "Tùy chỉnh: " + ngayBatDau + " đến " + ngayKetThuc;
            }
            default -> {
                model.addAttribute("error", "Bộ lọc không hợp lệ.");
                return "WebQuanLy/thong-ke";
            }
        }

        Pageable topSellingPageable = PageRequest.of(topSellingPage, 5);
        Pageable lowStockPageable   = PageRequest.of(lowStockPage, 5);

        ThongKeDoanhThuDTO thongKe = thongKeDichVu.layThongKeDoanhThu(boLoc, ngayBatDau, ngayKetThuc);
        Page<SanPhamBanChayDTO> sanPhamBanChay =
                thongKeDichVu.laySanPhamBanChay(boLoc, ngayBatDau, ngayKetThuc, topSellingPageable);
        Page<SanPhamTonKhoThapDTO> sanPhamTonKhoThap =
                thongKeDichVu.laySanPhamTonKhoThap(lowStockPageable);

        model.addAttribute("chartLabels",  thongKeDichVu.layNhanBieuDoLienTuc(ngayBatDau, ngayKetThuc));
        model.addAttribute("chartOrders",  thongKeDichVu.layDonHangBieuDoLienTuc(ngayBatDau, ngayKetThuc));
        model.addAttribute("chartRevenue", thongKeDichVu.layDoanhThuBieuDoLienTuc(ngayBatDau, ngayKetThuc));

        model.addAttribute("stats", thongKe);
        model.addAttribute("topSellingProducts", sanPhamBanChay);
        model.addAttribute("lowStockProducts", sanPhamTonKhoThap);
        model.addAttribute("filterStatus", trangThaiBoLoc);
        model.addAttribute("boLoc", boLoc);
        model.addAttribute("ngayBatDau", ngayBatDau);
        model.addAttribute("ngayKetThuc", ngayKetThuc);
        model.addAttribute("topSellingPage", topSellingPage);
        model.addAttribute("lowStockPage", lowStockPage);

        List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
        model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung user) {
            model.addAttribute("user", user);
        }

        LocalDateTime startDateTime = ngayBatDau.atStartOfDay();
        LocalDateTime endDateTime   = ngayKetThuc.atTime(LocalTime.MAX);

        // 4 số tổng quan: CHỈ đơn hoàn thành (đúng như UI)
        Map<String, Object> tongQuan = thongKeDichVu.layTongQuanHoanThanh(ngayBatDau, ngayKetThuc);
        model.addAttribute("sumCustomers", tongQuan.get("khach"));
        model.addAttribute("sumProducts",  tongQuan.get("sanPham"));
        model.addAttribute("sumOrders",    tongQuan.get("don"));
        model.addAttribute("sumRevenue",   tongQuan.get("doanhThu"));

        // Theo phương thức (nếu bạn vẫn dùng ở nơi khác)
        Map<String, Integer> tongDonHangTheoPhuongThuc =
                donHangService.demDonHangTheoPhuongThuc(startDateTime, endDateTime);
        model.addAttribute("tongDonTheoPhuongThuc", tongDonHangTheoPhuongThuc);

        // Pie: lấy TẤT CẢ trạng thái (đúng yêu cầu hiện tại)
        Map<String, Double> trangThaiPercentMap =
                thongKeDichVu.layPhanTramTatCaTrangThaiDonHang(ngayBatDau, ngayKetThuc);
        model.addAttribute("trangThaiPercentMap", trangThaiPercentMap);

        return "WebQuanLy/thong-ke";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(defaultValue = "month") String boLoc,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngayBatDau,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngayKetThuc
    ) {
        LocalDate today = LocalDate.now();
        switch (boLoc) {
            case "day"   -> { ngayBatDau = today; ngayKetThuc = today; }
            case "7days" -> { ngayBatDau = today.minusDays(6); ngayKetThuc = today; }
            case "month" -> { ngayBatDau = today.withDayOfMonth(1); ngayKetThuc = today; }
            case "year"  -> { ngayBatDau = today.withDayOfYear(1); ngayKetThuc = today; }
            case "custom_range" -> {
                if (ngayBatDau == null || ngayKetThuc == null)
                    throw new IllegalArgumentException("Vui lòng chọn ngày bắt đầu và kết thúc.");
                if (ngayBatDau.isAfter(ngayKetThuc))
                    throw new IllegalArgumentException("Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc.");
            }
            default -> { }
        }

        // Lấy đúng 4 KPI giống trên UI
        Map<String, Object> tongQuan = thongKeDichVu.layTongQuanHoanThanh(ngayBatDau, ngayKetThuc);
        long sumCustomers = ((Number) tongQuan.getOrDefault("khach", 0)).longValue();
        long sumProducts  = ((Number) tongQuan.getOrDefault("sanPham", 0)).longValue();
        long sumOrders    = ((Number) tongQuan.getOrDefault("don", 0)).longValue();
        long sumRevenue   = ((Number) tongQuan.getOrDefault("doanhThu", 0)).longValue();

        // % trạng thái (mọi trạng thái)
        Map<String, Double> trangThaiPercentMap =
                thongKeDichVu.layPhanTramTatCaTrangThaiDonHang(ngayBatDau, ngayKetThuc);

        byte[] bytes = thongKeExcelExporter.exportThongKeLite(
                boLoc, ngayBatDau, ngayKetThuc,
                sumCustomers, sumProducts, sumOrders, sumRevenue,
                trangThaiPercentMap
        );

        String fileName = String.format("thong-ke_%s_%s.xlsx", ngayBatDau, ngayKetThuc);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(bytes.length)
                .body(bytes);
    }
}
