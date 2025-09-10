package com.example.AsmGD1.controller.BanHang;

import com.example.AsmGD1.service.SanPham.ChiTietSanPhamService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;


@Component
public class SessionListener implements HttpSessionListener {
    @Autowired
    private ChiTietSanPhamService chiTietSanPhamService;

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        Map<UUID, Map<String, Object>> tempStockChanges = (Map<UUID, Map<String, Object>>) session.getAttribute("tempStockChanges");
        if (tempStockChanges != null) {
            // Không cần rollback vì chưa trừ thật, chỉ cần xóa session
            session.removeAttribute("tempStockChanges");
        }
    }
}