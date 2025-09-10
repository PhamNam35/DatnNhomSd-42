package com.example.AsmGD1.service.ThongKe;

import com.example.AsmGD1.dto.ThongKe.SanPhamBanChayDTO;
import com.example.AsmGD1.dto.ThongKe.SanPhamTonKhoThapDTO;
import com.example.AsmGD1.dto.ThongKe.ThongKeDoanhThuDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class ThongKeExcelExporter {

    public byte[] exportThongKe(
            String boLoc,
            LocalDate ngayBatDau,
            LocalDate ngayKetThuc,
            ThongKeDoanhThuDTO stats,
            List<String> chartLabels,
            List<Integer> chartOrders,
            List<Long> chartRevenue,
            Map<String, Double> trangThaiPercentMap,
            List<SanPhamBanChayDTO> topSelling,
            List<SanPhamTonKhoThapDTO> lowStock
    ) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ===== Styles =====
            CellStyle header = wb.createCellStyle();
            Font hFont = wb.createFont(); hFont.setBold(true);
            header.setFont(hFont);
            header.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setBorderBottom(BorderStyle.THIN);
            header.setBorderTop(BorderStyle.THIN);
            header.setBorderLeft(BorderStyle.THIN);
            header.setBorderRight(BorderStyle.THIN);

            CellStyle number = wb.createCellStyle();
            DataFormat df = wb.createDataFormat();
            number.setDataFormat(df.getFormat("#,##0"));

            CellStyle percent = wb.createCellStyle();
            percent.setDataFormat(df.getFormat("0.0%"));

            // ===== Sheet 0: Bộ lọc (đảm bảo luôn createCell trước khi set) =====
            Sheet s0 = wb.createSheet("Bo loc");

            Row r0 = s0.createRow(0);
            Cell c00 = r0.createCell(0);
            c00.setCellValue("Bộ lọc");
            c00.setCellStyle(header);

            Row r2 = s0.createRow(2);
            r2.createCell(0).setCellValue("Kiểu bộ lọc");
            r2.createCell(1).setCellValue(boLoc);

            Row r3 = s0.createRow(3);
            r3.createCell(0).setCellValue("Ngày bắt đầu");
            r3.createCell(1).setCellValue(ngayBatDau != null ? ngayBatDau.toString() : "");

            Row r4 = s0.createRow(4);
            r4.createCell(0).setCellValue("Ngày kết thúc");
            r4.createCell(1).setCellValue(ngayKetThuc != null ? ngayKetThuc.toString() : "");

            autosize(s0, 0, 1);

            // ===== Sheet 1: Tổng quan =====
            Sheet s1 = wb.createSheet("Tong quan");
            Row rH = s1.createRow(0);
            String[] h1 = {"Chỉ tiêu", "Giá trị (VND / số)"};
            for (int i = 0; i < h1.length; i++) {
                Cell c = rH.createCell(i);
                c.setCellValue(h1[i]);
                c.setCellStyle(header);
            }

            Object[][] rows1 = {
                    {"Doanh thu hôm nay (VND)", toLong(stats.getDoanhThuNgay())},
                    {"Doanh thu tháng (VND)",   toLong(stats.getDoanhThuThang())},
                    {"Doanh thu năm (VND)",     toLong(stats.getDoanhThuNam())},
                    {"Số đơn hôm nay",          (long) stats.getSoDonHangNgay()},
                    {"Số đơn tháng",            (long) stats.getSoDonHangThang()},
                    {"Số SP bán trong tháng",   (long) stats.getSoSanPhamThang()},
                    {"Tăng trưởng ngày (%)",    stats.getTangTruongNgay()},
                    {"Tăng trưởng tháng (%)",   stats.getTangTruongThang()},
                    {"Tăng trưởng năm (%)",     stats.getTangTruongNam()},
                    {"Tăng trưởng SP tháng (%)",stats.getTangTruongSanPhamThang()},
            };

            for (int i = 0; i < rows1.length; i++) {
                Row r = s1.createRow(i + 1);
                r.createCell(0).setCellValue(String.valueOf(rows1[i][0]));
                Cell v = r.createCell(1);
                Object val = rows1[i][1];
                if (val instanceof Number n) {
                    v.setCellValue(n.doubleValue());
                    if (i <= 2) v.setCellStyle(number); // 3 dòng đầu là doanh thu VND
                } else {
                    v.setCellValue(String.valueOf(val));
                }
            }
            autosize(s1, 0, 1);

            // ===== Sheet 2: Dữ liệu biểu đồ =====
            Sheet s2 = wb.createSheet("Bieu do");
            Row rh2 = s2.createRow(0);
            String[] h2 = {"Ngày", "Số hóa đơn", "Doanh thu (VND)"};
            for (int i = 0; i < h2.length; i++) {
                Cell c = rh2.createCell(i);
                c.setCellValue(h2[i]);
                c.setCellStyle(header);
            }

            int len = chartLabels != null ? chartLabels.size() : 0;
            for (int i = 0; i < len; i++) {
                Row r = s2.createRow(i + 1);
                r.createCell(0).setCellValue(chartLabels.get(i));
                Cell c1 = r.createCell(1);
                c1.setCellValue(safeInt(chartOrders, i));
                Cell c2 = r.createCell(2);
                c2.setCellValue(safeLong(chartRevenue, i));
                c2.setCellStyle(number);
            }
            autosize(s2, 0, 2);

            // ===== Sheet 3: Trạng thái đơn =====
            Sheet s3 = wb.createSheet("Trang thai don");
            Row rh3 = s3.createRow(0);
            String[] h3 = {"Trạng thái", "Tỷ lệ (%)"};
            for (int i = 0; i < h3.length; i++) {
                Cell c = rh3.createCell(i);
                c.setCellValue(h3[i]);
                c.setCellStyle(header);
            }

            int rowIdx = 1;
            if (trangThaiPercentMap != null) {
                for (Map.Entry<String, Double> e : trangThaiPercentMap.entrySet()) {
                    Row r = s3.createRow(rowIdx++);
                    r.createCell(0).setCellValue(e.getKey());
                    Cell v = r.createCell(1);
                    v.setCellValue((e.getValue() != null ? e.getValue() : 0.0) / 100.0); // % -> 0..1
                    v.setCellStyle(percent);
                }
            }
            autosize(s3, 0, 1);

            // ===== Sheet 4: Top bán chạy =====
            Sheet s4 = wb.createSheet("Top ban chay");
            Row rh4 = s4.createRow(0);
            String[] h4 = {"#", "Tên SP", "Màu", "Size", "Giá bán (VND)", "SL đã bán", "Ảnh"};
            for (int i = 0; i < h4.length; i++) {
                Cell c = rh4.createCell(i);
                c.setCellValue(h4[i]);
                c.setCellStyle(header);
            }

            if (topSelling != null) {
                for (int i = 0; i < topSelling.size(); i++) {
                    SanPhamBanChayDTO d = topSelling.get(i);
                    Row r = s4.createRow(i + 1);
                    r.createCell(0).setCellValue(i + 1);
                    r.createCell(1).setCellValue(nz(d.getTenSanPham()));
                    r.createCell(2).setCellValue(nz(d.getMauSac()));
                    r.createCell(3).setCellValue(nz(d.getKichCo()));
                    Cell gia = r.createCell(4);
                    gia.setCellValue(safeDouble(d.getGia()));
                    gia.setCellStyle(number);
                    r.createCell(5).setCellValue(safeLong(d.getSoLuongDaBan()));
                    r.createCell(6).setCellValue(nz(d.getImageUrl()));
                }
            }
            autosize(s4, 0, 6);

            // ===== Sheet 5: Sắp hết hàng =====
            Sheet s5 = wb.createSheet("Sap het hang");
            Row rh5 = s5.createRow(0);
            String[] h5 = {"#", "Tên SP", "Màu", "Size", "Giá bán (VND)", "Tồn kho", "Ảnh"};
            for (int i = 0; i < h5.length; i++) {
                Cell c = rh5.createCell(i);
                c.setCellValue(h5[i]);
                c.setCellStyle(header);
            }

            if (lowStock != null) {
                for (int i = 0; i < lowStock.size(); i++) {
                    SanPhamTonKhoThapDTO d = lowStock.get(i);
                    Row r = s5.createRow(i + 1);
                    r.createCell(0).setCellValue(i + 1);
                    r.createCell(1).setCellValue(nz(d.getTenSanPham()));
                    r.createCell(2).setCellValue(nz(d.getMauSac()));
                    r.createCell(3).setCellValue(nz(d.getKichCo()));
                    Cell gia = r.createCell(4);
                    gia.setCellValue(safeDouble(d.getGia()));
                    gia.setCellStyle(number);
                    r.createCell(5).setCellValue(safeLong(d.getSoLuongTonKho()));
                    r.createCell(6).setCellValue(nz(d.getImageUrl()));
                }
            }
            autosize(s5, 0, 6);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Xuất Excel thất bại: " + e.getMessage(), e);
        }
    }

    // ===== helpers =====
    private static void autosize(Sheet s, int from, int to) {
        for (int i = from; i <= to; i++) s.autoSizeColumn(i);
    }
    private static long toLong(java.math.BigDecimal bd) { return bd == null ? 0L : bd.longValue(); }
    private static int safeInt(List<Integer> list, int i) { return (list != null && i < list.size() && list.get(i) != null) ? list.get(i) : 0; }
    private static long safeLong(List<Long> list, int i) { return (list != null && i < list.size() && list.get(i) != null) ? list.get(i) : 0L; }
    private static double safeDouble(Number n) { return n == null ? 0d : n.doubleValue(); }
    private static long safeLong(Number n) { return n == null ? 0L : n.longValue(); }
    private static String nz(String s) { return s == null ? "" : s; }
}
