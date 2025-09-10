package com.example.AsmGD1.service;

import com.example.AsmGD1.entity.ChienDichGiamGia;
import com.example.AsmGD1.service.GiamGia.ChienDichGiamGiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketCampaignStatusService {

    @Autowired
    private ChienDichGiamGiaService chienDichService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Cache to track previous statuses
    private final Map<String, String> previousStatuses = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 60000) // Check every minute
    public void checkAndBroadcastStatusChanges() {
        try {
            // Get all active campaigns (you'll need to implement this method in your service)
            List<ChienDichGiamGia> campaigns = getAllActiveCampaigns();

            for (ChienDichGiamGia campaign : campaigns) {
                String campaignId = campaign.getId().toString();
                String currentStatus = getStatus(campaign);
                String previousStatus = previousStatuses.get(campaignId);

                // If status changed, broadcast the update
                if (previousStatus != null && !previousStatus.equals(currentStatus)) {
                    broadcastStatusChange(campaign, currentStatus, previousStatus);
                }

                // Update cache
                previousStatuses.put(campaignId, currentStatus);
            }
        } catch (Exception e) {
            System.err.println("Error checking campaign status changes: " + e.getMessage());
        }
    }

    private List<ChienDichGiamGia> getAllActiveCampaigns() {
        // You'll need to implement this method in your ChienDichGiamGiaService
        // For now, return empty list - you should implement this to get all campaigns
        // that are not ended or recently ended
        return List.of();
    }

    private String getStatus(ChienDichGiamGia campaign) {
        LocalDateTime now = LocalDateTime.now();
        if (campaign.getNgayBatDau().isAfter(now)) return "UPCOMING";
        else if (campaign.getNgayKetThuc().isBefore(now)) return "ENDED";
        return "ONGOING";
    }

    private void broadcastStatusChange(ChienDichGiamGia campaign, String newStatus, String oldStatus) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("action", "STATUS_CHANGED");
            message.put("campaignId", campaign.getId().toString());
            message.put("campaignCode", campaign.getMa());
            message.put("campaignName", campaign.getTen());
            message.put("newStatus", newStatus);
            message.put("oldStatus", oldStatus);
            message.put("timestamp", LocalDateTime.now().toString());

            // Broadcast to admin interface
            messagingTemplate.convertAndSend("/topic/campaign-updates", message);

            // Broadcast to client-side for price updates
            messagingTemplate.convertAndSend("/topic/price-updates", message);

            System.out.println("Broadcasted status change for campaign " + campaign.getMa() +
                    " from " + oldStatus + " to " + newStatus);
        } catch (Exception e) {
            System.err.println("Failed to broadcast status change: " + e.getMessage());
        }
    }
}
