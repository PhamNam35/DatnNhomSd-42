package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.entity.DanhMuc;
import com.example.AsmGD1.repository.SanPham.DanhMucRepository;
import com.example.AsmGD1.repository.SanPham.SanPhamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class DanhMucService {
    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    // Regex: cho phép chữ cái (kể cả tiếng Việt) và khoảng trắng giữa các từ,
    // không cho phép khoảng trắng đầu/cuối, số hoặc ký tự đặc biệt
    private static final Pattern NAME_PATTERN = Pattern.compile("^(\\p{L}{2,})(\\s\\p{L}{2,})*$");

    // Lấy danh sách danh mục với phân trang
    public Page<DanhMuc> getAllDanhMuc(Pageable pageable) {
        return danhMucRepository.findAll(pageable);
    }

    // Lấy tất cả danh mục (không phân trang)
    public List<DanhMuc> getAllDanhMuc() {
        return danhMucRepository.findAll();
    }

    // Tìm kiếm danh mục với phân trang
    public Page<DanhMuc> searchDanhMuc(String tenDanhMuc, Pageable pageable) {
        String keyword = (tenDanhMuc == null) ? "" : tenDanhMuc.trim();
        return danhMucRepository.findByTenDanhMucContainingIgnoreCase(keyword, pageable);
    }

    public DanhMuc getDanhMucById(UUID id) {
        return danhMucRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("DanhMuc not found with id: " + id));
    }

    public DanhMuc saveDanhMuc(DanhMuc danhMuc) throws IllegalArgumentException {
        // Kiểm tra tên danh mục null hoặc rỗng
        if (danhMuc.getTenDanhMuc() == null || danhMuc.getTenDanhMuc().isEmpty()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống");
        }

        // Kiểm tra khoảng trắng đầu
        if (danhMuc.getTenDanhMuc().startsWith(" ")) {
            throw new IllegalArgumentException("Tên danh mục không được bắt đầu bằng khoảng trắng");
        }

        // Trim tên danh mục
        String trimmedTenDanhMuc = danhMuc.getTenDanhMuc().trim();

        // Kiểm tra sau khi trim còn rỗng không
        if (trimmedTenDanhMuc.isEmpty()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống");
        }

        // Kiểm tra định dạng tên danh mục
        if (!NAME_PATTERN.matcher(trimmedTenDanhMuc).matches()) {
            throw new IllegalArgumentException(
                    "Tên danh mục chỉ được chứa chữ cái và khoảng trắng giữa các từ, " +
                            "không được chứa số, ký tự đặc biệt hoặc khoảng trắng ở cuối"
            );
        }

        // Kiểm tra trùng lặp tên danh mục
        if (danhMucRepository.findByTenDanhMucContainingIgnoreCase(trimmedTenDanhMuc)
                .stream()
                .anyMatch(d -> !d.getId().equals(danhMuc.getId()) &&
                        d.getTenDanhMuc().equalsIgnoreCase(trimmedTenDanhMuc))) {
            throw new IllegalArgumentException("Tên danh mục đã tồn tại");
        }

        // Set lại tên đã trim
        danhMuc.setTenDanhMuc(trimmedTenDanhMuc);
        return danhMucRepository.save(danhMuc);
    }

    public void deleteDanhMuc(UUID id) throws IllegalStateException {
        if (sanPhamRepository.existsByDanhMucId(id)) {
            throw new IllegalStateException("Không thể xóa danh mục vì đang có sản phẩm tham chiếu đến");
        }
        danhMucRepository.deleteById(id);
    }
}