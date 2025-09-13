package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.service.SanPham.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/acvstore/chi-tiet-san-pham")
public class QuickAddAttributeController {

    @Autowired private XuatXuService xuatXuService;
    @Autowired private ChatLieuService chatLieuService;
    @Autowired private KieuDangService kieuDangService;
    @Autowired private ThuongHieuService thuongHieuService;
    @Autowired private DayGiayService dayGiayService;

    @PostMapping("/save-auto-origin")
    public ResponseEntity<Map<String, Object>> saveOrigin(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        XuatXu entity = new XuatXu();
        entity.setTenXuatXu(name);
        xuatXuService.saveXuatXu(entity);
        return ResponseEntity.ok(Map.of("id", entity.getId(), "name", entity.getTenXuatXu()));
    }

    @PostMapping("/save-auto-material")
    public ResponseEntity<Map<String, Object>> saveMaterial(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        ChatLieu entity = new ChatLieu();
        entity.setTenChatLieu(name);
        chatLieuService.saveChatLieu(entity);
        return ResponseEntity.ok(Map.of("id", entity.getId(), "name", entity.getTenChatLieu()));
    }

    @PostMapping("/save-auto-style")
    public ResponseEntity<Map<String, Object>> saveStyle(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        KieuDang entity = new KieuDang();
        entity.setTenKieuDang(name);
        kieuDangService.saveKieuDang(entity);
        return ResponseEntity.ok(Map.of("id", entity.getId(), "name", entity.getTenKieuDang()));
    }


    @PostMapping("/save-auto-brand")
    public ResponseEntity<Map<String, Object>> saveBrand(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        ThuongHieu entity = new ThuongHieu();
        entity.setTenThuongHieu(name);
        thuongHieuService.saveThuongHieu(entity);
        return ResponseEntity.ok(Map.of("id", entity.getId(), "name", entity.getTenThuongHieu()));
    }

    @PostMapping("/save-auto-dayGiay")
    public ResponseEntity<Map<String, Object>> saveDayGiay(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        DayGiay entity = new DayGiay();
        entity.setTenDayGiay(name);
        dayGiayService.saveDayGiay(entity);
        return ResponseEntity.ok(Map.of("id", entity.getId(), "name", entity.getTenDayGiay()));
    }
}
