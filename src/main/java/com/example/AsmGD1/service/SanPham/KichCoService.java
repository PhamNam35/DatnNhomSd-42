package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.entity.KichCo;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.repository.SanPham.KichCoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class KichCoService {
    @Autowired
    private KichCoRepository kichCoRepository;

    @Autowired
    private ChiTietSanPhamRepository sanPhamChiTietRepository;

    // Lấy danh sách kích cỡ với phân trang
    public Page<KichCo> getAllKichCo(Pageable pageable) {
        return kichCoRepository.findAll(pageable);
    }

    // Lấy tất cả kích cỡ (không phân trang)
    public List<KichCo> getAllKichCo() {
        return kichCoRepository.findAll();
    }

    // Tìm kiếm kích cỡ với phân trang
    public Page<KichCo> searchKichCo(String ten, Pageable pageable) {
        String keyword = (ten == null) ? "" : ten.trim();
        return kichCoRepository.findByTenContainingIgnoreCase(keyword, pageable);
    }

    public KichCo getKichCoById(UUID id) {
        return kichCoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kích cỡ không tồn tại với ID: " + id));
    }

    public KichCo saveKichCo(KichCo kichCo) throws IllegalArgumentException {
        // Kiểm tra null hoặc rỗng
        if (kichCo.getTen() == null || kichCo.getTen().isEmpty()) {
            throw new IllegalArgumentException("Tên kích cỡ không được để trống");
        }

        // Kiểm tra khoảng trắng đầu
        if (kichCo.getTen().startsWith(" ")) {
            throw new IllegalArgumentException("Tên kích cỡ không được bắt đầu bằng khoảng trắng");
        }

        // Chuẩn hóa tên trước khi validate
        String tenNormalized = kichCo.getTen().trim().toUpperCase();

        // Kiểm tra tên hợp lệ (chỉ cho phép XS, S, M, L, XL, XXL, XXXL)
        if (!tenNormalized.matches("^(XS|S|M|L|XL|XXL|XXXL)$")) {
            throw new IllegalArgumentException(
                    "Tên kích cỡ không hợp lệ. Chỉ cho phép XS, S, M, L, XL, XXL, XXXL (không phân biệt hoa/thường)."
            );
        }

        // Kiểm tra trùng lặp (so sánh sau khi chuẩn hóa)
        if (kichCoRepository.findByTenContainingIgnoreCase(tenNormalized)
                .stream()
                .anyMatch(k -> !k.getId().equals(kichCo.getId())
                        && k.getTen().equalsIgnoreCase(tenNormalized))) {
            throw new IllegalArgumentException("Tên kích cỡ đã tồn tại");
        }

        // Set lại tên đã chuẩn hóa
        kichCo.setTen(tenNormalized);

        return kichCoRepository.save(kichCo);
    }

    public void deleteKichCo(UUID id) throws IllegalStateException {
        if (sanPhamChiTietRepository.existsByKichCoId(id)) {
            throw new IllegalStateException("Không thể xóa kích cỡ vì đang có sản phẩm chi tiết tham chiếu đến");
        }
        kichCoRepository.deleteById(id);
    }
}
