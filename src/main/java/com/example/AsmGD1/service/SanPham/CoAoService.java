package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.entity.CoAo;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.repository.SanPham.CoAoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class CoAoService {
    @Autowired
    private CoAoRepository coAoRepository;

    @Autowired
    private ChiTietSanPhamRepository sanPhamChiTietRepository;

    // Regex: cho phép chữ cái (kể cả tiếng Việt) và khoảng trắng giữa các từ,
    // không cho phép khoảng trắng đầu/cuối, số hoặc ký tự đặc biệt
    private static final Pattern NAME_PATTERN = Pattern.compile("^(\\p{L}{2,})(\\s\\p{L}{2,})*$");

    // Lấy danh sách cổ áo với phân trang
    public Page<CoAo> getAllCoAo(Pageable pageable) {
        return coAoRepository.findAll(pageable);
    }

    // Lấy tất cả cổ áo (không phân trang)
    public List<CoAo> getAllCoAo() {
        return coAoRepository.findAll();
    }

    // Tìm kiếm cổ áo với phân trang
    public Page<CoAo> searchCoAo(String tenCoAo, Pageable pageable) {
        String keyword = (tenCoAo == null) ? "" : tenCoAo.trim();
        return coAoRepository.findByTenCoAoContainingIgnoreCase(keyword, pageable);
    }

    public CoAo getCoAoById(UUID id) {
        return coAoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("CoAo not found with id: " + id));
    }

    public CoAo saveCoAo(CoAo coAo) throws IllegalArgumentException {
        // Kiểm tra tên cổ áo null hoặc rỗng
        if (coAo.getTenCoAo() == null || coAo.getTenCoAo().isEmpty()) {
            throw new IllegalArgumentException("Tên cổ áo không được để trống");
        }

        // Kiểm tra khoảng trắng đầu
        if (coAo.getTenCoAo().startsWith(" ")) {
            throw new IllegalArgumentException("Tên cổ áo không được bắt đầu bằng khoảng trắng");
        }

        // Trim tên cổ áo
        String trimmedTenCoAo = coAo.getTenCoAo().trim();

        // Kiểm tra sau khi trim còn rỗng không
        if (trimmedTenCoAo.isEmpty()) {
            throw new IllegalArgumentException("Tên cổ áo không được để trống");
        }

        // Kiểm tra định dạng tên cổ áo
        if (!NAME_PATTERN.matcher(trimmedTenCoAo).matches()) {
            throw new IllegalArgumentException(
                    "Tên cổ áo chỉ được chứa chữ cái và khoảng trắng giữa các từ, " +
                            "không được chứa số, ký tự đặc biệt hoặc khoảng trắng ở cuối"
            );
        }

        // Kiểm tra trùng lặp tên cổ áo
        if (coAoRepository.findByTenCoAoContainingIgnoreCase(trimmedTenCoAo)
                .stream()
                .anyMatch(c -> !c.getId().equals(coAo.getId()) &&
                        c.getTenCoAo().equalsIgnoreCase(trimmedTenCoAo))) {
            throw new IllegalArgumentException("Tên cổ áo đã tồn tại");
        }

        // Set lại tên đã trim
        coAo.setTenCoAo(trimmedTenCoAo);
        return coAoRepository.save(coAo);
    }

    public void deleteCoAo(UUID id) throws IllegalStateException {
        if (sanPhamChiTietRepository.existsByCoAoId(id)) {
            throw new IllegalStateException("Không thể xóa cổ áo vì đang có sản phẩm chi tiết tham chiếu đến");
        }
        coAoRepository.deleteById(id);
    }
}