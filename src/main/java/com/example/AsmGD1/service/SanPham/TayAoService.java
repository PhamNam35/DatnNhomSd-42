package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.entity.TayAo;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.repository.SanPham.TayAoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class TayAoService {
    @Autowired
    private TayAoRepository tayAoRepository;

    @Autowired
    private ChiTietSanPhamRepository sanPhamChiTietRepository;

    // Regex: cho phép chữ cái (bao gồm dấu tiếng Việt) và khoảng trắng giữa các từ,
    // không cho phép số, ký tự đặc biệt hoặc khoảng trắng đầu/cuối
    private static final Pattern NAME_PATTERN = Pattern.compile("^(\\p{L}{2,})(\\s\\p{L}{2,})*$");

    // Lấy danh sách tay áo với phân trang
    public Page<TayAo> getAllTayAo(Pageable pageable) {
        return tayAoRepository.findAll(pageable);
    }

    // Lấy tất cả tay áo (không phân trang)
    public List<TayAo> getAllTayAo() {
        return tayAoRepository.findAll();
    }

    // Tìm kiếm tay áo với phân trang
    public Page<TayAo> searchTayAo(String tenTayAo, Pageable pageable) {
        String keyword = (tenTayAo == null) ? "" : tenTayAo.trim();
        return tayAoRepository.findByTenTayAoContainingIgnoreCase(keyword, pageable);
    }

    public TayAo getTayAoById(UUID id) {
        return tayAoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TayAo not found with id: " + id));
    }

    public TayAo saveTayAo(TayAo tayAo) throws IllegalArgumentException {
        // Kiểm tra tên null hoặc rỗng
        if (tayAo.getTenTayAo() == null || tayAo.getTenTayAo().isEmpty()) {
            throw new IllegalArgumentException("Tên tay áo không được để trống");
        }

        // Kiểm tra khoảng trắng đầu
        if (tayAo.getTenTayAo().startsWith(" ")) {
            throw new IllegalArgumentException("Tên tay áo không được bắt đầu bằng khoảng trắng");
        }

        // Trim tên tay áo
        String trimmedTenTayAo = tayAo.getTenTayAo().trim();

        // Kiểm tra sau khi trim còn rỗng không
        if (trimmedTenTayAo.isEmpty()) {
            throw new IllegalArgumentException("Tên tay áo không được để trống");
        }

        // Kiểm tra định dạng tên
        if (!NAME_PATTERN.matcher(trimmedTenTayAo).matches()) {
            throw new IllegalArgumentException(
                    "Tên tay áo chỉ được chứa chữ cái và khoảng trắng giữa các từ, " +
                            "không được chứa số, ký tự đặc biệt hoặc khoảng trắng ở cuối"
            );
        }

        // Kiểm tra trùng lặp tên tay áo
        if (tayAoRepository.findByTenTayAoContainingIgnoreCase(trimmedTenTayAo)
                .stream()
                .anyMatch(t -> !t.getId().equals(tayAo.getId()) &&
                        t.getTenTayAo().equalsIgnoreCase(trimmedTenTayAo))) {
            throw new IllegalArgumentException("Tên tay áo đã tồn tại");
        }

        // Set lại tên đã trim
        tayAo.setTenTayAo(trimmedTenTayAo);
        return tayAoRepository.save(tayAo);
    }

    public void deleteTayAo(UUID id) throws IllegalStateException {
        if (sanPhamChiTietRepository.existsByTayAoId(id)) {
            throw new IllegalStateException("Không thể xóa tay áo vì đang có sản phẩm chi tiết tham chiếu đến");
        }
        tayAoRepository.deleteById(id);
    }
}