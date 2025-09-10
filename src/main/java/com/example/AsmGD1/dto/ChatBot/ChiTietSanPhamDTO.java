package com.example.AsmGD1.dto.ChatBot;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class ChiTietSanPhamDTO {
    private UUID id;
    private String kichCo;
    private String mauSac;
    private String chatLieu;
    private String xuatXu;
    private BigDecimal gia;
    private Integer soLuongTonKho;
    private List<String> hinhAnhUrls;
}

