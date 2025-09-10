package com.example.AsmGD1.service;

import com.example.AsmGD1.entity.PhieuGiamGia;
import com.example.AsmGD1.repository.GiamGia.PhieuGiamGiaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WebSocketVoucherStatusService {

    @Autowired
    private PhieuGiamGiaRepository phieuGiamGiaRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private Map<String, String> lastKnownStatuses = new HashMap<>();
    private Map<String, Integer> lastKnownQuantities = new HashMap<>();

    @Scheduled(fixedRate = 15000) // Check every 15 seconds instead of 30
    public void checkAndBroadcastStatusChanges() {
        try {
            List<PhieuGiamGia> activeVouchers = phieuGiamGiaRepository.findAll();

            for (PhieuGiamGia voucher : activeVouchers) {
                String currentStatus = getTrangThai(voucher);
                Integer currentQuantity = voucher.getSoLuong();
                String voucherId = voucher.getId().toString();

                String lastStatus = lastKnownStatuses.get(voucherId);
                Integer lastQuantity = lastKnownQuantities.get(voucherId);

                boolean statusChanged = lastStatus == null || !lastStatus.equals(currentStatus);
                boolean quantityChanged = lastQuantity == null || !lastQuantity.equals(currentQuantity);

                if (statusChanged || quantityChanged) {
                    lastKnownStatuses.put(voucherId, currentStatus);
                    lastKnownQuantities.put(voucherId, currentQuantity);

                    if (lastStatus != null || lastQuantity != null) {
                        if (statusChanged) {
                            broadcastStatusChange(voucher, currentStatus, lastStatus);
                        }
                        if (quantityChanged && !statusChanged) {
                            broadcastQuantityChange(voucher, currentQuantity, lastQuantity);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking voucher status changes: " + e.getMessage());
        }
    }

    private void broadcastQuantityChange(PhieuGiamGia voucher, Integer newQuantity, Integer oldQuantity) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("action", "QUANTITY_CHANGED");
            update.put("voucherId", voucher.getId().toString());
            update.put("voucherCode", voucher.getMa());
            update.put("voucherName", voucher.getTen());
            update.put("oldQuantity", oldQuantity);
            update.put("newQuantity", newQuantity);
            update.put("status", getTrangThai(voucher));
            update.put("message", String.format("Phiếu %s: số lượng từ %d → %d",
                    voucher.getMa(), oldQuantity != null ? oldQuantity : 0, newQuantity != null ? newQuantity : 0));
            update.put("timestamp", System.currentTimeMillis());

            // Broadcast to admin voucher list
            messagingTemplate.convertAndSend("/topic/vouchers", update);

            // Broadcast to payment pages (exclude the user who just used it)
            messagingTemplate.convertAndSend("/topic/payment/vouchers", update);

        } catch (Exception e) {
            System.err.println("Failed to broadcast voucher quantity change: " + e.getMessage());
        }
    }

    private void broadcastStatusChange(PhieuGiamGia voucher, String newStatus, String oldStatus) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("action", "STATUS_CHANGED");
            update.put("voucherId", voucher.getId().toString());
            update.put("voucherCode", voucher.getMa());
            update.put("voucherName", voucher.getTen());
            update.put("oldStatus", oldStatus);
            update.put("newStatus", newStatus);
            update.put("quantity", voucher.getSoLuong());
            update.put("message", String.format("Phiếu %s chuyển từ '%s' sang '%s'", voucher.getMa(), oldStatus, newStatus));
            update.put("timestamp", System.currentTimeMillis());

            // Broadcast to admin voucher list
            messagingTemplate.convertAndSend("/topic/vouchers", update);

            // Broadcast to payment pages
            messagingTemplate.convertAndSend("/topic/payment/vouchers", update);

        } catch (Exception e) {
            System.err.println("Failed to broadcast voucher status change: " + e.getMessage());
        }
    }

    public void broadcastVoucherUsed(PhieuGiamGia voucher, String userId) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("action", "VOUCHER_USED");
            update.put("voucherId", voucher.getId().toString());
            update.put("voucherCode", voucher.getMa());
            update.put("voucherName", voucher.getTen());
            update.put("quantity", voucher.getSoLuong());
            update.put("status", getTrangThai(voucher));
            update.put("userId", userId);
            update.put("message", String.format("Phiếu %s đã được sử dụng", voucher.getMa()));
            update.put("timestamp", System.currentTimeMillis());

            // Broadcast to admin voucher list
            messagingTemplate.convertAndSend("/topic/vouchers", update);

            // Broadcast to payment pages (exclude the user who just used it)
            messagingTemplate.convertAndSend("/topic/payment/vouchers", update);

        } catch (Exception e) {
            System.err.println("Failed to broadcast voucher usage: " + e.getMessage());
        }
    }

    private String getTrangThai(PhieuGiamGia v) {
        LocalDateTime now = LocalDateTime.now();

        if (v.getSoLuong() != null && v.getSoLuong() <= 0) {
            return "Đã kết thúc";
        }

        if (v.getNgayBatDau() != null && v.getNgayKetThuc() != null) {
            if (now.isBefore(v.getNgayBatDau())) {
                return "Sắp diễn ra";
            } else if (!now.isAfter(v.getNgayKetThuc())) {
                return "Đang diễn ra";
            } else {
                return "Đã kết thúc";
            }
        }
        return "Không xác định";
    }
}
