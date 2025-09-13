package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.entity.DayGiay;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.repository.SanPham.DayGiayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class DayGiayService {
    @Autowired
    private DayGiayRepository dayGiayRepository;

    @Autowired
    private ChiTietSanPhamRepository sanPhamChiTietRepository;

    // Regex to allow letters (including Vietnamese characters) and spaces between words,
    // but not leading/trailing spaces, numbers, or special characters
    private static final Pattern NAME_PATTERN = Pattern.compile("^(\\p{L}{2,})(\\s\\p{L}{2,})*$");

    public Page<DayGiay> getAllDayGiay(Pageable pageable) {
        return dayGiayRepository.findAll(pageable);
    }

    public List<DayGiay> getAllDayGiay() {
        return dayGiayRepository.findAll();
    }

    public Page<DayGiay> searchDayGiay(String tenDayGiay, Pageable pageable) {
        String keyword = (tenDayGiay == null) ? "" : tenDayGiay.trim();
        return dayGiayRepository.findByTenDayGiayContainingIgnoreCase(keyword, pageable);
    }

    public DayGiay getDayGiayById(UUID id) {
        return dayGiayRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("DayGiay not found with id: " + id));
    }

    public DayGiay saveDayGiay(DayGiay dayGiay) throws IllegalArgumentException {
        if (dayGiay.getTenDayGiay() == null || dayGiay.getTenDayGiay().isEmpty()) {
            throw new IllegalArgumentException("Tên dây giầy không được để trống");
        }

        // Check for leading spaces before trimming
        if (dayGiay.getTenDayGiay().startsWith(" ")) {
            throw new IllegalArgumentException("Tên dây giầy không được bắt đầu bằng khoảng trắng");
        }

        // Trim the input for further validation
        String trimmedTenDayGiay = dayGiay.getTenDayGiay().trim();

        // Check if trimmed input is empty
        if (trimmedTenDayGiay.isEmpty()) {
            throw new IllegalArgumentException("Tên dây giầy không được để trống");
        }
        if (!NAME_PATTERN.matcher(trimmedTenDayGiay).matches()) {
            throw new IllegalArgumentException("Tên dây giầy chỉ được chứa chữ cái và khoảng trắng giữa các từ, không được chứa số, ký tự đặc biệt hoặc khoảng trắng ở cuối");
        }

        if (dayGiayRepository.findByTenDayGiayContainingIgnoreCase(trimmedTenDayGiay)
                .stream()
                .anyMatch(x -> !x.getId().equals(dayGiay.getId()) && x.getTenDayGiay().equalsIgnoreCase(trimmedTenDayGiay))) {
            throw new IllegalArgumentException("Tên dây giầy đã tồn tại");
        }

        // Set trimmed value before saving
        dayGiay.setTenDayGiay(trimmedTenDayGiay);
        return dayGiayRepository.save(dayGiay);
    }

    public void deleteDayGiay(UUID id) throws IllegalStateException {
        if (sanPhamChiTietRepository.existsByDayGiayId(id)) {
            throw new IllegalStateException("Không thể xóa dây giầy vì đang có sản phẩm chi tiết tham chiếu đến");
        }
        dayGiayRepository.deleteById(id);
    }
}