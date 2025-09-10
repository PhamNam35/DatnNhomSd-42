package com.example.AsmGD1.service.ThongKe;

import com.example.AsmGD1.dto.ThongKe.SanPhamBanChayDTO;
import com.example.AsmGD1.dto.ThongKe.SanPhamTonKhoThapDTO;
import com.example.AsmGD1.dto.ThongKe.ThongKeDoanhThuDTO;
import com.example.AsmGD1.repository.SanPham.HinhAnhSanPhamRepository;
import com.example.AsmGD1.repository.ThongKe.ThongKeRepository;
import com.example.AsmGD1.service.BanHang.DonHangService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ThongKeService {

    @Autowired
    private ThongKeRepository thongKeRepository;

    @Autowired
    private HinhAnhSanPhamRepository hinhAnhSanPhamRepository;

    @Autowired
    private DonHangService donHangService;

    public ThongKeDoanhThuDTO layThongKeDoanhThu(String boLoc, LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        ThongKeDoanhThuDTO thongKe = new ThongKeDoanhThuDTO();
        LocalDate homNay = LocalDate.now();

        switch (boLoc) {
            case "day" -> { ngayBatDau = homNay; ngayKetThuc = homNay; }
            case "7days" -> { ngayBatDau = homNay.minusDays(6); ngayKetThuc = homNay; }
            case "month" -> { ngayBatDau = homNay.withDayOfMonth(1); ngayKetThuc = homNay; }
            case "year" -> { ngayBatDau = homNay.withDayOfYear(1); ngayKetThuc = homNay; }
            case "custom_range" -> {
                if (ngayBatDau == null || ngayKetThuc == null)
                    throw new IllegalArgumentException("Ngày bắt đầu và kết thúc không được để trống.");
                if (ngayBatDau.isAfter(ngayKetThuc))
                    throw new IllegalArgumentException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
            }
        }

        LocalDateTime dayStart   = homNay.atStartOfDay();
        LocalDateTime dayEnd     = homNay.atTime(LocalTime.MAX);
        LocalDateTime monthStart = homNay.withDayOfMonth(1).atStartOfDay();
        LocalDateTime yearStart  = homNay.withDayOfYear(1).atStartOfDay();

        BigDecimal doanhThuNgay  = defaultBigDecimal(thongKeRepository.tinhDoanhThuTheoHoaDon(dayStart, dayEnd));
        BigDecimal doanhThuThang = defaultBigDecimal(thongKeRepository.tinhDoanhThuTheoHoaDon(monthStart, dayEnd));
        BigDecimal doanhThuNam   = defaultBigDecimal(thongKeRepository.tinhDoanhThuTheoHoaDon(yearStart, dayEnd));

        Integer donHangNgay  = defaultInteger(thongKeRepository.demHoaDonTheoKhoangThoiGian(dayStart, dayEnd));
        Integer donHangThang = defaultInteger(thongKeRepository.demHoaDonTheoKhoangThoiGian(monthStart, dayEnd));
        Integer sanPhamThang = defaultInteger(thongKeRepository.demSanPhamTheoHoaDon(monthStart, dayEnd));

        BigDecimal dtNgayTruoc = defaultBigDecimal(
                thongKeRepository.tinhDoanhThuTheoHoaDon(
                        dayStart.minusDays(1),
                        homNay.minusDays(1).atTime(LocalTime.MAX)));

        LocalDate prevMonth = homNay.minusMonths(1);
        BigDecimal dtThangTruoc = defaultBigDecimal(
                thongKeRepository.tinhDoanhThuTheoHoaDon(
                        prevMonth.withDayOfMonth(1).atStartOfDay(),
                        prevMonth.withDayOfMonth(prevMonth.lengthOfMonth()).atTime(LocalTime.MAX)));

        LocalDate prevYear = homNay.minusYears(1);
        BigDecimal dtNamTruoc = defaultBigDecimal(
                thongKeRepository.tinhDoanhThuTheoHoaDon(
                        prevYear.withDayOfYear(1).atStartOfDay(),
                        prevYear.withDayOfYear(prevYear.lengthOfYear()).atTime(LocalTime.MAX)));

        Integer spThangTruoc = defaultInteger(
                thongKeRepository.demSanPhamTheoHoaDon(
                        prevMonth.withDayOfMonth(1).atStartOfDay(),
                        prevMonth.withDayOfMonth(prevMonth.lengthOfMonth()).atTime(LocalTime.MAX)));

        thongKe.setDoanhThuNgay(doanhThuNgay);
        thongKe.setDoanhThuThang(doanhThuThang);
        thongKe.setDoanhThuNam(doanhThuNam);
        thongKe.setSoDonHangNgay(donHangNgay);
        thongKe.setSoDonHangThang(donHangThang);
        thongKe.setSoSanPhamThang(sanPhamThang);

        thongKe.setTangTruongNgay(tinhTangTruong(doanhThuNgay, dtNgayTruoc));
        thongKe.setTangTruongThang(tinhTangTruong(doanhThuThang, dtThangTruoc));
        thongKe.setTangTruongNam(tinhTangTruong(doanhThuNam, dtNamTruoc));
        thongKe.setTangTruongSanPhamThang(tinhTangTruong(sanPhamThang, spThangTruoc));

        return thongKe;
    }

    public Page<SanPhamBanChayDTO> laySanPhamBanChay(String boLoc, LocalDate ngayBatDau, LocalDate ngayKetThuc, Pageable pageable) {
        LocalDate homNay = LocalDate.now();

        switch (boLoc) {
            case "day" -> { ngayBatDau = homNay; ngayKetThuc = homNay; }
            case "7days" -> { ngayBatDau = homNay.minusDays(6); ngayKetThuc = homNay; }
            case "month" -> { ngayBatDau = homNay.withDayOfMonth(1); ngayKetThuc = homNay; }
            case "year" -> { ngayBatDau = homNay.withDayOfYear(1); ngayKetThuc = homNay; }
            case "custom_range" -> {
                if (ngayBatDau == null || ngayKetThuc == null)
                    throw new IllegalArgumentException("Ngày bắt đầu và kết thúc không được để trống.");
                if (ngayBatDau.isAfter(ngayKetThuc))
                    throw new IllegalArgumentException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
            }
            default -> throw new IllegalArgumentException("Bộ lọc không hợp lệ.");
        }

        LocalDateTime start = ngayBatDau.atStartOfDay();
        LocalDateTime end   = ngayKetThuc.atTime(LocalTime.MAX);

        Page<SanPhamBanChayDTO> page = thongKeRepository.laySanPhamBanChayTheoHoaDon(start, end, pageable);
        for (SanPhamBanChayDTO dto : page.getContent()) {
            String img = hinhAnhSanPhamRepository
                    .findFirstImageByChiTietSanPham(dto.getIdChiTietSanPham())
                    .orElse("/img/default.png");
            dto.setImageUrl(img);
        }
        return page;
    }

    public Page<SanPhamTonKhoThapDTO> laySanPhamTonKhoThap(Pageable pageable) {
        Page<SanPhamTonKhoThapDTO> page = thongKeRepository.laySanPhamTonKhoThap(30, pageable);
        for (SanPhamTonKhoThapDTO dto : page.getContent()) {
            String img = hinhAnhSanPhamRepository
                    .findFirstImageByChiTietSanPham(dto.getIdChiTietSanPham())
                    .orElse("/img/default.png");
            dto.setImageUrl(img);
        }
        return page;
    }

    public List<String> layNhanBieuDo(LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        List<LocalDate> days = thongKeRepository.layNhanBieuDoTheoHoaDon(
                ngayBatDau.atStartOfDay(), ngayKetThuc.atTime(LocalTime.MAX));
        return days.stream().map(LocalDate::toString).collect(Collectors.toList());
    }

    public List<Integer> layDonHangBieuDo(LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        List<LocalDate> days = thongKeRepository.layNhanBieuDoTheoHoaDon(
                ngayBatDau.atStartOfDay(), ngayKetThuc.atTime(LocalTime.MAX));
        List<Integer> result = new ArrayList<>();
        for (LocalDate day : days) {
            Integer count = thongKeRepository.laySoHoaDonTheoNgay(day);
            result.add(count != null ? count : 0);
        }
        return result;
    }

    public List<Integer> laySanPhamBieuDo(LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        List<LocalDate> days = thongKeRepository.layNhanBieuDoTheoHoaDon(
                ngayBatDau.atStartOfDay(), ngayKetThuc.atTime(LocalTime.MAX));
        List<Integer> result = new ArrayList<>();
        for (LocalDate day : days) {
            Integer count = thongKeRepository.laySoSanPhamTheoNgay(day);
            result.add(count != null ? count : 0);
        }
        return result;
    }

    private Double tinhTangTruong(BigDecimal hienTai, BigDecimal truocDo) {
        if (truocDo == null || truocDo.compareTo(BigDecimal.ZERO) == 0)
            return hienTai != null && hienTai.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        if (hienTai == null) return -100.0;

        return hienTai.subtract(truocDo)
                .divide(truocDo, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private Double tinhTangTruong(Integer hienTai, Integer truocDo) {
        if (truocDo == null || truocDo == 0)
            return hienTai != null && hienTai > 0 ? 100.0 : 0.0;
        if (hienTai == null) return -100.0;

        return ((double) (hienTai - truocDo) / truocDo) * 100;
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Integer defaultInteger(Integer value) {
        return value != null ? value : 0;
    }

    public Map<String, Map<String, Integer>> thongKeChiTietPhuongThucVaTrangThai(LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        LocalDateTime start = ngayBatDau.atStartOfDay();
        LocalDateTime end = ngayKetThuc.plusDays(1).atStartOfDay();
        return donHangService.thongKeChiTietTheoPhuongThucVaTrangThai(start, end);
    }

    private String normalize(String s) {
        if (s == null) return "";
        String lower = s.toLowerCase().trim();
        lower = java.text.Normalizer.normalize(lower, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        lower = lower.replaceAll("\\s+", "_");
        return lower;
    }

    private String toDisplayLabel(String raw) {
        String n = normalize(raw);
        if (n.equals("thanh_cong") || n.equals("hoan_thanh")) return "Thành công";
        return switch (n) {
            case "cho_xac_nhan", "cho_duyet" -> "Chờ xác nhận";
            case "dang_chuan_bi", "chuan_bi" -> "Đang chuẩn bị";
            case "dang_giao", "giao_hang" -> "Đang giao";
            case "da_huy", "huy" -> "Đã hủy";
            case "tra_hang", "hoan_tra" -> "Trả hàng";
            case "thanh_toan_that_bai" -> "Thanh toán thất bại";
            default -> {
                String original = raw == null ? "" : raw.trim();
                yield original.isEmpty() ? "Khác" :
                        original.substring(0,1).toUpperCase() + original.substring(1);
            }
        };
    }

    public Map<String, Double> layPhanTramTatCaTrangThaiDonHang(LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        LocalDateTime start = ngayBatDau.atStartOfDay();
        LocalDateTime end = ngayKetThuc.atTime(LocalTime.MAX);

        List<Object[]> rawData = thongKeRepository.thongKePhanTramTatCaTrangThaiDonHang(start, end);
        int tong = rawData.stream()
                .mapToInt(r -> ((Long) r[1]).intValue())
                .sum();

        Map<String, Double> result = new LinkedHashMap<>();
        for (Object[] row : rawData) {
            String rawStatus = (String) row[0];
            int count = ((Long) row[1]).intValue();
            String label = toDisplayLabel(rawStatus);
            double percent = (tong == 0) ? 0.0 : (count * 100.0 / tong);
            result.merge(label, percent, Double::sum);
        }

        return result;
    }

    private List<LocalDate> taoTrucNgayLienTuc(LocalDate start, LocalDate end) {
        List<LocalDate> days = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            days.add(d);
        }
        return days;
    }

    public List<String> layNhanBieuDoLienTuc(LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        return taoTrucNgayLienTuc(ngayBatDau, ngayKetThuc)
                .stream().map(LocalDate::toString).collect(Collectors.toList());
    }

    public List<Integer> layDonHangBieuDoLienTuc(LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        LocalDateTime start = ngayBatDau.atStartOfDay();
        LocalDateTime end   = ngayKetThuc.atTime(LocalTime.MAX);
        List<Object[]> rows = thongKeRepository.thongKeSoHoaDonTheoNgay(start, end);

        Map<LocalDate, Integer> map = new HashMap<>();
        for (Object[] r : rows) {
            LocalDate d = ((java.sql.Date) r[0]).toLocalDate();
            Integer c   = (Integer) r[1];
            map.put(d, c);
        }

        List<Integer> result = new ArrayList<>();
        for (LocalDate d : taoTrucNgayLienTuc(ngayBatDau, ngayKetThuc)) {
            result.add(map.getOrDefault(d, 0));
        }
        return result;
    }

    public List<Long> layDoanhThuBieuDoLienTuc(LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        LocalDateTime start = ngayBatDau.atStartOfDay();
        LocalDateTime end   = ngayKetThuc.atTime(LocalTime.MAX);
        List<Object[]> rows = thongKeRepository.thongKeDoanhThuTheoNgay(start, end);

        Map<LocalDate, Long> map = new HashMap<>();
        for (Object[] r : rows) {
            LocalDate d = ((java.sql.Date) r[0]).toLocalDate();
            Long sum = 0L;
            if (r[1] != null) {
                sum = (r[1] instanceof BigDecimal)
                        ? ((BigDecimal) r[1]).longValue()
                        : ((Number) r[1]).longValue();
            }
            map.put(d, sum);
        }

        List<Long> result = new ArrayList<>();
        for (LocalDate d : taoTrucNgayLienTuc(ngayBatDau, ngayKetThuc)) {
            result.add(map.getOrDefault(d, 0L));
        }
        return result;
    }
}