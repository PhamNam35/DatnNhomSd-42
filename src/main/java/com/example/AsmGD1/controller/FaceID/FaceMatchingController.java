package com.example.AsmGD1.controller.FaceID;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.ByteBuffer;
import java.util.*;

@RestController
public class FaceMatchingController {

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @PostMapping("/api/verify-descriptor")
    public ResponseEntity<?> verifyFace(@RequestBody Map<String, Object> payload, Authentication auth, HttpSession session) {
        String username = auth.getName();
        List<Double> incomingDescriptor = (List<Double>) payload.get("descriptor");

        if (username == null || incomingDescriptor == null || incomingDescriptor.size() != 128) {
            return ResponseEntity.badRequest().body(Map.of("message", "Dữ liệu không hợp lệ"));
        }

        Optional<NguoiDung> optionalUser = nguoiDungRepository.findByTenDangNhap(username);
        if (optionalUser.isEmpty() || optionalUser.get().getFaceDescriptor() == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Người dùng chưa đăng ký khuôn mặt"));
        }

        byte[] savedBytes = optionalUser.get().getFaceDescriptor();
        float[] savedDescriptor = new float[128];
        ByteBuffer.wrap(savedBytes).asFloatBuffer().get(savedDescriptor);

        // So sánh
        double distance = 0.0;
        for (int i = 0; i < 128; i++) {
            double diff = incomingDescriptor.get(i) - savedDescriptor[i];
            distance += diff * diff;
        }
        distance = Math.sqrt(distance);

        if (distance < 0.6) {
            session.setAttribute("faceVerified", true); // ✅ Chỉ set khi đúng
            return ResponseEntity.ok(Map.of("match", true, "message", "Xác minh khuôn mặt thành công"));
        } else {
            session.setAttribute("faceVerified", false); // ❌ Sai thì đảm bảo là false
            return ResponseEntity.status(401).body(Map.of("match", false, "message", "Khuôn mặt không khớp"));
        }
    }

}