package com.example.AsmGD1.dto.ThongBao;

import com.example.AsmGD1.entity.ChiTietThongBaoNhom;
import lombok.Data;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Data
public class ThongBaoDTO {
    private UUID idChiTietThongBao; // Đổi tên để rõ ràng hơn
    private String tieuDe;
    private String noiDung;
    private String thoiGian;
    private boolean daXem;

    public ThongBaoDTO(ChiTietThongBaoNhom tb) {
        this.idChiTietThongBao = tb.getId(); // Ánh xạ ID của ChiTietThongBaoNhom
        this.tieuDe = tb.getThongBaoNhom().getTieuDe();
        this.noiDung = tb.getThongBaoNhom().getNoiDung();
        this.thoiGian = tb.getThongBaoNhom().getThoiGianTao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        this.daXem = tb.isDaXem();
    }
}