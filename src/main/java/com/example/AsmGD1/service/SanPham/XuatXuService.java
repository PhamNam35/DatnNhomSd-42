package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.entity.XuatXu;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.repository.SanPham.XuatXuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class XuatXuService {
    @Autowired
    private XuatXuRepository xuatXuRepository;

    @Autowired
    private ChiTietSanPhamRepository sanPhamChiTietRepository;

    // Regex to allow letters (including Vietnamese characters) and spaces between words,
    // but not leading/trailing spaces, numbers, or special characters
    private static final Pattern NAME_PATTERN = Pattern.compile("^(\\p{L}{2,})(\\s\\p{L}{2,})*$");

    public Page<XuatXu> getAllXuatXu(Pageable pageable) {
        return xuatXuRepository.findAll(pageable);
    }

    public List<XuatXu> getAllXuatXu() {
        return xuatXuRepository.findAll();
    }

    public Page<XuatXu> searchXuatXu(String tenXuatXu, Pageable pageable) {
        String keyword = (tenXuatXu == null) ? "" : tenXuatXu.trim();
        return xuatXuRepository.findByTenXuatXuContainingIgnoreCase(keyword, pageable);
    }

    public XuatXu getXuatXuById(UUID id) {
        return xuatXuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("XuatXu not found with id: " + id));
    }

    public XuatXu saveXuatXu(XuatXu xuatXu) throws IllegalArgumentException {
        // Check if tenXuatXu is null or empty
        if (xuatXu.getTenXuatXu() == null || xuatXu.getTenXuatXu().isEmpty()) {
            throw new IllegalArgumentException("Tên xuất xứ không được để trống");
        }

        // Check for leading spaces before trimming
        if (xuatXu.getTenXuatXu().startsWith(" ")) {
            throw new IllegalArgumentException("Tên xuất xứ không được bắt đầu bằng khoảng trắng");
        }

        // Trim the input for further validation
        String trimmedTenXuatXu = xuatXu.getTenXuatXu().trim();

        // Check if trimmed input is empty
        if (trimmedTenXuatXu.isEmpty()) {
            throw new IllegalArgumentException("Tên xuất xứ không được để trống");
        }

        // Check format of tenXuatXu (only letters and spaces between words, no trailing spaces)
        if (!NAME_PATTERN.matcher(trimmedTenXuatXu).matches()) {
            throw new IllegalArgumentException("Tên xuất xứ chỉ được chứa chữ cái và khoảng trắng giữa các từ, không được chứa số, ký tự đặc biệt hoặc khoảng trắng ở cuối");
        }

        // Check if tenXuatXu already exists
        if (xuatXuRepository.findByTenXuatXuContainingIgnoreCase(trimmedTenXuatXu)
                .stream()
                .anyMatch(x -> !x.getId().equals(xuatXu.getId()) && x.getTenXuatXu().equalsIgnoreCase(trimmedTenXuatXu))) {
            throw new IllegalArgumentException("Tên xuất xứ đã tồn tại");
        }

        // Set trimmed value before saving
        xuatXu.setTenXuatXu(trimmedTenXuatXu);
        return xuatXuRepository.save(xuatXu);
    }

    public void deleteXuatXu(UUID id) throws IllegalStateException {
        if (sanPhamChiTietRepository.existsByXuatXuId(id)) {
            throw new IllegalStateException("Không thể xóa xuất xứ vì đang có sản phẩm chi tiết tham chiếu đến");
        }
        xuatXuRepository.deleteById(id);
    }
}