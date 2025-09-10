package com.example.AsmGD1.controller.ViThanhToan;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.ViThanhToan;
import com.example.AsmGD1.service.ViThanhToan.ViThanhToanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/wallet")
public class WalletApiController {

    @Autowired
    private ViThanhToanService viService;

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse> getWalletBalance(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(new ApiResponse(false, null, "Vui lòng đăng nhập."));
            }

            Object principal = authentication.getPrincipal();
            if (!(principal instanceof NguoiDung user)) {
                return ResponseEntity.status(403).body(new ApiResponse(false, null, "Tài khoản không hợp lệ."));
            }

            UUID idNguoiDung = user.getId();
            ViThanhToan vi = viService.findByUser(idNguoiDung);

            if (vi == null) {
                vi = viService.taoViMoi(idNguoiDung);
            }

            return ResponseEntity.ok(new ApiResponse(true, vi.getSoDu(), "Lấy số dư thành công."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponse(false, null, "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    public static class ApiResponse {
        public boolean success;
        public Object data;
        public String message;

        public ApiResponse(boolean success, Object data, String message) {
            this.success = success;
            this.data = data;
            this.message = message;
        }
    }
}
