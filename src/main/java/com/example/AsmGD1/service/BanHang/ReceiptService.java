package com.example.AsmGD1.service.BanHang;

import org.springframework.stereotype.Service;

@Service
public class ReceiptService {
    public byte[] generateReceipt(String orderCode) {
        // Implement PDF generation using iText or similar library
        return new byte[]{};
    }
}