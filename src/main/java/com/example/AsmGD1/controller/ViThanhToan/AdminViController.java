package com.example.AsmGD1.controller.ViThanhToan;

import com.example.AsmGD1.entity.LichSuGiaoDichVi;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.ViThanhToan;
import com.example.AsmGD1.entity.YeuCauRutTien;
import com.example.AsmGD1.repository.ViThanhToan.LichSuGiaoDichViRepository;
import com.example.AsmGD1.repository.ViThanhToan.ViThanhToanRepository;
import com.example.AsmGD1.repository.ViThanhToan.YeuCauRutTienRepository;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/acvstore/vi")
public class AdminViController {

    @Autowired
    private ViThanhToanRepository viRepo;

    @Autowired
    private YeuCauRutTienRepository rutTienRepo;

    @Autowired
    private NguoiDungService nguoiDungService;

    @Autowired
    private LichSuGiaoDichViRepository lichSuGiaoDichViRepository;
    // Tổng số dư của tất cả ví
    @GetMapping("/tong-hop")
    public String tongHopVi(Model model) {
        List<ViThanhToan> danhSachVi = viRepo.findAll();
        BigDecimal tongSoDu = danhSachVi.stream()
                .map(ViThanhToan::getSoDu)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("tongSoDu", tongSoDu);
        model.addAttribute("danhSachVi", danhSachVi);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String tenDangNhap = authentication.getName();
        return "WebQuanLy/vi/tong-hop";
    }

    // Danh sách yêu cầu rút tiền
    @GetMapping("/yeu-cau-rut-tien")
    public String danhSachYeuCauRutTien(Model model) {
        List<YeuCauRutTien> danhSachYeuCau = rutTienRepo.findAll();
        model.addAttribute("danhSachYeuCau", danhSachYeuCau);

        // 👇 Lấy người dùng đang đăng nhập
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String tenDangNhap = authentication.getName();

        NguoiDung user = nguoiDungService.findByTenDangNhap(tenDangNhap);
        model.addAttribute("user", user);

        return "WebQuanLy/vi/yeu-cau-rut-tien";
    }

    // Duyệt yêu cầu rút tiền
    @PostMapping("/duyet/{id}")
    public String duyetRutTien(@PathVariable UUID id,
                               @RequestParam("anhBangChung") MultipartFile file) {
        YeuCauRutTien yeuCau = rutTienRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));

        if (!"Đang chờ".equalsIgnoreCase(yeuCau.getTrangThai())) {
            return "redirect:/acvstore/vi/yeu-cau-rut-tien";
        }

        ViThanhToan vi = yeuCau.getViThanhToan();
        BigDecimal soTienRut = yeuCau.getSoTien();

        if (vi.getSoDu().compareTo(soTienRut) >= 0) {
            try {
                // 📁 Lấy hệ điều hành và set đường dẫn upload
                String os = System.getProperty("os.name").toLowerCase();
                String uploadDir;

                if (os.contains("win")) {
                    uploadDir = "C:/DATN/uploads/bang-chung/";
                } else {
                    String userHome = System.getProperty("user.home");
                    uploadDir = userHome + "/DATN/uploads/bang-chung/";
                }

                // 👉 Làm sạch tên file để tránh lỗi unicode & khoảng trắng
                String originalFileName = file.getOriginalFilename();
                String cleanedFileName = java.text.Normalizer.normalize(originalFileName, java.text.Normalizer.Form.NFD)
                        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                        .replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");

                String fileName = UUID.randomUUID().toString() + "_" + cleanedFileName;

                // Tạo folder nếu chưa tồn tại
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                // Ghi file
                Path filePath = uploadPath.resolve(fileName);
                Files.write(filePath, file.getBytes());

                // Gán tên file vào DB
                yeuCau.setAnhBangChung(fileName);

            } catch (Exception e) {
                e.printStackTrace();
            }

            // Trừ tiền và cập nhật ví
            vi.setSoDu(vi.getSoDu().subtract(soTienRut));
            vi.setThoiGianCapNhat(LocalDateTime.now());
            viRepo.save(vi);

            // Cập nhật trạng thái yêu cầu
            yeuCau.setTrangThai("Đã duyệt");
            yeuCau.setThoiGianXuLy(LocalDateTime.now());
            rutTienRepo.save(yeuCau);

            // Lưu lịch sử giao dịch
            LichSuGiaoDichVi lichSu = new LichSuGiaoDichVi();
            lichSu.setIdViThanhToan(vi.getId());
            lichSu.setLoaiGiaoDich("Rút tiền");
            lichSu.setSoTien(soTienRut);
            lichSu.setThoiGianGiaoDich(LocalDateTime.now());
            lichSu.setCreatedAt(LocalDateTime.now());
            lichSu.setMaGiaoDich(yeuCau.getMaGiaoDich());
            lichSu.setMoTa("Rút tiền về tài khoản: " + yeuCau.getSoTaiKhoan() + " (" + yeuCau.getTenNganHang() + ")");
            lichSuGiaoDichViRepository.save(lichSu);
        }

        return "redirect:/acvstore/vi/yeu-cau-rut-tien";
    }


    // Từ chối yêu cầu
    @PostMapping("/tu-choi/{id}")
    public String tuChoiRutTien(@PathVariable UUID id, @RequestParam(name = "lyDo", required = false) String lyDo) {
        YeuCauRutTien yeuCau = rutTienRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));

        if (!"Đang chờ".equalsIgnoreCase(yeuCau.getTrangThai())) {
            return "redirect:/acvstore/vi/yeu-cau-rut-tien";
        }

        yeuCau.setTrangThai("Từ chối");
        yeuCau.setGhiChu(lyDo);
        yeuCau.setThoiGianXuLy(LocalDateTime.now());
        rutTienRepo.save(yeuCau);

        // Ghi lịch sử hoàn tiền vào ví
        LichSuGiaoDichVi ls = new LichSuGiaoDichVi();
        ls.setIdViThanhToan(yeuCau.getViThanhToan().getId());
        ls.setLoaiGiaoDich("Hoàn tiền");
        ls.setSoTien(yeuCau.getSoTien());
        ls.setMaGiaoDich(yeuCau.getMaGiaoDich());
        ls.setMoTa("Hoàn tiền vào ví do từ chối yêu cầu rút tiền. Lý do: " + (lyDo != null ? lyDo : "Không rõ"));
        ls.setCreatedAt(LocalDateTime.now());
        ls.setThoiGianGiaoDich(LocalDateTime.now());
        lichSuGiaoDichViRepository.save(ls);


        return "redirect:/acvstore/vi/yeu-cau-rut-tien";
    }
}
