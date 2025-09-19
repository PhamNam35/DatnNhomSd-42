package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.entity.KieuDang;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.repository.SanPham.KieuDangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class KieuDangService {
    @Autowired
    private KieuDangRepository kieuDangRepository;

    @Autowired
    private ChiTietSanPhamRepository sanPhamChiTietRepository;

    // Regex to allow letters (including Vietnamese characters) and spaces between words,
    // but not leading/trailing spaces, numbers, or special characters
    private static final Pattern NAME_PATTERN = Pattern.compile("^(\\p{L}{2,})(\\s\\p{L}{2,})*$");

    public Page<KieuDang> getAllKieuDang(Pageable pageable) {
        return kieuDangRepository.findAll(pageable);
    }

    public List<KieuDang> getAllKieuDang() {
        return kieuDangRepository.findAll();
    }

    public Page<KieuDang> searchKieuDang(String tenKieuDang, Pageable pageable) {
        String keyword = (tenKieuDang == null) ? "" : tenKieuDang.trim();
        return kieuDangRepository.findByTenKieuDangContainingIgnoreCase(keyword, pageable);
    }

    public KieuDang getKieuDangById(UUID id) {
        return kieuDangRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("KieuDang not found with id: " + id));
    }

    public KieuDang saveKieuDang(KieuDang kieuDang) throws IllegalArgumentException {
        if (kieuDang.getTenKieuDang() == null || kieuDang.getTenKieuDang().isEmpty()) {
            throw new IllegalArgumentException("Tên kiểu dáng không được để trống");
        }

        // Check for leading spaces before trimming
        if (kieuDang.getTenKieuDang().startsWith(" ")) {
            throw new IllegalArgumentException("Tên kiểu dáng không được bắt đầu bằng khoảng trắng");
        }

        // Trim the input for further validation
        String trimmedTenKieuDang = kieuDang.getTenKieuDang().trim();

        // Check if trimmed input is empty
        if (trimmedTenKieuDang.isEmpty()) {
            throw new IllegalArgumentException("Tên kiểu dáng không được để trống");
        }

        // Check name format
        if (!NAME_PATTERN.matcher(trimmedTenKieuDang).matches()) {
            throw new IllegalArgumentException(
                    "Tên kiểu dáng chỉ được chứa chữ cái và khoảng trắng giữa các từ, " +
                            "không được chứa số, ký tự đặc biệt hoặc khoảng trắng ở cuối"
            );
        }

        // Check for duplicate name
        if (kieuDangRepository.findByTenKieuDangContainingIgnoreCase(trimmedTenKieuDang)
                .stream()
                .anyMatch(k -> !k.getId().equals(kieuDang.getId()) &&
                        k.getTenKieuDang().equalsIgnoreCase(trimmedTenKieuDang))) {
            throw new IllegalArgumentException("Tên kiểu dáng đã tồn tại");
        }

        // Set trimmed value before saving
        kieuDang.setTenKieuDang(trimmedTenKieuDang);
        return kieuDangRepository.save(kieuDang);
    }

    public void deleteKieuDang(UUID id) throws IllegalStateException {
        if (sanPhamChiTietRepository.existsByKieuDangId(id)) {
            throw new IllegalStateException("Không thể xóa kiểu dáng vì đang có sản phẩm chi tiết tham chiếu đến");
        }
        kieuDangRepository.deleteById(id);
    }
}