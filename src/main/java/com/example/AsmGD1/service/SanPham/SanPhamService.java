package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.dto.ChatBot.ChiTietSanPhamDTO;
import com.example.AsmGD1.dto.ChatBot.HoaDonSanPhamDTO;
import com.example.AsmGD1.dto.ChatBot.SanPhamWithChiTietDTO;
import com.example.AsmGD1.dto.SanPham.SanPhamDto;
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
                                               UUID chatLieuId, UUID xuatXuId, UUID tayAoId, UUID coAoId,
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
                tayAoId,
                coAoId,
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

    public List<SanPhamDto> getAllSanPhamDtos() {
        List<SanPham> danhSach = sanPhamRepository.findAllByTrangThai();
        return danhSach.stream().map(sp -> {
            SanPhamDto dto = new SanPhamDto();
            dto.setId(sp.getId());
            dto.setMaSanPham(sp.getMaSanPham());
            dto.setTenSanPham(sp.getTenSanPham());
            dto.setMoTa(sp.getMoTa());
            dto.setUrlHinhAnh(sp.getUrlHinhAnh());
            dto.setTrangThai(sp.getTrangThai());
            dto.setThoiGianTao(sp.getThoiGianTao());
            dto.setTongSoLuong(sp.getTongSoLuong());

            if (sp.getDanhMuc() != null) {
                dto.setDanhMucId(sp.getDanhMuc().getId());
                dto.setTenDanhMuc(sp.getDanhMuc().getTenDanhMuc());
            }

            dto.setPrice(sp.getMinPrice().toString());
            dto.setOldPrice(sp.getMaxPrice().toString());
            dto.setSold(String.valueOf(sp.getTotalStockQuantity()));
            return dto;
        }).toList();
    }

    public List<SanPhamWithChiTietDTO> getSanPhamWithChiTietDTOs() {
        List<SanPham> danhSachSanPham = sanPhamRepository.findAll();

        return danhSachSanPham.stream().map(sp -> {
            SanPhamWithChiTietDTO dto = new SanPhamWithChiTietDTO();
            dto.setId(sp.getId());
            dto.setMaSanPham(sp.getMaSanPham());
            dto.setTenSanPham(sp.getTenSanPham());
            dto.setTenDanhMuc(sp.getDanhMuc().getTenDanhMuc());
            dto.setUrlHinhAnh(sp.getUrlHinhAnh());
            dto.setTongSoLuong(sp.getTongSoLuong());

            // 1. Set chi tiết sản phẩm như cũ
            dto.setChiTietSanPhams(sp.getChiTietSanPhams().stream().map(ct -> {
                ChiTietSanPhamDTO ctDto = new ChiTietSanPhamDTO();
                ctDto.setId(ct.getId());
                ctDto.setKichCo(ct.getKichCo().getTen());
                ctDto.setMauSac(ct.getMauSac().getTenMau());
                ctDto.setChatLieu(ct.getChatLieu().getTenChatLieu());
                ctDto.setXuatXu(ct.getXuatXu().getTenXuatXu());
                ctDto.setGia(ct.getGia());
                ctDto.setSoLuongTonKho(ct.getSoLuongTonKho());
                List<String> urls = (ct.getHinhAnhSanPhams() == null) ? List.of()
                        : ct.getHinhAnhSanPhams().stream()
                        .sorted(Comparator.comparing(
                                ha -> ha.getThuTu() == null ? Integer.MAX_VALUE : ha.getThuTu()
                        ))
                        .map(HinhAnhSanPham::getUrlHinhAnh)
                        .toList();

                ctDto.setHinhAnhUrls(urls);
                return ctDto;
            }).collect(Collectors.toList()));

            // 2. Lấy hóa đơn liên quan đến sản phẩm
            List<ChiTietDonHang> donHangs = chiTietDonHangRepository.findBySanPhamId(sp.getId());

            List<HoaDonSanPhamDTO> hoaDonDtos = donHangs.stream().map(ct -> {
                HoaDon hoaDon = hoaDonRepository.findByDonHang_Id(ct.getDonHang().getId());
                if (hoaDon == null) return null; // tránh null pointer nếu đơn hàng chưa có hóa đơn

                HoaDonSanPhamDTO hdDto = new HoaDonSanPhamDTO();
                hdDto.setIdHoaDon(hoaDon.getId());
                hdDto.setIdDonHang(ct.getDonHang().getId());
                hdDto.setIdChiTietSanPham(ct.getChiTietSanPham().getId());
                hdDto.setTenSanPham(ct.getTenSanPham());
                hdDto.setSoLuong(ct.getSoLuong());
                hdDto.setGia(ct.getGia());
                hdDto.setThanhTien(ct.getThanhTien());
                hdDto.setGhiChu(ct.getGhiChu());
                hdDto.setNgayTaoHoaDon(hoaDon.getNgayTao());
                return hdDto;
            }).filter(Objects::nonNull).collect(Collectors.toList());

            dto.setHoaDonSanPhams(hoaDonDtos);

            return dto;
        }).collect(Collectors.toList());
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

    public Page<SanPham> getPagedAvailableProducts(Pageable pageable) {
        Page<SanPham> page = sanPhamRepository.findAvailableProducts(LocalDateTime.now(), pageable);
        page.getContent().forEach(this::setTongSoLuong);
        page.getContent().forEach(this::autoUpdateStatusBasedOnDetails);
        return page;
    }

    public Page<SanPham> searchAvailableByTenOrMa(String keyword, Pageable pageable) {
        Page<SanPham> page = sanPhamRepository.searchAvailableByTenOrMa(
                keyword == null ? "" : keyword.trim(),
                LocalDateTime.now(),
                pageable
        );
        page.getContent().forEach(this::setTongSoLuong);
        page.getContent().forEach(this::autoUpdateStatusBasedOnDetails);
        return page;
    }
    public Page<SanPham> getPagedAvailableOrSelectedProducts(Pageable pageable, Collection<UUID> selectedIds) {
        Page<SanPham> base = sanPhamRepository.findAvailableProducts(LocalDateTime.now(), pageable);
        if (selectedIds == null || selectedIds.isEmpty()) return base;

        LinkedHashMap<UUID, SanPham> map = new LinkedHashMap<>();
        // ƯU TIÊN giữ các SP đã chọn (dù không “rảnh”)
        sanPhamRepository.findAllById(selectedIds).forEach(sp -> map.put(sp.getId(), sp));
        // Sau đó thêm các SP “rảnh”
        base.getContent().forEach(sp -> map.putIfAbsent(sp.getId(), sp));

        List<SanPham> all = new ArrayList<>(map.values());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<SanPham> content = start >= end ? List.of() : all.subList(start, end);

        content.forEach(this::setTongSoLuong);
        content.forEach(this::autoUpdateStatusBasedOnDetails);

        return new PageImpl<>(content, pageable, all.size());
    }

    public Page<SanPham> searchAvailableOrSelectedByTenOrMa(String keyword, Pageable pageable, Collection<UUID> selectedIds) {
        Page<SanPham> base = sanPhamRepository.searchAvailableByTenOrMa(
                keyword == null ? "" : keyword.trim(),
                LocalDateTime.now(),
                pageable
        );
        if (selectedIds == null || selectedIds.isEmpty()) return base;

        LinkedHashMap<UUID, SanPham> map = new LinkedHashMap<>();
        sanPhamRepository.findAllById(selectedIds).forEach(sp -> map.put(sp.getId(), sp));
        base.getContent().forEach(sp -> map.putIfAbsent(sp.getId(), sp));

        List<SanPham> all = new ArrayList<>(map.values());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<SanPham> content = start >= end ? List.of() : all.subList(start, end);

        content.forEach(this::setTongSoLuong);
        content.forEach(this::autoUpdateStatusBasedOnDetails);

        return new PageImpl<>(content, pageable, all.size());
    }

}