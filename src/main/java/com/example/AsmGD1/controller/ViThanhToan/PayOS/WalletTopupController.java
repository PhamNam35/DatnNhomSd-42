package com.example.AsmGD1.controller.ViThanhToan.PayOS;

import com.example.AsmGD1.entity.ViThanhToan;
import com.example.AsmGD1.service.ViThanhToan.ViThanhToanService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/wallet-topup")
public class WalletTopupController {

    @Autowired
    private PayOS payOS;

    @Autowired
    private ViThanhToanService viThanhToanService;

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmTopup(@RequestBody Map<String, Object> payload) {
        try {
            Object idNguoiDungRaw = payload.get("idNguoiDung");
            Object soTienRaw = payload.get("soTien");

            // Kiểm tra đầu vào
            if (idNguoiDungRaw == null || soTienRaw == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Thiếu tham số idNguoiDung hoặc soTien"
                ));
            }

            UUID idNguoiDung = UUID.fromString(idNguoiDungRaw.toString());
            BigDecimal soTien = new BigDecimal(soTienRaw.toString());

            // ✅ Cộng tiền vào ví
            viThanhToanService.napTien(idNguoiDung, soTien);

            ViThanhToan vi = viThanhToanService.findByUser(idNguoiDung);

            return ResponseEntity.ok(Map.of(
                    "message", "Nạp tiền thành công",
                    "soDuMoi", vi.getSoDu()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Nạp tiền thất bại: " + e.getMessage()));
        }
    }



    @PostMapping(value = "/create", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void createTopupLink(
            @RequestParam("soTien") int soTien,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            String baseUrl = getBaseUrl(request);
            String returnUrl = baseUrl + "/wallet-topup/success";
            String cancelUrl = baseUrl + "/wallet-topup/cancel";

            long orderCode = Long.parseLong(String.valueOf(new Date().getTime()).substring(5)); // lấy 6 số cuối thời gian

            ItemData item = ItemData.builder()
                    .name("Nạp tiền vào ví ACV")
                    .quantity(1)
                    .price(soTien)
                    .build();

            PaymentData paymentData = PaymentData.builder()
                    .orderCode(orderCode)
                    .amount(soTien)
                    .description("TOPUP:" + orderCode)  // dùng để nhận biết trong webhook
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .item(item)
                    .build();

            CheckoutResponseData checkout = payOS.createPaymentLink(paymentData);

            response.setHeader("Location", checkout.getCheckoutUrl());
            response.setStatus(302);
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
        }
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        String url = scheme + "://" + serverName;
        if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("https") && serverPort != 443)) {
            url += ":" + serverPort;
        }
        url += contextPath;
        return url;
    }
}
