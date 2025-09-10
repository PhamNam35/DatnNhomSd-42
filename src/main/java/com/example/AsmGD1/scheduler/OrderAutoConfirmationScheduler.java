package com.example.AsmGD1.scheduler;

import com.example.AsmGD1.entity.DonHang;
import com.example.AsmGD1.entity.HoaDon;
import com.example.AsmGD1.entity.LichSuHoaDon;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.HoaDon.HoaDonRepository;
import com.example.AsmGD1.repository.HoaDon.LichSuHoaDonRepository;
import com.example.AsmGD1.repository.BanHang.DonHangRepository; // <- thêm nếu muốn save DonHang tường minh
import com.example.AsmGD1.service.HoaDon.HoaDonService;
import com.example.AsmGD1.service.ThongBao.ThongBaoService;
import com.example.AsmGD1.service.WebKhachHang.EmailService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class OrderAutoConfirmationScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderAutoConfirmationScheduler.class);

    private static final String TRANSPORT_SUCCESS_STATUS = "Vận chuyển thành công";
    private static final String COMPLETED_STATUS = "Hoàn thành";
    private static final String DON_HANG_SUCCESS_STATUS = "THANH_CONG";

    private final HoaDonRepository hoaDonRepository;
    private final LichSuHoaDonRepository lichSuHoaDonRepository;
    private final DonHangRepository donHangRepository;
    private final HoaDonService hoaDonService;
    private final ThongBaoService thongBaoService;
    private final EmailService emailService;

    // Đọc từ cấu hình: mặc định 30 giây
    private final long autoConfirmSeconds;

    public OrderAutoConfirmationScheduler(HoaDonRepository hoaDonRepository,
                                          LichSuHoaDonRepository lichSuHoaDonRepository,
                                          DonHangRepository donHangRepository,
                                          HoaDonService hoaDonService,
                                          ThongBaoService thongBaoService,
                                          EmailService emailService,
                                          @Value("${order.auto-complete-seconds:30}") long autoConfirmSeconds) {
        this.hoaDonRepository = hoaDonRepository;
        this.lichSuHoaDonRepository = lichSuHoaDonRepository;
        this.donHangRepository = donHangRepository;
        this.hoaDonService = hoaDonService;
        this.thongBaoService = thongBaoService;
        this.emailService = emailService;
        this.autoConfirmSeconds = autoConfirmSeconds;
    }

    /**
     * Quét mỗi 10 giây (fixedDelay) để giảm trùng lặp khi job chạy lâu.
     * Bạn có thể chỉnh về 5 giây nếu cần mượt hơn.
     */
    @Scheduled(fixedDelay = 10_000)
    @Transactional(rollbackOn = Exception.class)
    public void autoConfirmOrders() {
        log.info("Bắt đầu task tự động xác nhận đơn hàng ({}s)...", autoConfirmSeconds);

        // Lấy tất cả hóa đơn đang ở trạng thái "Vận chuyển thành công"
        List<HoaDon> delivered = hoaDonRepository.findByTrangThai(TRANSPORT_SUCCESS_STATUS);
        int confirmed = 0;

        for (HoaDon hd : delivered) {
            try {
                // Lấy mốc thời gian "Vận chuyển thành công" gần nhất
                Optional<LichSuHoaDon> latestDelivered = lichSuHoaDonRepository
                        .findFirstByHoaDonIdAndTrangThaiOrderByThoiGianDesc(hd.getId(), TRANSPORT_SUCCESS_STATUS);

                if (latestDelivered.isEmpty()) continue;

                LocalDateTime deliveredAt = latestDelivered.get().getThoiGian();
                long elapsed = Duration.between(deliveredAt, LocalDateTime.now()).getSeconds();

                // Chưa đủ thời gian → bỏ qua
                if (elapsed < autoConfirmSeconds) continue;

                // Double-check: có ai vừa đổi trạng thái không?
                if (!TRANSPORT_SUCCESS_STATUS.equals(hd.getTrangThai())) continue;

                // Cập nhật hóa đơn
                hd.setTrangThai(COMPLETED_STATUS);
                hd.setNgayThanhToan(LocalDateTime.now());
                String note = "Hệ thống tự động xác nhận đã nhận hàng sau " + autoConfirmSeconds + " giây.";
                hd.setGhiChu(note);

                // Cập nhật đơn hàng
                DonHang dh = hd.getDonHang();
                if (dh != null) {
                    dh.setTrangThai(DON_HANG_SUCCESS_STATUS);
                    // Lưu tường minh để tránh phụ thuộc cascade
                    donHangRepository.save(dh);
                }

                // Lưu hóa đơn
                hoaDonRepository.save(hd);

                // Ghi lịch sử
                try {
                    hoaDonService.addLichSuHoaDon(hd, COMPLETED_STATUS, note);
                } catch (Exception e) {
                    log.warn("Lỗi add lịch sử, bỏ qua: {}", e.getMessage());
                }

                // Thông báo + email (không làm fail job)
                try {
                    thongBaoService.taoThongBaoHeThong(
                            "admin",
                            "Đơn hàng đã hoàn thành (tự động)",
                            "Đơn hàng mã " + (dh != null ? dh.getMaDonHang() : "N/A") + " đã được hệ thống tự động xác nhận sau " + autoConfirmSeconds + " giây.",
                            dh
                    );

                    if (dh != null && dh.getNguoiDung() != null && dh.getNguoiDung().getEmail() != null) {
                        NguoiDung nguoiDung = dh.getNguoiDung();
                        String emailContent = "<h2>Hoàn thành đơn hàng</h2>" +
                                "<p>Xin chào " + nguoiDung.getHoTen() + ",</p>" +
                                "<p>Đơn hàng <strong>" + dh.getMaDonHang() + "</strong> đã được hệ thống tự động xác nhận hoàn thành sau " + autoConfirmSeconds + " giây kể từ khi giao thành công.</p>" +
                                "<p>Nếu có vấn đề, vui lòng liên hệ ACV Store.</p>";
                        emailService.sendEmail(nguoiDung.getEmail(), "Hoàn thành đơn hàng - ACV Store", emailContent);
                    }
                } catch (Exception e) {
                    log.warn("Gửi thông báo/email lỗi: {}", e.getMessage());
                }

                confirmed++;
                log.info("Auto-complete hóa đơn {} xong.", hd.getId());
            } catch (Exception ex) {
                log.error("Lỗi auto-complete hóa đơn {}: {}", hd.getId(), ex.getMessage(), ex);
            }
        }

        log.info("Task hoàn tất: {} hóa đơn được tự động xác nhận.", confirmed);
    }
}