package com.example.AsmGD1.controller.ThongBao;

import com.example.AsmGD1.service.ThongBao.ThongBaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test-thong-bao")
public class ThongBaoTestController {

    @Autowired
    private ThongBaoService thongBaoService;

    @GetMapping
    public String testThongBao() {
        thongBaoService.taoThongBaoHeThong(
                "admin", // vai trò đúng với DB (chữ thường nhé)
                "Thông báo test",
                "Đây là nội dung thử nghiệm gửi thông báo.",
                null // không cần đơn hàng
        );
        return "Đã gọi hàm tạo thông báo.";
    }
}