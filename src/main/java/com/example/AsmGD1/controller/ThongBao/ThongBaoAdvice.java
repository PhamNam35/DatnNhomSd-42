 package com.example.AsmGD1.controller.ThongBao;

import com.example.AsmGD1.dto.ThongBao.ThongBaoDTO;
import com.example.AsmGD1.entity.ChiTietThongBaoNhom;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.service.ThongBao.ThongBaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ControllerAdvice
public class ThongBaoAdvice {

    private static final Logger logger = LoggerFactory.getLogger(ThongBaoAdvice.class);

    @Autowired
    private ThongBaoService thongBaoService;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @ModelAttribute
    public void thongBaoChoTatCaTrang(Model model, Principal principal) {
        try {
            if (principal != null) {
                Optional<NguoiDung> nguoiDungOpt = nguoiDungRepository.findByTenDangNhap(principal.getName());

                if (nguoiDungOpt.isPresent()) {
                    NguoiDung nguoiDung = nguoiDungOpt.get();
                    logger.info("Đã đăng nhập với người dùng: {}", nguoiDung.getTenDangNhap());

                    // Lấy 5 thông báo mới nhất
                    List<ChiTietThongBaoNhom> danhSach = thongBaoService.lay5ThongBaoMoiNhat(nguoiDung.getId());
                    List<ThongBaoDTO> dtoList = danhSach.stream()
                            .map(ThongBaoDTO::new)
                            .collect(Collectors.toList());

                    model.addAttribute("user", nguoiDung);
                    model.addAttribute("unreadCount", thongBaoService.demSoThongBaoChuaXem(nguoiDung.getId()));
                    model.addAttribute("notifications", dtoList);
                } else {
                    logger.warn("Không tìm thấy người dùng với tên đăng nhập: {}", principal.getName());
                    model.addAttribute("user", null);
                    model.addAttribute("unreadCount", 0);
                    model.addAttribute("notifications", List.of());
                }
            } else {
                logger.debug("Chưa đăng nhập (Principal null)");
                model.addAttribute("user", null);
                model.addAttribute("unreadCount", 0);
                model.addAttribute("notifications", List.of());
            }
        } catch (Exception e) {
            logger.error("Lỗi khi tải thông báo cho tất cả trang", e);
            model.addAttribute("user", null);
            model.addAttribute("unreadCount", 0);
            model.addAttribute("notifications", List.of());
        }
    }
}
