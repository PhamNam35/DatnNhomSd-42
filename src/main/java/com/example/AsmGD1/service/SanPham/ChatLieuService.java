package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.entity.ChatLieu;
import com.example.AsmGD1.repository.SanPham.ChatLieuRepository;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ChatLieuService {
    @Autowired
    private ChatLieuRepository chatLieuRepository;

    @Autowired
    private ChiTietSanPhamRepository sanPhamChiTietRepository;

    // Regex: cho phép chữ cái (kể cả tiếng Việt) và khoảng trắng giữa các từ
    // Không cho phép khoảng trắng đầu/cuối, số hoặc ký tự đặc biệt
    private static final Pattern NAME_PATTERN = Pattern.compile("^(\\p{L}{2,})(\\s\\p{L}{2,})*$");

    // Lấy danh sách chất liệu với phân trang
    public Page<ChatLieu> getAllChatLieu(Pageable pageable) {
        return chatLieuRepository.findAll(pageable);
    }

    // Lấy tất cả chất liệu (không phân trang)
    public List<ChatLieu> getAllChatLieu() {
        return chatLieuRepository.findAll();
    }

    // Tìm kiếm chất liệu với phân trang
    public Page<ChatLieu> searchChatLieu(String tenChatLieu, Pageable pageable) {
        String keyword = (tenChatLieu == null) ? "" : tenChatLieu.trim();
        return chatLieuRepository.findByTenChatLieuContainingIgnoreCase(keyword, pageable);
    }

    public ChatLieu getChatLieuById(UUID id) {
        return chatLieuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ChatLieu not found with id: " + id));
    }

    public ChatLieu saveChatLieu(ChatLieu chatLieu) throws IllegalArgumentException {
        // Kiểm tra null hoặc rỗng
        if (chatLieu.getTenChatLieu() == null || chatLieu.getTenChatLieu().isEmpty()) {
            throw new IllegalArgumentException("Tên chất liệu không được để trống");
        }

        // Kiểm tra khoảng trắng đầu
        if (chatLieu.getTenChatLieu().startsWith(" ")) {
            throw new IllegalArgumentException("Tên chất liệu không được bắt đầu bằng khoảng trắng");
        }

        // Trim tên chất liệu
        String trimmedTenChatLieu = chatLieu.getTenChatLieu().trim();

        // Kiểm tra sau khi trim còn rỗng hay không
        if (trimmedTenChatLieu.isEmpty()) {
            throw new IllegalArgumentException("Tên chất liệu không được để trống");
        }

        // Kiểm tra định dạng tên chất liệu
        if (!NAME_PATTERN.matcher(trimmedTenChatLieu).matches()) {
            throw new IllegalArgumentException(
                    "Tên chất liệu chỉ được chứa chữ cái và khoảng trắng giữa các từ, " +
                            "không được chứa số, ký tự đặc biệt hoặc khoảng trắng ở cuối"
            );
        }

        // Kiểm tra trùng lặp
        if (chatLieuRepository.findByTenChatLieuContainingIgnoreCase(trimmedTenChatLieu)
                .stream()
                .anyMatch(c -> !c.getId().equals(chatLieu.getId()) &&
                        c.getTenChatLieu().equalsIgnoreCase(trimmedTenChatLieu))) {
            throw new IllegalArgumentException("Tên chất liệu đã tồn tại");
        }

        // Set tên sau khi trim để lưu
        chatLieu.setTenChatLieu(trimmedTenChatLieu);
        return chatLieuRepository.save(chatLieu);
    }

    public void deleteChatLieu(UUID id) throws IllegalStateException {
        if (sanPhamChiTietRepository.existsByChatLieuId(id)) {
            throw new IllegalStateException("Không thể xóa chất liệu vì đang có sản phẩm chi tiết tham chiếu đến");
        }
        chatLieuRepository.deleteById(id);
    }
}
