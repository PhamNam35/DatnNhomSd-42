package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.BanHang.ChiTietDonHangRepository;
import com.example.AsmGD1.repository.HoaDon.HoaDonRepository;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.repository.SanPham.SanPhamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SanPhamService {

    private static final Logger logger = LoggerFactory.getLogger(SanPhamService.class);

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private ChiTietDonHangRepository chiTietDonHangRepository;

    private final String UPLOAD_DIR;

    public SanPhamService() {
        String os = System.getProperty("os.name").toLowerCase();
        UPLOAD_DIR = os.contains("win") ? "C:/DATN/uploads/san_pham/" : System.getProperty("user.home") + "/DATN/uploads/san_pham/";
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("Created directory: {}", UPLOAD_DIR);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + UPLOAD_DIR, e);
        }
    }

    public List<SanPham> findAll() {
        return sanPhamRepository.findAll();
    }

    public List<SanPham> findAllByTrangThai() {
        return sanPhamRepository.findAllByTrangThai();
    }

    public Page<SanPham> findAllPaginated(Pageable pageable) {
        Pageable sortedByThoiGianTaoDesc = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "thoiGianTao"));
        Page<SanPham> sanPhamPage = sanPhamRepository.findAll(sortedByThoiGianTaoDesc);
        sanPhamPage.getContent().forEach(this::setTongSoLuong);
        sanPhamPage.getContent().forEach(this::autoUpdateStatusBasedOnDetails);
        return sanPhamPage;
    }

    public SanPham findById(UUID id) {
        SanPham sanPham = sanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại với ID: " + id));
        setTongSoLuong(sanPham);
        autoUpdateStatusBasedOnDetails(sanPham);
        return sanPham;
    }


    public Page<SanPham> findByAdvancedFilters(String searchName, Boolean trangThai,
                                               UUID danhMucId, UUID thuongHieuId, UUID kieuDangId,
                                               UUID chatLieuId, UUID xuatXuId, UUID dayGiayId,
                                               Pageable pageable) {
        // Pageable sẽ tự áp dụng sort từ controller
        Page<SanPham> sanPhamPage = sanPhamRepository.findByAdvancedFilters(
                searchName != null && !searchName.isEmpty() ? searchName : null,
                trangThai,
                danhMucId,
                thuongHieuId,
                kieuDangId,
                chatLieuId,
                xuatXuId,
                dayGiayId,
                pageable
        );
        sanPhamPage.getContent().forEach(this::setTongSoLuong);
        sanPhamPage.getContent().forEach(this::autoUpdateStatusBasedOnDetails);
        return sanPhamPage;
    }

    public void save(SanPham sanPham) {
        sanPhamRepository.save(sanPham);
    }

    public void saveSanPhamWithImage(SanPham sanPham, MultipartFile imageFile) {
        if (imageFile != null && !imageFile.isEmpty() && imageFile.getOriginalFilename() != null && !imageFile.getOriginalFilename().isEmpty()) {
            try {
                String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
                Path filePath = Paths.get(UPLOAD_DIR, fileName);
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, imageFile.getBytes());
                if (!Files.exists(filePath)) {
                    logger.error("Tệp không được lưu đúng cách: {}", filePath);
                    throw new RuntimeException("Không thể lưu tệp ảnh: " + fileName);
                }
                logger.info("Đã lưu ảnh: {}", fileName);
                sanPham.setUrlHinhAnh("/images/" + fileName);
            } catch (IOException e) {
                logger.error("Không thể lưu tệp ảnh: {}", imageFile.getOriginalFilename(), e);
                throw new RuntimeException("Không thể lưu tệp ảnh: " + imageFile.getOriginalFilename(), e);
            }
        }
        sanPhamRepository.save(sanPham);
    }

    public void deleteById(UUID id) {
        SanPham sanPham = sanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại với ID: " + id));
        if (sanPham.getUrlHinhAnh() != null && !sanPham.getUrlHinhAnh().isEmpty()) {
            try {
                String fileName = sanPham.getUrlHinhAnh().replace("/images/", "");
                Path filePath = Paths.get(UPLOAD_DIR, fileName);
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    logger.info("Đã xóa tệp ảnh: {}", filePath);
                }
            } catch (IOException e) {
                logger.error("Không thể xóa ảnh từ thư mục local: {}", sanPham.getUrlHinhAnh(), e);
            }
        }
        sanPhamRepository.deleteById(id);
    }

    public List<SanPham> findByTenSanPhamContaining(String name) {
        List<SanPham> sanPhams = sanPhamRepository.findByTenSanPhamContainingIgnoreCase(name);
        sanPhams.forEach(this::setTongSoLuong);
        sanPhams.forEach(this::autoUpdateStatusBasedOnDetails);
        return sanPhams;
    }

    public Page<SanPham> findByTenSanPhamContaining(String searchName, Boolean trangThai, Pageable pageable) {
        Pageable sortedByThoiGianTaoDesc = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "thoiGianTao"));
        Page<SanPham> sanPhamPage;
        if (trangThai != null) {
            sanPhamPage = sanPhamRepository.findByTenSanPhamContainingIgnoreCaseAndTrangThai(searchName, trangThai, sortedByThoiGianTaoDesc);
        } else {
            sanPhamPage = sanPhamRepository.findByTenSanPhamContainingIgnoreCase(searchName, sortedByThoiGianTaoDesc);
        }
        sanPhamPage.getContent().forEach(this::setTongSoLuong);
        sanPhamPage.getContent().forEach(this::autoUpdateStatusBasedOnDetails);
        return sanPhamPage;
    }

    public Page<SanPham> findByTrangThai(Boolean trangThai, Pageable pageable) {
        Pageable sortedByThoiGianTaoDesc = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "thoiGianTao"));
        Page<SanPham> sanPhamPage = sanPhamRepository.findByTrangThai(trangThai, sortedByThoiGianTaoDesc);
        sanPhamPage.getContent().forEach(this::setTongSoLuong);
        sanPhamPage.getContent().forEach(this::autoUpdateStatusBasedOnDetails);
        return sanPhamPage;
    }

    public Page<SanPham> getPagedProducts(Pageable pageable) {
        Page<SanPham> sanPhamPage = sanPhamRepository.findAll(pageable);
        sanPhamPage.getContent().forEach(this::setTongSoLuong);
        sanPhamPage.getContent().forEach(this::autoUpdateStatusBasedOnDetails);
        return sanPhamPage;
    }

    public List<SanPham> getAll() {
        List<SanPham> sanPhams = sanPhamRepository.findAll();
        sanPhams.forEach(this::setTongSoLuong);
        sanPhams.forEach(this::autoUpdateStatusBasedOnDetails);
        return sanPhams;
    }

    public Page<SanPham> searchByTenOrMa(String keyword, Pageable pageable) {
        Page<SanPham> sanPhamPage = sanPhamRepository.findByTenSanPhamContainingIgnoreCaseOrMaSanPhamContainingIgnoreCase(keyword, keyword, pageable);
        sanPhamPage.getContent().forEach(this::setTongSoLuong);
        sanPhamPage.getContent().forEach(this::autoUpdateStatusBasedOnDetails);
        return sanPhamPage;
    }

    private void setTongSoLuong(SanPham sanPham) {
        long tongSoLuong = chiTietSanPhamRepository.findBySanPhamId(sanPham.getId())
                .stream()
                .mapToLong(ChiTietSanPham::getSoLuongTonKho)
                .sum();
        sanPham.setTongSoLuong(tongSoLuong);
    }

    public boolean hasActiveChiTietSanPham(UUID sanPhamId) {
        List<ChiTietSanPham> chiTietList = chiTietSanPhamRepository.findBySanPhamId(sanPhamId);
        return chiTietList.stream().anyMatch(ct -> Boolean.TRUE.equals(ct.getTrangThai()));
    }

    private void autoUpdateStatusBasedOnDetails(SanPham sanPham) {
        if (sanPham.getTrangThai() && !hasActiveChiTietSanPham(sanPham.getId())) {
            sanPham.setTrangThai(false);
            save(sanPham);
        }
    }



    public List<SanPham> getSanPhamLienQuan(UUID idSanPham, int limit) {
        SanPham sanPhamGoc = sanPhamRepository.findById(idSanPham)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm gốc"));

        UUID idDanhMuc = sanPhamGoc.getDanhMuc().getId();
        String tenSanPham = sanPhamGoc.getTenSanPham();

        Pageable pageable = PageRequest.of(0, limit);
        return sanPhamRepository.findSanPhamLienQuan(idSanPham, idDanhMuc, tenSanPham.toLowerCase(), pageable);
    }
    public boolean existsByMaSanPham(String maSanPham) {
        return sanPhamRepository.existsByMaSanPham(maSanPham);
    }

    public boolean existsByTenSanPham(String tenSanPham) {
        return sanPhamRepository.existsByTenSanPham(tenSanPham);
    }



}