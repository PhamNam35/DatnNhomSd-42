package com.example.AsmGD1.service.ViThanhToan;

import com.example.AsmGD1.entity.ViThanhToan;
import com.example.AsmGD1.entity.YeuCauRutTien;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.repository.ViThanhToan.ViThanhToanRepository;
import com.example.AsmGD1.repository.ViThanhToan.YeuCauRutTienRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class YeuCauRutTienService {

    @Autowired
    private YeuCauRutTienRepository yeuCauRutTienRepository;

    @Autowired
    private ViThanhToanRepository viThanhToanRepository;

    @Autowired
    private NguoiDungRepository nguoiDungRepository; // ✅ Thêm repo này

    public YeuCauRutTien taoYeuCauRutTien(UUID idNguoiDung, BigDecimal soTien) {
        // Lấy entity người dùng từ UUID
        var nguoiDung = nguoiDungRepository.findById(idNguoiDung)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Tìm ví theo người dùng
        ViThanhToan vi = viThanhToanRepository.findByNguoiDung(nguoiDung)
                .orElseThrow(() -> new RuntimeException("Ví không tồn tại"));

        if (vi.getSoDu().compareTo(soTien) < 0) {
            throw new RuntimeException("Số dư không đủ để rút");
        }

        // Tạo yêu cầu rút tiền
        YeuCauRutTien yeuCau = new YeuCauRutTien();
        yeuCau.setViThanhToan(vi);
        yeuCau.setSoTien(soTien);

        return yeuCauRutTienRepository.save(yeuCau);
    }
}
