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

    // Regex: cho phép chữ cái (bao gồm dấu tiếng Việt) và khoảng trắng giữa các từ,
    // không cho phép số, ký tự đặc biệt hoặc khoảng trắng đầu/cuối
    private static final Pattern NAME_PATTERN = Pattern.compile("^(\\p{L}{2,})(\\s\\p{L}{2,})*$");

    // Lấy danh sách kiểu dáng với phân trang
    public Page<KieuDang> getAllKieuDang(Pageable pageable) {
        return kieuDangRepository.findAll(pageable);
    }

    // Lấy tất cả kiểu dáng (không phân trang)
    public List<KieuDang> getAllKieuDang() {
        return kieuDangRepository.findAll();
    }

    // Tìm kiếm kiểu dáng với phân trang
    public Page<KieuDang> searchKieuDang(String tenKieuDang, Pageable pageable) {
        String keyword = (tenKieuDang == null) ? "" : tenKieuDang.trim();
        return kieuDangRepository.findByTenKieuDangContainingIgnoreCase(keyword, pageable);
    }

    public KieuDang getKieuDangById(UUID id) {
        return kieuDangRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("KieuDang not found with id: " + id));
    }

    public KieuDang saveKieuDang(KieuDang kieuDang) throws IllegalArgumentException {
        // Kiểm tra tên null hoặc rỗng
        if (kieuDang.getTenKieuDang() == null || kieuDang.getTenKieuDang().isEmpty()) {
            throw new IllegalArgumentException("Tên kiểu dáng không được để trống");
        }

        // Kiểm tra khoảng trắng đầu
        if (kieuDang.getTenKieuDang().startsWith(" ")) {
            throw new IllegalArgumentException("Tên kiểu dáng không được bắt đầu bằng khoảng trắng");
        }

        // Trim tên kiểu dáng
        String trimmedTenKieuDang = kieuDang.getTenKieuDang().trim();

        // Kiểm tra sau khi trim còn rỗng không
        if (trimmedTenKieuDang.isEmpty()) {
            throw new IllegalArgumentException("Tên kiểu dáng không được để trống");
        }

        // Kiểm tra định dạng tên
        if (!NAME_PATTERN.matcher(trimmedTenKieuDang).matches()) {
            throw new IllegalArgumentException(
                    "Tên kiểu dáng chỉ được chứa chữ cái và khoảng trắng giữa các từ, " +
                            "không được chứa số, ký tự đặc biệt hoặc khoảng trắng ở cuối"
            );
        }

        // Kiểm tra trùng lặp tên
        if (kieuDangRepository.findByTenKieuDangContainingIgnoreCase(trimmedTenKieuDang)
                .stream()
                .anyMatch(k -> !k.getId().equals(kieuDang.getId()) &&
                        k.getTenKieuDang().equalsIgnoreCase(trimmedTenKieuDang))) {
            throw new IllegalArgumentException("Tên kiểu dáng đã tồn tại");
        }

        // Set lại tên đã trim
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