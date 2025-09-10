package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.entity.ThuongHieu;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.repository.SanPham.ThuongHieuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ThuongHieuService {
    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private ChiTietSanPhamRepository sanPhamChiTietRepository;

    // Regex: cho phép chữ cái (bao gồm dấu tiếng Việt) và khoảng trắng giữa các từ,
    // không cho phép số, ký tự đặc biệt hoặc khoảng trắng đầu/cuối
    private static final Pattern NAME_PATTERN = Pattern.compile("^(\\p{L}{2,})(\\s\\p{L}{2,})*$");

    // Lấy danh sách thương hiệu với phân trang
    public Page<ThuongHieu> getAllThuongHieu(Pageable pageable) {
        return thuongHieuRepository.findAll(pageable);
    }

    // Lấy tất cả thương hiệu (không phân trang)
    public List<ThuongHieu> getAllThuongHieu() {
        return thuongHieuRepository.findAll();
    }

    // Tìm kiếm thương hiệu với phân trang
    public Page<ThuongHieu> searchThuongHieu(String tenThuongHieu, Pageable pageable) {
        String keyword = (tenThuongHieu == null) ? "" : tenThuongHieu.trim();
        return thuongHieuRepository.findByTenThuongHieuContainingIgnoreCase(keyword, pageable);
    }

    public ThuongHieu getThuongHieuById(UUID id) {
        return thuongHieuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Thương hiệu không tồn tại với ID: " + id));
    }

    public ThuongHieu saveThuongHieu(ThuongHieu thuongHieu) throws IllegalArgumentException {
        String ten = thuongHieu.getTenThuongHieu();

        // Kiểm tra null hoặc rỗng
        if (ten == null || ten.isEmpty()) {
            throw new IllegalArgumentException("Tên thương hiệu không được để trống");
        }

        // Kiểm tra khoảng trắng đầu
        if (ten.startsWith(" ")) {
            throw new IllegalArgumentException("Tên thương hiệu không được bắt đầu bằng khoảng trắng");
        }

        // Trim tên
        String trimmedTen = ten.trim();

        // Kiểm tra sau khi trim còn rỗng không
        if (trimmedTen.isEmpty()) {
            throw new IllegalArgumentException("Tên thương hiệu không được để trống");
        }

        // Kiểm tra định dạng
        if (!NAME_PATTERN.matcher(trimmedTen).matches()) {
            throw new IllegalArgumentException(
                    "Tên thương hiệu chỉ được chứa chữ cái và khoảng trắng giữa các từ, " +
                            "không được chứa số, ký tự đặc biệt hoặc khoảng trắng ở cuối"
            );
        }

        // Kiểm tra trùng lặp
        if (thuongHieuRepository.findByTenThuongHieuContainingIgnoreCase(trimmedTen)
                .stream()
                .anyMatch(t -> !t.getId().equals(thuongHieu.getId()) &&
                        t.getTenThuongHieu().equalsIgnoreCase(trimmedTen))) {
            throw new IllegalArgumentException("Tên thương hiệu đã tồn tại");
        }

        // Set lại tên đã chuẩn hóa
        thuongHieu.setTenThuongHieu(trimmedTen);

        return thuongHieuRepository.save(thuongHieu);
    }

    public void deleteThuongHieu(UUID id) throws IllegalStateException {
        if (sanPhamChiTietRepository.existsByThuongHieuId(id)) {
            throw new IllegalStateException("Không thể xóa thương hiệu vì đang có sản phẩm chi tiết tham chiếu đến");
        }
        thuongHieuRepository.deleteById(id);
    }
}