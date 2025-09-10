package com.example.AsmGD1.controller.ViThanhToan.PayOS;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import vn.payos.PayOS;
import vn.payos.type.Webhook;
import vn.payos.type.WebhookData;

@RestController
@RequestMapping("/wallet-topup")
public class WalletTopupWebhookController {

    @Autowired
    private PayOS payOS;

    @PostMapping("/webhook")
    public ObjectNode handleWebhook(@RequestBody Webhook webhookBody) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode response = mapper.createObjectNode();

        try {
            WebhookData data = payOS.verifyPaymentWebhookData(webhookBody);

            if (data.getDescription() != null && data.getDescription().startsWith("TOPUP:")) {
                long soTien = data.getAmount();
                long orderCode = data.getOrderCode();

                // TODO: Lấy userId từ orderCode (nếu có cơ chế mapping)
                // TODO: Cộng tiền vào ví người dùng

                System.out.println("✅ Nạp thành công " + soTien + " vào ví với mã giao dịch: " + orderCode);
            }

            response.put("error", 0);
            response.put("message", "Webhook processed");
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", -1);
            response.put("message", e.getMessage());
            return response;
        }
    }
}
