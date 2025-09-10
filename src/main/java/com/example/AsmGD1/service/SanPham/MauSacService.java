package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.entity.MauSac;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.repository.SanPham.MauSacRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class MauSacService {
    @Autowired
    private MauSacRepository mauSacRepository;

    @Autowired
    private ChiTietSanPhamRepository sanPhamChiTietRepository;

    // Regex: cho phép chữ cái (bao gồm dấu tiếng Việt) và khoảng trắng giữa các từ,
    // không cho phép số, ký tự đặc biệt hoặc khoảng trắng đầu/cuối
    private static final Pattern NAME_PATTERN = Pattern.compile("^(\\p{L}{2,})(\\s\\p{L}{2,})*$");

    // Lấy danh sách màu sắc với phân trang
    public Page<MauSac> getAllMauSac(Pageable pageable) {
        return mauSacRepository.findAll(pageable);
    }

    // Lấy tất cả màu sắc (không phân trang)
    public List<MauSac> getAllMauSac() {
        return mauSacRepository.findAll();
    }

    // Tìm kiếm màu sắc với phân trang
    public Page<MauSac> searchMauSac(String tenMau, Pageable pageable) {
        String keyword = (tenMau == null) ? "" : tenMau.trim();
        return mauSacRepository.findByTenMauContainingIgnoreCase(keyword, pageable);
    }

    public MauSac getMauSacById(UUID id) {
        return mauSacRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Màu sắc không tồn tại với ID: " + id));
    }

    public MauSac saveMauSac(MauSac mauSac) throws IllegalArgumentException {
        // Kiểm tra tên null hoặc rỗng
        if (mauSac.getTenMau() == null || mauSac.getTenMau().isEmpty()) {
            throw new IllegalArgumentException("Tên màu sắc không được để trống");
        }

        // Kiểm tra khoảng trắng đầu
        if (mauSac.getTenMau().startsWith(" ")) {
            throw new IllegalArgumentException("Tên màu sắc không được bắt đầu bằng khoảng trắng");
        }

        // Trim tên màu
        String trimmedTenMau = mauSac.getTenMau().trim();

        // Kiểm tra sau khi trim còn rỗng không
        if (trimmedTenMau.isEmpty()) {
            throw new IllegalArgumentException("Tên màu sắc không được để trống");
        }

        // Kiểm tra định dạng tên
        if (!NAME_PATTERN.matcher(trimmedTenMau).matches()) {
            throw new IllegalArgumentException(
                    "Tên màu sắc chỉ được chứa chữ cái và khoảng trắng giữa các từ, " +
                            "không được chứa số, ký tự đặc biệt hoặc khoảng trắng ở cuối"
            );
        }

        // Kiểm tra trùng lặp tên màu
        if (mauSacRepository.findByTenMauContainingIgnoreCase(trimmedTenMau)
                .stream()
                .anyMatch(m -> !m.getId().equals(mauSac.getId()) &&
                        m.getTenMau().equalsIgnoreCase(trimmedTenMau))) {
            throw new IllegalArgumentException("Tên màu sắc đã tồn tại");
        }

        // Set lại tên đã trim
        mauSac.setTenMau(trimmedTenMau);
        return mauSacRepository.save(mauSac);
    }

    public void deleteMauSac(UUID id) throws IllegalStateException {
        if (sanPhamChiTietRepository.existsByMauSacId(id)) {
            throw new IllegalStateException("Không thể xóa màu sắc vì đang có sản phẩm chi tiết tham chiếu đến");
        }
        mauSacRepository.deleteById(id);
    }
}