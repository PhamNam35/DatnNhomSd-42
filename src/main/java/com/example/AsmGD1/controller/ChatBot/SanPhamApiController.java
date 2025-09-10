package com.example.AsmGD1.controller.ChatBot;

import com.example.AsmGD1.dto.ChatBot.SanPhamWithChiTietDTO;
import com.example.AsmGD1.dto.SanPham.SanPhamDto;
import com.example.AsmGD1.service.SanPham.SanPhamService;
import com.example.AsmGD1.service.WebKhachHang.KhachhangSanPhamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/san-pham")
public class SanPhamApiController {

    @Autowired
    private SanPhamService sanPhamService;

    @Autowired
    private KhachhangSanPhamService khachhangSanPhamService;

    @GetMapping
    public List<SanPhamDto> getAllSanPham() {
        return sanPhamService.getAllSanPhamDtos();
    }

    @GetMapping("/{id}")
    public SanPhamDto getById(@PathVariable("id") UUID id) {
        return sanPhamService.getAllSanPhamDtos()
                .stream().filter(sp -> sp.getId().equals(id)).findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
    }

    @GetMapping("/with-chi-tiet")
    public List<SanPhamWithChiTietDTO> getSanPhamWithChiTiet() {
        return sanPhamService.getSanPhamWithChiTietDTOs();
    }

    @GetMapping("/moi-nhat")
    public List<SanPhamDto> getSanPhamMoiNhat() {
        return khachhangSanPhamService.getSanPhamMoiNhatDtos(); // ✅ đúng service
    }

    @GetMapping("/ban-chay")
    public List<SanPhamDto> getSanPhamBanChay() {
        return khachhangSanPhamService.getSanPhamBanChayDtos();
    }

}

