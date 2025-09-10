package com.example.AsmGD1.controller.FaceID;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
public class FaceVerificationController {

    @PostMapping("/acvstore/verify-success")
    @ResponseBody
    public ResponseEntity<?> setFaceVerified(HttpSession session, Authentication auth) {
        if (auth == null) {
            System.out.println("⚠️ Không có thông tin đăng nhập trong request.");
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }

        String username = auth.getName();
        session.setAttribute("faceVerified", true);
        System.out.println("✅ Xác minh thành công cho: " + username);
        return ResponseEntity.ok(Map.of("success", true));
    }


}

