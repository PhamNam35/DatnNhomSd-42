package com.example.AsmGD1.controller.ThongBao;

import com.example.AsmGD1.dto.ThongBao.ThongBaoDTO;
import com.example.AsmGD1.entity.ChiTietThongBaoNhom;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.repository.ThongBao.ChiTietThongBaoNhomRepository;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.ThongBao.ThongBaoService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/acvstore/thong-bao")
public class ThongBaoController {

    @Autowired
    private ThongBaoService thongBaoService;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private NguoiDungService nguoiDungService;

    @Autowired
    private ChiTietThongBaoNhomRepository chiTietThongBaoNhomRepository;

    // ===========================================================
    // SSE HUB: giữ emitter theo userId để đẩy số chưa đọc realtime
    // ===========================================================
    private static final Map<UUID, List<SseEmitter>> EMITTERS = new ConcurrentHashMap<>();

    private static void removeEmitter(UUID userId, SseEmitter em) {
        List<SseEmitter> list = EMITTERS.get(userId);
        if (list != null) list.remove(em);
    }

    /** Cho phép Service hoặc các endpoint khác đẩy số mới xuống client */
    public static void pushUnread(UUID userId, long unread) {
        List<SseEmitter> list = EMITTERS.getOrDefault(userId, Collections.emptyList());
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter em : list) {
            try {
                em.send(SseEmitter.event()
                        .name("message")
                        .data("{\"type\":\"count\",\"unreadCount\":" + unread + "}"));
            } catch (Exception e) {
                dead.add(em);
            }
        }
        list.removeAll(dead);
    }

    /** API đếm nhanh – dùng để sync lần đầu và fallback khi mất SSE */
    @GetMapping("/count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUnreadCount(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Chưa đăng nhập"));
        }
        Optional<NguoiDung> opt = nguoiDungRepository.findByTenDangNhap(principal.getName());
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Không tìm thấy người dùng"));
        }
        long unread = thongBaoService.demSoThongBaoChuaXem(opt.get().getId());
        return ResponseEntity.ok(Map.of("unreadCount", unread));
    }

    /** Kết nối SSE – client mở 1 lần, mọi thay đổi sẽ tự đẩy xuống */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter stream(Principal principal) {
        if (principal == null) return new SseEmitter(0L);
        Optional<NguoiDung> opt = nguoiDungRepository.findByTenDangNhap(principal.getName());
        if (opt.isEmpty()) return new SseEmitter(0L);

        UUID userId = opt.get().getId();
        SseEmitter emitter = new SseEmitter(0L); // không timeout
        EMITTERS.computeIfAbsent(userId, k -> Collections.synchronizedList(new ArrayList<>())).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(e -> removeEmitter(userId, emitter));

        // Gửi số hiện tại ngay khi kết nối
        try {
            long unread = thongBaoService.demSoThongBaoChuaXem(userId);
            emitter.send(SseEmitter.event().name("message")
                    .data("{\"type\":\"count\",\"unreadCount\":" + unread + "}"));
        } catch (Exception ignored) {}

        return emitter;
    }

    // ============================== CÁC API SẴN CÓ ==============================

    @GetMapping("/load")
    @ResponseBody
    public ResponseEntity<?> taiDanhSachThongBao(
            Authentication authentication,
            @RequestParam(value = "unread", defaultValue = "false") boolean unread,
            HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Người dùng chưa đăng nhập");
        }

        String username = authentication.getName();
        Optional<NguoiDung> optionalNguoiDung = nguoiDungRepository.findByTenDangNhap(username);
        if (!optionalNguoiDung.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Không tìm thấy người dùng");
        }

        NguoiDung user = optionalNguoiDung.get();
        List<ChiTietThongBaoNhom> danhSach;
        if (unread) {
            danhSach = thongBaoService.lay5ThongBaoChuaXem(user.getId());
        } else {
            danhSach = thongBaoService.lay5ThongBaoMoiNhat(user.getId());
        }

        List<ThongBaoDTO> dtoList = danhSach.stream()
                .map(ThongBaoDTO::new)
                .limit(5)
                .collect(Collectors.toList());

        long soChuaDoc = thongBaoService.demSoThongBaoChuaXem(user.getId());

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("notifications", dtoList);
        responseMap.put("unreadCount", soChuaDoc);

        return ResponseEntity.ok(responseMap);
    }

    @GetMapping("/xem")
    @ResponseBody
    public ResponseEntity<?> hienThiThongBao(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "") String status,
            HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        Map<String, Object> responseMap = new HashMap<>();
        Optional<NguoiDung> optionalNguoiDung = nguoiDungRepository.findByTenDangNhap(principal.getName());

        if (optionalNguoiDung.isPresent()) {
            NguoiDung nguoiDung = optionalNguoiDung.get();
            List<ChiTietThongBaoNhom> notifications = thongBaoService.layThongBaoTheoNguoiDungVaTrangThai(nguoiDung.getId(), page, size, status);
            long totalCount = thongBaoService.demTongSoThongBaoTheoTrangThai(nguoiDung.getId(), status);

            List<ThongBaoDTO> dtoList = notifications.stream()
                    .map(ThongBaoDTO::new)
                    .collect(Collectors.toList());

            responseMap.put("notifications", dtoList);
            responseMap.put("unreadCount", thongBaoService.demSoThongBaoChuaXem(nguoiDung.getId()));
            responseMap.put("totalCount", totalCount);
            responseMap.put("user", nguoiDung);
            return ResponseEntity.ok(responseMap);
        } else {
            responseMap.put("notifications", null);
            responseMap.put("unreadCount", 0);
            responseMap.put("totalCount", 0);
            responseMap.put("user", null);
            return ResponseEntity.ok(responseMap);
        }
    }

    @PostMapping("/danh-dau-da-xem")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> danhDauThongBaoDaXem(@RequestParam UUID idChiTietThongBao, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (principal == null) {
                response.put("error", "Người dùng chưa đăng nhập.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            Optional<NguoiDung> optionalNguoiDung = nguoiDungRepository.findByTenDangNhap(principal.getName());
            if (!optionalNguoiDung.isPresent()) {
                response.put("error", "Không tìm thấy người dùng.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            NguoiDung nguoiDung = optionalNguoiDung.get();
            thongBaoService.danhDauDaXem(idChiTietThongBao, nguoiDung.getId());
            long unreadCount = thongBaoService.demSoThongBaoChuaXem(nguoiDung.getId());

            // 🔔 đẩy realtime
            ThongBaoController.pushUnread(nguoiDung.getId(), unreadCount);

            response.put("unreadCount", unreadCount);
            response.put("message", "Đã đánh dấu là đã đọc thành công");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("error", "Lỗi server: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/danh-dau-tat-ca")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> danhDauTatCaThongBaoDaXem(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<NguoiDung> optionalNguoiDung = nguoiDungRepository.findByTenDangNhap(principal.getName());
            if (!optionalNguoiDung.isPresent()) {
                response.put("error", "Không tìm thấy người dùng.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            NguoiDung nguoiDung = optionalNguoiDung.get();
            thongBaoService.danhDauTatCaDaXem(nguoiDung.getId());
            long unreadCount = thongBaoService.demSoThongBaoChuaXem(nguoiDung.getId());

            // 🔔 đẩy realtime
            ThongBaoController.pushUnread(nguoiDung.getId(), unreadCount);

            response.put("unreadCount", unreadCount);
            response.put("message", "Đã đánh dấu tất cả thông báo là đã đọc thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Lỗi server: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/debug/thong-bao")
    @ResponseBody
    public List<ChiTietThongBaoNhom> debugThongBao(Principal principal) {
        Optional<NguoiDung> optionalNguoiDung = nguoiDungRepository.findByTenDangNhap(principal.getName());
        if (optionalNguoiDung.isPresent()) {
            return chiTietThongBaoNhomRepository.findByNguoiDungIdOrderByThongBaoNhom_ThoiGianTaoDesc(optionalNguoiDung.get().getId());
        }
        return List.of();
    }

    @PostMapping("/danh-dau-chua-xem")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> danhDauChuaXem(@RequestParam UUID idChiTietThongBao, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (principal == null) {
                response.put("error", "Người dùng chưa đăng nhập.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Optional<NguoiDung> optionalNguoiDung = nguoiDungRepository.findByTenDangNhap(principal.getName());
            if (!optionalNguoiDung.isPresent()) {
                response.put("error", "Không tìm thấy người dùng.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            NguoiDung nguoiDung = optionalNguoiDung.get();
            thongBaoService.danhDauChuaXem(idChiTietThongBao, nguoiDung.getId());
            long unreadCount = thongBaoService.demSoThongBaoChuaXem(nguoiDung.getId());

            // 🔔 đẩy realtime
            ThongBaoController.pushUnread(nguoiDung.getId(), unreadCount);

            response.put("unreadCount", unreadCount);
            response.put("message", "Đã đánh dấu là chưa xem thành công");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("error", "Lỗi server: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/xoa")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> xoaMotThongBao(@RequestParam UUID idChiTietThongBao, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (principal == null) {
                response.put("error", "Người dùng chưa đăng nhập.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Optional<NguoiDung> nguoiDungOpt = nguoiDungRepository.findByTenDangNhap(principal.getName());
            if (nguoiDungOpt.isEmpty()) {
                response.put("error", "Không tìm thấy người dùng.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            thongBaoService.xoaThongBao(idChiTietThongBao, nguoiDungOpt.get().getId());
            long unreadCount = thongBaoService.demSoThongBaoChuaXem(nguoiDungOpt.get().getId());

            // 🔔 đẩy realtime
            ThongBaoController.pushUnread(nguoiDungOpt.get().getId(), unreadCount);

            response.put("message", "Đã xóa thông báo.");
            response.put("unreadCount", unreadCount);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("error", "Lỗi server: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/xoa-tat-ca")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> xoaTatCaThongBao(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (principal == null) {
                response.put("error", "Người dùng chưa đăng nhập.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Optional<NguoiDung> nguoiDungOpt = nguoiDungRepository.findByTenDangNhap(principal.getName());
            if (nguoiDungOpt.isEmpty()) {
                response.put("error", "Không tìm thấy người dùng.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            thongBaoService.xoaTatCaThongBao(nguoiDungOpt.get().getId());

            // 🔔 đẩy realtime = 0
            ThongBaoController.pushUnread(nguoiDungOpt.get().getId(), 0L);

            response.put("message", "Đã xóa toàn bộ thông báo.");
            response.put("unreadCount", 0);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Lỗi server: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
