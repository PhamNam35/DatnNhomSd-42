package com.example.AsmGD1.controller.HoaDon;

import com.example.AsmGD1.dto.HoaDonDTO;
import com.example.AsmGD1.entity.ChiTietDonHang;
import com.example.AsmGD1.entity.HoaDon;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.HoaDon.HoaDonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/acvstore/tra-hang")
public class TraHangController {

    @Autowired
    private HoaDonService hoaDonService;

    @GetMapping("/{hoaDonId}")
    public String showReturnPage(@PathVariable String hoaDonId, @RequestParam(required = false) String chiTietDonHangId, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String tenDangNhap = authentication.getName();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) auth.getPrincipal();
            model.addAttribute("user", user);
        }
        try {
            UUID uuid = UUID.fromString(hoaDonId);
            HoaDon hoaDon = hoaDonService.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + hoaDonId));

            if (!"Hoàn thành".equals(hoaDon.getTrangThai()) && !"Vận chuyển thành công".equals(hoaDon.getTrangThai()) && !"Đã trả hàng một phần".equals(hoaDon.getTrangThai())) {
                throw new IllegalStateException("Hóa đơn phải ở trạng thái 'Hoàn thành', 'Vận chuyển thành công' hoặc 'Đã trả hàng một phần' để thực hiện trả hàng.");
            }

            HoaDonDTO hoaDonDTO = hoaDonService.getHoaDonDetail(hoaDonId);
            List<ChiTietDonHang> returnableItems = hoaDonService.getReturnableItems(uuid);
            model.addAttribute("hoaDon", hoaDonDTO);
            model.addAttribute("returnableItems", returnableItems);
            model.addAttribute("chiTietDonHangId", chiTietDonHangId != null ? UUID.fromString(chiTietDonHangId) : null);

            return "WebQuanLy/tra-hang";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "ID hóa đơn không hợp lệ: " + hoaDonId);
            return "error";
        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi tải trang trả hàng: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/confirm/{hoaDonId}")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> confirmReturn(@PathVariable String hoaDonId,
                                                             @RequestBody Map<String, Object> request) {
        System.out.println("Received request for hoaDonId: " + hoaDonId);
        System.out.println("Request data: " + request);
        try {
            UUID uuid = UUID.fromString(hoaDonId);
            HoaDon hoaDon = hoaDonService.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + hoaDonId));

            // Cho phép trả hàng khi trạng thái là "Hoàn thành", "Vận chuyển thành công" hoặc "Đã trả hàng một phần"
            if (!"Hoàn thành".equals(hoaDon.getTrangThai()) && !"Vận chuyển thành công".equals(hoaDon.getTrangThai())
                    && !"Đã trả hàng một phần".equals(hoaDon.getTrangThai())) {
                throw new IllegalStateException("Hóa đơn phải ở trạng thái 'Hoàn thành', 'Vận chuyển thành công' hoặc 'Đã trả hàng một phần' để thực hiện trả hàng.");
            }

            @SuppressWarnings("unchecked")
            List<String> chiTietDonHangIds = (List<String>) request.get("chiTietDonHangIds");
            String lyDoTraHang = (String) request.get("lyDoTraHang");

            if (chiTietDonHangIds == null || chiTietDonHangIds.isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn ít nhất một sản phẩm để trả.");
            }
            if (lyDoTraHang == null || lyDoTraHang.trim().isEmpty()) {
                throw new IllegalArgumentException("Lý do trả hàng không được để trống.");
            }

            List<UUID> uuids = chiTietDonHangIds.stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toList());

            hoaDonService.processReturn(uuid, uuids, lyDoTraHang);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Trả hàng thành công.");
            response.put("success", true);
            response.put("hoaDonId", hoaDonId); // Trả về hoaDonId để sử dụng trong redirect
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "success", false));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "success", false));
        } catch (Exception e) {
            System.err.println("Error in confirmReturn: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xử lý trả hàng: " + e.getMessage(), "success", false));
        }
    }
}