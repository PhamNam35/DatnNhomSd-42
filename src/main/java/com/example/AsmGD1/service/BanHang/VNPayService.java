package com.example.AsmGD1.service.BanHang;

import com.example.AsmGD1.dto.BanHang.DonHangDTO;
import org.springframework.stereotype.Service;

@Service
public class VNPayService {
    public String createPaymentUrl(DonHangDTO orderDTO) {
        // Implement logic to generate VNPay payment URL
        // Sử dụng thư viện VNPay SDK
        return "https://sandbox.vnpay.vn/payment_url";
    }
}