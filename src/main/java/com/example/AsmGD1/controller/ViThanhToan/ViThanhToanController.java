package com.example.AsmGD1.controller.ViThanhToan;

import com.example.AsmGD1.dto.ViThanhToan.LichSuGiaoDichViDTO;
import com.example.AsmGD1.entity.LichSuGiaoDichVi;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.ViThanhToan;
import com.example.AsmGD1.entity.YeuCauRutTien;
import com.example.AsmGD1.repository.ViThanhToan.LichSuGiaoDichViRepository;
import com.example.AsmGD1.repository.ViThanhToan.YeuCauRutTienRepository;
import com.example.AsmGD1.service.ThongBao.ThongBaoService;
import com.example.AsmGD1.service.ViThanhToan.ViThanhToanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/vi")
public class ViThanhToanController {

    @Autowired
    private ViThanhToanService viService;

    @Autowired
    private LichSuGiaoDichViRepository lichSuRepo;

    @Autowired
    private YeuCauRutTienRepository yeuCauRutTienRepository;
    @Autowired
    private ThongBaoService thongBaoService;

    // Trang xem ví
    @GetMapping
    public String xemVi(Model model, Authentication authentication) {
        NguoiDung currentUser = (NguoiDung) authentication.getPrincipal();
        UUID idNguoiDung = currentUser.getId();

        ViThanhToan vi = viService.findByUser(idNguoiDung);
        model.addAttribute("user", currentUser);
        model.addAttribute("vi", vi);

        if (vi != null) {
            BigDecimal tongDangCho = viService.tongTienDangCho(vi.getId());
            BigDecimal soDuKhaDung = vi.getSoDu().subtract(tongDangCho);

            model.addAttribute("soDuKhaDung", soDuKhaDung);
            model.addAttribute("tongDangCho", tongDangCho);
        }

        return "WebKhachHang/xem_vi";
    }


    // POST tạo ví nếu chưa có
    @PostMapping("/tao-vi")
    public String taoVi(Authentication authentication, RedirectAttributes attrs) {
        NguoiDung currentUser = (NguoiDung) authentication.getPrincipal();
        UUID idNguoiDung = currentUser.getId();

        ViThanhToan vi = viService.findByUser(idNguoiDung);
        if (vi == null) {
            viService.taoViMoi(idNguoiDung);
            attrs.addFlashAttribute("msg", "Tạo ví mới thành công!");

            // Tạo thông báo cho admin
            thongBaoService.taoThongBaoHeThong(
                    "admin",
                    "Khách hàng tạo ví mới",
                    "Khách hàng " + currentUser.getHoTen() + " (" + currentUser.getEmail() + ") đã tạo ví mới."
            );

        } else {
            attrs.addFlashAttribute("msg", "Bạn đã có ví.");
        }

        return "redirect:/vi";
    }


    // POST nạp tiền
    @PostMapping("/nap-tien")
    public String napTien(
            @RequestParam BigDecimal soTien,
            Authentication authentication,
            RedirectAttributes attrs) {

        NguoiDung currentUser = (NguoiDung) authentication.getPrincipal();
        UUID idNguoiDung = currentUser.getId();

        viService.napTien(idNguoiDung, soTien);
        attrs.addFlashAttribute("msg", "Nạp tiền thành công");

        // Tạo thông báo cho admin
        thongBaoService.taoThongBaoHeThong(
                "admin",
                "Khách hàng nạp tiền",
                "Khách hàng " + currentUser.getHoTen() + " (" + currentUser.getEmail() + ") đã nạp tiền: " + soTien + " VNĐ."
        );

        return "redirect:/vi";
    }


    // GET xem lịch sử ví
    @GetMapping("/lich-su")
    public String lichSuVi(Model model, Authentication authentication) {
        NguoiDung currentUser = (NguoiDung) authentication.getPrincipal();
        UUID idNguoiDung = currentUser.getId();

        ViThanhToan vi = viService.findByUser(idNguoiDung);

        if (vi == null) {
            model.addAttribute("msg", "Bạn chưa có ví để xem lịch sử.");
            return "redirect:/vi";
        }

        List<LichSuGiaoDichVi> lichSu = lichSuRepo.findByIdViThanhToanOrderByCreatedAtDesc(vi.getId());
        List<LichSuGiaoDichViDTO> lichSuDTOs = lichSu.stream().map(giaoDich -> {
            LichSuGiaoDichViDTO dto = new LichSuGiaoDichViDTO();
            dto.setId(giaoDich.getId());
            dto.setIdViThanhToan(giaoDich.getIdViThanhToan());
            dto.setIdDonHang(giaoDich.getIdDonHang());
            dto.setLoaiGiaoDich(giaoDich.getLoaiGiaoDich());
            dto.setSoTien(giaoDich.getSoTien());
            dto.setThoiGianGiaoDich(giaoDich.getThoiGianGiaoDich());
            dto.setMoTa(giaoDich.getMoTa());
            dto.setMaGiaoDich(giaoDich.getMaGiaoDich());
            dto.setCreatedAt(giaoDich.getCreatedAt());

            // Lấy anhBangChung từ YeuCauRutTien dựa trên maGiaoDich
            if ("Rút tiền".equals(giaoDich.getLoaiGiaoDich())) {
                Optional<YeuCauRutTien> yeuCau = yeuCauRutTienRepository.findByMaGiaoDich(giaoDich.getMaGiaoDich());
                yeuCau.ifPresent(rutTien -> {
                    String fileName = rutTien.getAnhBangChung();
                    if (fileName != null && !fileName.isEmpty()) {
                        dto.setAnhBangChung("/uploads/bang-chung/" + fileName); // Thêm tiền tố đúng
                    }
                });
            }

            return dto;
        }).toList();

        model.addAttribute("lichSu", lichSuDTOs);
        model.addAttribute("user", currentUser);
        return "WebKhachHang/lich_su";
    }
}