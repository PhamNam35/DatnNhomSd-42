package com.example.AsmGD1.controller.HoaDon;

import com.example.AsmGD1.dto.HoaDonDTO;
import com.example.AsmGD1.entity.DiaChiNguoiDung;
import com.example.AsmGD1.entity.DonHang;
import com.example.AsmGD1.entity.HoaDon;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.BanHang.DonHangRepository;
import com.example.AsmGD1.service.HoaDon.HoaDonService;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/acvstore/hoa-don")
public class HoaDonController {

    @Autowired
    private HoaDonService hoaDonService;

    @Autowired
    private DonHangRepository donHangRepository;

    @Autowired
    private NguoiDungService nguoiDungService;

    @GetMapping
    public String hienThiTrangHoaDon(Model model,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "5") int size,
                                     @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<HoaDon> hoaDonPage = hoaDonService.findAll(search, null, null, null, pageable);
        model.addAttribute("hoaDonPage", hoaDonPage);
        model.addAttribute("search", search);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String tenDangNhap = authentication.getName();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) auth.getPrincipal();
            model.addAttribute("user", user);
        }

        return "WebQuanLy/hoa-don";
    }

    @GetMapping("/list")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getHoaDonList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String salesMethod) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<HoaDon> hoaDonPage = hoaDonService.findAll(search, trangThai, paymentMethod, salesMethod, pageable);

            List<Map<String, Object>> hoaDonList = hoaDonPage.getContent().stream()
                    .map(row -> {
                        Map<String, Object> hoaDonInfo = new HashMap<>();
                        hoaDonInfo.put("id", row.getId());
                        hoaDonInfo.put("maHoaDon", row.getDonHang().getMaDonHang());
                        hoaDonInfo.put("tenKhachHang", row.getNguoiDung() != null ? row.getNguoiDung().getHoTen() : "Khách lẻ");
                        hoaDonInfo.put("tenNhanVien", row.getNhanVien() != null ? row.getNhanVien().getHoTen() : "Không rõ");
                        hoaDonInfo.put("tongTien", row.getTongTien());
                        hoaDonInfo.put("thoiGianTao", row.getNgayTao());
                        hoaDonInfo.put("phuongThucThanhToan", row.getPhuongThucThanhToan() != null ? row.getPhuongThucThanhToan().getTenPhuongThuc() : "Chưa chọn");
                        hoaDonInfo.put("trangThai", hoaDonService.getCurrentStatus(row));
                        hoaDonInfo.put("phuongThucBanHang", row.getDonHang().getPhuongThucBanHang());
                        return hoaDonInfo;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("hoaDon", hoaDonList);
            response.put("totalPages", hoaDonPage.getTotalPages());
            response.put("totalElements", hoaDonPage.getTotalElements());
            response.put("currentPage", page);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi lấy danh sách hóa đơn: " + e.getMessage()));
        }
    }

    @GetMapping("/detail/{id}")
    public String getHoaDonDetail(@PathVariable String id, Model model) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID hóa đơn không được để trống.");
            }
            UUID uuid = UUID.fromString(id);
            HoaDon hoaDon = hoaDonService.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + id));

//            if ("Tại quầy".equalsIgnoreCase(hoaDon.getDonHang().getPhuongThucBanHang()) &&
//                    !"Hoàn thành".equals(hoaDon.getTrangThai())) {
//                hoaDonService.updateStatus(uuid, "Hoàn thành", "Hoàn thành tự động (Tại quầy)", true);
//            }

            model.addAttribute("hoaDon", hoaDon);
            model.addAttribute("currentStatus", hoaDonService.getCurrentStatus(hoaDon));
            return "WebQuanLy/hoa-don-detail";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi tải chi tiết hóa đơn: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/status/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getOrderStatus(@PathVariable UUID id) {
        HoaDon hoaDon = hoaDonService.findById(id)
                .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + id));
        Map<String, String> response = new HashMap<>();
        response.put("currentStatus", hoaDonService.getCurrentStatus(hoaDon));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteHoaDon(@PathVariable String id) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID hóa đơn không được để trống.");
            }
            UUID uuid = UUID.fromString(id);
            hoaDonService.deleteHoaDon(uuid);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Hóa đơn đã được xóa thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xóa hóa đơn: " + e.getMessage()));
        }
    }

    @PostMapping("/update/{id}")
    @ResponseBody
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> updateHoaDon(@PathVariable String id, @RequestBody Map<String, String> request) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID hóa đơn không được để trống.");
            }
            UUID uuid = UUID.fromString(id);
            HoaDon hoaDon = hoaDonService.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + id));

            String hoTen = request.get("hoTen");
            String soDienThoai = request.get("soDienThoai");
            String tinhThanhPho = request.get("tinhThanhPho");
            String quanHuyen = request.get("quanHuyen");
            String phuongXa = request.get("phuongXa");
            String chiTietDiaChi = request.get("chiTietDiaChi");

            if (hoTen == null || hoTen.trim().isEmpty() ||
                    soDienThoai == null || soDienThoai.trim().isEmpty() ||
                    tinhThanhPho == null || tinhThanhPho.trim().isEmpty() ||
                    quanHuyen == null || quanHuyen.trim().isEmpty() ||
                    phuongXa == null || phuongXa.trim().isEmpty() ||
                    chiTietDiaChi == null || chiTietDiaChi.trim().isEmpty()) {
                throw new IllegalArgumentException("Thông tin địa chỉ không được để trống.");
            }

            // Cập nhật hoặc tạo mới DiaChiNguoiDung
            DiaChiNguoiDung diaChi = hoaDon.getDiaChi();
            if (diaChi == null) {
                diaChi = new DiaChiNguoiDung();
                diaChi.setNguoiDung(hoaDon.getNguoiDung());
                diaChi.setThoiGianTao(LocalDateTime.now());
            }

            diaChi.setNguoiNhan(hoTen);
            diaChi.setSoDienThoaiNguoiNhan(soDienThoai);
            diaChi.setTinhThanhPho(tinhThanhPho);
            diaChi.setQuanHuyen(quanHuyen);
            diaChi.setPhuongXa(phuongXa);
            diaChi.setChiTietDiaChi(chiTietDiaChi);
            diaChi.setMacDinh(true); // Có thể điều chỉnh logic này tùy theo yêu cầu

            hoaDon.setDiaChi(diaChi);
            hoaDonService.save(hoaDon);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Thông tin hóa đơn đã được cập nhật thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi cập nhật hóa đơn: " + e.getMessage()));
        }
    }


    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadHoaDon(@PathVariable String id) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID hóa đơn không được để trống.");
            }
            byte[] pdfBytes = hoaDonService.generateHoaDonPDF(id);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header("Content-Disposition", "attachment; filename=hoa_don_" + id + ".pdf")
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("Lỗi khi tải PDF: " + e.getMessage()).getBytes());
        }
    }

    @PostMapping("/cancel/{id}")
    @ResponseBody
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable String id, @RequestBody Map<String, String> request) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID hóa đơn không được để trống.");
            }
            UUID uuid = UUID.fromString(id);
            String ghiChu = request.get("ghiChu");
            if (ghiChu == null || ghiChu.trim().isEmpty()) {
                throw new IllegalArgumentException("Ghi chú không được để trống.");
            }
            hoaDonService.cancelOrder(uuid, ghiChu);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đơn hàng đã được hủy thành công.");
            response.put("currentStatus", "Hủy đơn hàng");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi hủy đơn hàng: " + e.getMessage()));
        }
    }

    @PostMapping("/exchange/{id}")
    @ResponseBody
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> exchangeOrder(@PathVariable String id, @RequestBody Map<String, Object> request) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID hóa đơn không được để trống.");
            }
            UUID uuid = UUID.fromString(id);
            String lyDoDoiHang = (String) request.get("lyDoDoiHang");
            List<String> chiTietDonHangIdsStr = (List<String>) request.get("chiTietDonHangIds");
            List<String> newChiTietSanPhamIdsStr = (List<String>) request.get("newChiTietSanPhamIds");

            if (lyDoDoiHang == null || lyDoDoiHang.trim().isEmpty()) {
                throw new IllegalArgumentException("Lý do đổi hàng không được để trống.");
            }
            if (chiTietDonHangIdsStr == null || chiTietDonHangIdsStr.isEmpty() || newChiTietSanPhamIdsStr == null || newChiTietSanPhamIdsStr.isEmpty()) {
                throw new IllegalArgumentException("Danh sách sản phẩm trả hoặc sản phẩm mới không được để trống.");
            }

            List<UUID> chiTietDonHangIds = chiTietDonHangIdsStr.stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toList());
            List<UUID> newChiTietSanPhamIds = newChiTietSanPhamIdsStr.stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toList());

            hoaDonService.processExchange(uuid, chiTietDonHangIds, newChiTietSanPhamIds, lyDoDoiHang);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đơn hàng đã được đổi hàng thành công.");
            response.put("currentStatus", "Đã đổi hàng");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi đổi hàng: " + e.getMessage()));
        }
    }

    @PostMapping("/confirm/{id}")
    @ResponseBody
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> confirmHoaDon(@PathVariable String id, @RequestBody Map<String, String> request) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID hóa đơn không được để trống.");
            }
            UUID uuid = UUID.fromString(id);
            String ghiChu = request.get("ghiChu");
            if (ghiChu == null || ghiChu.trim().isEmpty()) {
                throw new IllegalArgumentException("Ghi chú không được để trống.");
            }
            HoaDon hoaDon = hoaDonService.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + id));

            DonHang donHang = hoaDon.getDonHang();
            if (donHang == null) {
                throw new RuntimeException("Đơn hàng liên quan không tồn tại.");
            }
            donHang = donHangRepository.findById(donHang.getId())
                    .orElseThrow(() -> new RuntimeException("Đơn hàng không tìm thấy trong cơ sở dữ liệu."));

            if (!"Chưa xác nhận".equals(hoaDon.getTrangThai())) {
                throw new RuntimeException("Hóa đơn phải ở trạng thái 'Chưa xác nhận' để xác nhận.");
            }

            String trangThai;
            if ("Tại quầy".equalsIgnoreCase(donHang.getPhuongThucBanHang())) {
                trangThai = "Hoàn thành";
                ghiChu = "Hoàn thành (Tại quầy)";
                donHang.setTrangThaiThanhToan(true);
            } else if ("Online".equalsIgnoreCase(donHang.getPhuongThucBanHang())) {
                trangThai = "Đã xác nhận Online";
                ghiChu = "Đã xác nhận đơn hàng Online";
                if ("Ví Thanh Toán".equalsIgnoreCase(donHang.getPhuongThucThanhToan().getTenPhuongThuc())) {
                    donHang.setTrangThaiThanhToan(true);
                    donHang.setThoiGianThanhToan(LocalDateTime.now());
                }
            } else {
                trangThai = "Đã xác nhận";
                ghiChu = "Đã xác nhận đơn hàng Giao hàng";
            }

            donHangRepository.save(donHang);
            hoaDonService.updateStatus(uuid, trangThai, ghiChu, true);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đơn hàng đã được " + trangThai.toLowerCase() + " thành công.");
            response.put("currentStatus", trangThai);
            response.put("phuongThucBanHang", donHang.getPhuongThucBanHang());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xác nhận hóa đơn: " + e.getMessage()));
        }
    }

    @PostMapping("/confirm-delivery/{id}")
    @ResponseBody
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> confirmDelivery(@PathVariable String id, @RequestBody Map<String, String> request) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID hóa đơn không được để trống.");
            }
            UUID uuid = UUID.fromString(id);
            String ghiChu = request.get("ghiChu");
            if (ghiChu == null || ghiChu.trim().isEmpty()) {
                throw new IllegalArgumentException("Ghi chú không được để trống.");
            }
            HoaDon hoaDon = hoaDonService.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + id));

            DonHang donHang = hoaDon.getDonHang();
            if (donHang == null) {
                throw new RuntimeException("Đơn hàng liên quan không tồn tại.");
            }
            if (!"Giao hàng".equalsIgnoreCase(donHang.getPhuongThucBanHang())) {
                throw new RuntimeException("Chỉ các đơn hàng có phương thức 'Giao hàng' mới có thể chuyển sang trạng thái 'Đang vận chuyển'.");
            }
            if (!"Đã xác nhận".equals(hoaDon.getTrangThai())) {
                throw new RuntimeException("Hóa đơn phải ở trạng thái 'Đã xác nhận' để chuyển sang 'Đang vận chuyển'.");
            }

            hoaDonService.updateStatus(uuid, "Đang vận chuyển", ghiChu, true);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đơn hàng đã được xác nhận giao hàng thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xác nhận giao hàng: " + e.getMessage()));
        }
    }

    @PostMapping("/confirm-delivered/{id}")
    @ResponseBody
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> confirmDelivered(@PathVariable String id, @RequestBody Map<String, String> request) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID hóa đơn không được để trống.");
            }
            UUID uuid = UUID.fromString(id);
            String ghiChu = request.get("ghiChu");
            if (ghiChu == null || ghiChu.trim().isEmpty()) {
                throw new IllegalArgumentException("Ghi chú không được để trống.");
            }
            HoaDon hoaDon = hoaDonService.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + id));

            DonHang donHang = hoaDon.getDonHang();
            if (donHang == null) {
                throw new RuntimeException("Đơn hàng liên quan không tồn tại.");
            }
            if (!"Giao hàng".equalsIgnoreCase(donHang.getPhuongThucBanHang())) {
                throw new RuntimeException("Chỉ các đơn hàng có phương thức 'Giao hàng' mới có thể chuyển sang trạng thái 'Vận chuyển thành công'.");
            }
            if (!"Đang vận chuyển".equals(hoaDon.getTrangThai())) {
                throw new RuntimeException("Hóa đơn phải ở trạng thái 'Đang vận chuyển' để chuyển sang 'Vận chuyển thành công'.");
            }

            hoaDonService.updateStatus(uuid, "Vận chuyển thành công", ghiChu, true);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đơn hàng đã được xác nhận vận chuyển thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xác nhận vận chuyển thành công: " + e.getMessage()));
        }
    }

    @PostMapping("/confirm-online-processing/{id}")
    @ResponseBody
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> confirmOnlineProcessing(@PathVariable String id, @RequestBody Map<String, String> request) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID hóa đơn không được để trống.");
            }
            UUID uuid = UUID.fromString(id);
            String ghiChu = request.get("ghiChu");
            if (ghiChu == null || ghiChu.trim().isEmpty()) {
                throw new IllegalArgumentException("Ghi chú không được để trống.");
            }
            HoaDon hoaDon = hoaDonService.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + id));

            DonHang donHang = hoaDon.getDonHang();
            if (donHang == null) {
                throw new RuntimeException("Đơn hàng liên quan không tồn tại.");
            }
            if (!"Online".equalsIgnoreCase(donHang.getPhuongThucBanHang())) {
                throw new RuntimeException("Chỉ các đơn hàng có phương thức 'Online' mới có thể chuyển sang trạng thái 'Đang xử lý Online'.");
            }
            if (!"Đã xác nhận Online".equals(hoaDon.getTrangThai())) {
                throw new RuntimeException("Hóa đơn phải ở trạng thái 'Đã xác nhận Online' để chuyển sang 'Đang xử lý Online'.");
            }

            hoaDonService.updateStatus(uuid, "Đang xử lý Online", ghiChu, true);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đơn hàng đã được xác nhận đang xử lý Online thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xác nhận xử lý Online: " + e.getMessage()));
        }
    }

    @PostMapping("/confirm-online-shipping/{id}")
    @ResponseBody
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> confirmOnlineShipping(@PathVariable String id, @RequestBody Map<String, String> request) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID hóa đơn không được để trống.");
            }
            UUID uuid = UUID.fromString(id);
            String ghiChu = request.get("ghiChu");
            if (ghiChu == null || ghiChu.trim().isEmpty()) {
                throw new IllegalArgumentException("Ghi chú không được để trống.");
            }
            HoaDon hoaDon = hoaDonService.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + id));

            DonHang donHang = hoaDon.getDonHang();
            if (donHang == null) {
                throw new RuntimeException("Đơn hàng liên quan không tồn tại.");
            }
            if (!"Online".equalsIgnoreCase(donHang.getPhuongThucBanHang())) {
                throw new RuntimeException("Chỉ các đơn hàng có phương thức 'Online' mới có thể chuyển sang trạng thái 'Đang vận chuyển'.");
            }
            if (!"Đang xử lý Online".equals(hoaDon.getTrangThai())) {
                throw new RuntimeException("Hóa đơn phải ở trạng thái 'Đang xử lý Online' để chuyển sang 'Đang vận chuyển'.");
            }

            hoaDonService.updateStatus(uuid, "Đang vận chuyển", ghiChu, true);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đơn hàng đã được xác nhận đang vận chuyển thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xác nhận đang vận chuyển: " + e.getMessage()));
        }
    }

    @PostMapping("/confirm-online-delivered/{id}")
    @ResponseBody
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> confirmOnlineDelivered(@PathVariable String id, @RequestBody Map<String, String> request) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID hóa đơn không được để trống.");
            }
            UUID uuid = UUID.fromString(id);
            String ghiChu = request.get("ghiChu");
            if (ghiChu == null || ghiChu.trim().isEmpty()) {
                throw new IllegalArgumentException("Ghi chú không được để trống.");
            }
            HoaDon hoaDon = hoaDonService.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + id));

            DonHang donHang = hoaDon.getDonHang();
            if (donHang == null) {
                throw new RuntimeException("Đơn hàng liên quan không tồn tại.");
            }
            if (!"Online".equalsIgnoreCase(donHang.getPhuongThucBanHang())) {
                throw new RuntimeException("Chỉ các đơn hàng có phương thức 'Online' mới có thể chuyển sang trạng thái 'Vận chuyển thành công'.");
            }
            if (!"Đang vận chuyển".equals(hoaDon.getTrangThai())) {
                throw new RuntimeException("Hóa đơn phải ở trạng thái 'Đang vận chuyển' để chuyển sang 'Vận chuyển thành công'.");
            }

            hoaDonService.updateStatus(uuid, "Vận chuyển thành công", ghiChu, true);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đơn hàng đã được xác nhận vận chuyển thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xác nhận vận chuyển thành công: " + e.getMessage()));
        }
    }

    @PostMapping("/complete/{id}")
    @ResponseBody
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> completeOrder(@PathVariable String id, @RequestBody Map<String, String> request) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID hóa đơn không được để trống.");
            }
            UUID uuid = UUID.fromString(id);
            String ghiChu = request.get("ghiChu");
            if (ghiChu == null || ghiChu.trim().isEmpty()) {
                throw new IllegalArgumentException("Ghi chú không được để trống.");
            }
            HoaDon hoaDon = hoaDonService.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + id));

            DonHang donHang = hoaDon.getDonHang();
            if (donHang == null) {
                throw new RuntimeException("Đơn hàng liên quan không tồn tại.");
            }
            if (!"Vận chuyển thành công".equals(hoaDon.getTrangThai())) {
                throw new RuntimeException("Hóa đơn phải ở trạng thái 'Vận chuyển thành công' để chuyển sang 'Hoàn thành'.");
            }

            // Kiểm tra quyền của người dùng (chỉ khách hàng được phép xác nhận hoàn thành)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof NguoiDung)) {
                throw new RuntimeException("Chỉ khách hàng có thể xác nhận hoàn thành đơn hàng.");
            }
            NguoiDung user = (NguoiDung) auth.getPrincipal();
            if (!user.getId().equals(hoaDon.getNguoiDung().getId())) {
                throw new RuntimeException("Bạn không có quyền xác nhận hoàn thành đơn hàng này.");
            }

            hoaDonService.updateStatus(uuid, "Hoàn thành", ghiChu, true);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đơn hàng đã được xác nhận hoàn thành thành công.");
            response.put("currentStatus", "Hoàn thành");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xác nhận hoàn thành: " + e.getMessage()));
        }
    }
}