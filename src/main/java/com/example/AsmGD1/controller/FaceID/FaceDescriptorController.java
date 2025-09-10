package com.example.AsmGD1.controller.FaceID;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.*;

@RestController
public class FaceDescriptorController {

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @GetMapping("/api/get-descriptor")
    public ResponseEntity<?> getDescriptor(@RequestParam("username") String username) {
        if (username == null || username.trim().isEmpty() || username.equals("unknown")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username không hợp lệ"));
        }

        Optional<NguoiDung> optionalUser = nguoiDungRepository.findByTenDangNhap(username);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy người dùng hoặc đã bị khóa"));
        }

        NguoiDung user = optionalUser.get();
        byte[] bytes = user.getFaceDescriptor();
        if (bytes == null || bytes.length == 0) {
            return ResponseEntity.status(404).body(Map.of("message", "Người dùng chưa đăng ký khuôn mặt"));
        }

        try {
            List<Double> descriptor = new ArrayList<>();
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
            while (dis.available() > 0) {
                descriptor.add((double) dis.readFloat());
            }
            return ResponseEntity.ok(Map.of("descriptors", List.of(descriptor)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Lỗi khi đọc descriptor"));
        }
    }
}