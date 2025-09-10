package com.example.AsmGD1.controller.ViThanhToan.PayOS;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WalletTopupCallbackController {

    @GetMapping("/wallet-topup/success")
    public String handleSuccess() {
        // ✅ Hiển thị HTML "Nạp tiền thành công"
        return "wallet/success";
    }

    @GetMapping("/wallet-topup/cancel")
    public String handleCancel() {
        // ❌ Hiển thị HTML "Nạp tiền thất bại"
        return "wallet/cancel";
    }
}
