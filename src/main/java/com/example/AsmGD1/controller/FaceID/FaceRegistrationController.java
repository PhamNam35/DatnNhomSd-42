package com.example.AsmGD1.controller.FaceID;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.*;

@RestController
public class FaceRegistrationController {

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    private static final int EMBEDDING_SIZE = 128;
    private static final double SAME_PERSON_THRESHOLD = 0.60; // điều chỉnh nếu cần

    @PostMapping("/api/register-descriptor")
    public ResponseEntity<?> registerFace(@RequestBody Map<String, Object> payload, Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Bạn chưa đăng nhập"));
        }

        String username = auth.getName();

        Object raw = payload.get("descriptor");
        if (!(raw instanceof List<?> rawList) || rawList.size() != EMBEDDING_SIZE) {
            return ResponseEntity.badRequest().body(Map.of("message", "Dữ liệu không hợp lệ (descriptor 128 chiều)"));
        }

        // Ép kiểu an toàn -> float[128]
        float[] newDesc = new float[EMBEDDING_SIZE];
        for (int i = 0; i < EMBEDDING_SIZE; i++) {
            Object v = rawList.get(i);
            if (!(v instanceof Number num)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Descriptor không hợp lệ"));
            }
            newDesc[i] = num.floatValue();
        }

        Optional<NguoiDung> optionalUser = nguoiDungRepository.findByTenDangNhap(username);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy người dùng hoặc bị khóa"));
        }

        NguoiDung me = optionalUser.get();

        // (a) Tài khoản đã có khuôn mặt?
        if (Boolean.TRUE.equals(me.getFaceRegistered()) && me.getFaceDescriptor() != null && me.getFaceDescriptor().length > 0) {
            return ResponseEntity.status(409).body(Map.of(
                    "message", "Tài khoản này đã đăng ký khuôn mặt trước đó"
            ));
        }

        // (b) So với tất cả user khác đã có descriptor
        List<NguoiDung> others = nguoiDungRepository.findByFaceDescriptorIsNotNullAndFaceRegisteredTrue();
        for (NguoiDung other : others) {
            if (other.getId().equals(me.getId())) continue; // bỏ qua chính mình (phòng trường hợp DB đã có)
            float[] otherDesc = decodeFloat128(other.getFaceDescriptor());
            double dist = l2(newDesc, otherDesc);
            if (dist < SAME_PERSON_THRESHOLD) {
                // KHÔNG tiết lộ tài khoản cụ thể vì lý do bảo mật
                return ResponseEntity.status(409).body(Map.of(
                        "message", "Khuôn mặt này đã được đăng ký cho một tài khoản khác"
                ));
            }
        }

        // (c) Lưu descriptor cho tài khoản hiện tại
        me.setFaceDescriptor(encodeFloat(newDesc));
        me.setFaceRegistered(true);
        nguoiDungRepository.save(me);

        return ResponseEntity.ok(Map.of("message", "Đăng ký khuôn mặt thành công"));
    }

    // ===== Helpers =====
    private static float[] decodeFloat128(byte[] bytes) {
        float[] out = new float[EMBEDDING_SIZE];
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes))) {
            for (int i = 0; i < EMBEDDING_SIZE; i++) {
                out[i] = dis.readFloat();
            }
        } catch (IOException e) {
            // Nếu dữ liệu trong DB không đủ 128*4 byte -> ném lỗi rõ ràng
            throw new IllegalStateException("Descriptor trong DB không hợp lệ", e);
        }
        return out;
    }

    private static byte[] encodeFloat(float[] arr) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            for (float v : arr) dos.writeFloat(v);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Không thể encode descriptor", e);
        }
    }

    private static double l2(float[] a, float[] b) {
        double s = 0;
        for (int i = 0; i < EMBEDDING_SIZE; i++) {
            double d = a[i] - b[i];
            s += d * d;
        }
        return Math.sqrt(s);
        // Nếu muốn cosine distance: cos = dot(a,b)/(||a||*||b||) rồi dùng 1 - cos
    }
}
