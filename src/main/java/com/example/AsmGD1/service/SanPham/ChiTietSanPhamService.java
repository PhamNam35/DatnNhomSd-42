package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.dto.ChiTietSanPham.ChiTietSanPhamBatchDto;
import com.example.AsmGD1.dto.ChiTietSanPham.ChiTietSanPhamUpdateDto;
import com.example.AsmGD1.dto.ChiTietSanPham.ChiTietSanPhamVariationDto;
import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.SanPham.*;
import com.example.AsmGD1.util.QRCodeUtil;
import com.google.zxing.WriterException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChiTietSanPhamService {
    private static final Logger logger = LoggerFactory.getLogger(ChiTietSanPhamService.class);

    @Autowired private ChiTietSanPhamRepository chiTietSanPhamRepo;
    @Autowired private HinhAnhSanPhamRepository hinhAnhSanPhamRepo;
    @Autowired private SanPhamRepository sanPhamRepo;
    @Autowired private MauSacRepository mauSacRepo;
    @Autowired private KichCoRepository kichCoRepo;
    @Autowired private ChatLieuRepository chatLieuRepo;
    @Autowired private XuatXuRepository xuatXuRepo;
    @Autowired private TayAoRepository tayAoRepo;
    @Autowired private CoAoRepository coAoRepo;
    @Autowired private KieuDangRepository kieuDangRepo;
    @Autowired private ThuongHieuRepository thuongHieuRepo;
    @Autowired private SimpMessagingTemplate messagingTemplate; // Thêm để gửi message WebSocket

    private final String UPLOAD_DIR;

    public ChiTietSanPhamService() {
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




    @PostConstruct
    public void syncAllProductDetailsQRCode() {
        List<ChiTietSanPham> allDetails = chiTietSanPhamRepo.findAll();

        Set<String> validFileNames = allDetails.stream()
                .map(detail -> "qr_" + detail.getId() + ".png")
                .collect(Collectors.toSet());

        File qrDirectory = new File(QRCodeUtil.getBaseDir());

        if (qrDirectory.exists() && qrDirectory.isDirectory()) {
            File[] existingFiles = qrDirectory.listFiles((dir, name) -> name.endsWith(".png"));
            if (existingFiles != null) {
                for (File file : existingFiles) {
                    if (!validFileNames.contains(file.getName())) {
                        boolean deleted = file.delete();
                        if (deleted) {
                            logger.info("🗑️ Đã xóa file QR không còn hợp lệ: {}", file.getName());
                        }
                    }
                }
            }
        }

        for (ChiTietSanPham detail : allDetails) {
            String qrFileName = "qr_" + detail.getId() + ".png";
            Path qrFilePath = Paths.get(QRCodeUtil.getBaseDir(), qrFileName);

            if (!Files.exists(qrFilePath)) {
                try {
                    QRCodeUtil.generateQRCodeImage(detail.getId().toString(), 250, 250, qrFileName);
                    logger.info("✅ QR code created for product detail ID: {}", detail.getId());
                } catch (Exception e) {
                    logger.error("❌ Lỗi khi tạo QR cho ID: {}", detail.getId(), e);
                }
            }
        }
    }

    @Transactional
    public void deleteChiTietSanPham(UUID id) {
        ChiTietSanPham chiTiet = chiTietSanPhamRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết sản phẩm với ID: " + id));

        String qrFileName = "qr_" + chiTiet.getId() + ".png";
        Path qrPath = Paths.get(QRCodeUtil.getBaseDir(), qrFileName);
        try {
            if (Files.exists(qrPath)) {
                Files.delete(qrPath);
                logger.info("🗑️ Đã xoá QR Code file: {}", qrPath);
            }
        } catch (IOException e) {
            logger.error("❌ Lỗi khi xóa QR Code file: {}", qrPath, e);
        }

        chiTietSanPhamRepo.delete(chiTiet);

        // Gửi thông báo WebSocket đến topic của sản phẩm
        UUID sanPhamId = chiTiet.getSanPham().getId();
        messagingTemplate.convertAndSend("/topic/product/" + sanPhamId, Map.of("action", "refresh"));
    }

    public List<ChiTietSanPham> findAllByTrangThaiAndKeyword(String keyword) {
        return chiTietSanPhamRepo.findAllByTrangThaiAndKeyword(keyword);
    }

    public List<ChiTietSanPham> findByFilters(UUID productId, UUID colorId, UUID sizeId, UUID originId, UUID materialId,
                                              UUID styleId, UUID sleeveId, UUID collarId, UUID brandId, String gender, Boolean status) {
        StringBuilder query = new StringBuilder("SELECT ct FROM ChiTietSanPham ct " +
                "JOIN FETCH ct.sanPham sp " +
                "JOIN FETCH ct.kichCo kc " +
                "JOIN FETCH ct.mauSac ms " +
                "WHERE sp.id = :productId");
        Map<String, Object> params = new HashMap<>();
        params.put("productId", productId);

        if (colorId != null) {
            query.append(" AND ct.mauSac.id = :colorId");
            params.put("colorId", colorId);
        }
        if (sizeId != null) {
            query.append(" AND ct.kichCo.id = :sizeId");
            params.put("sizeId", sizeId);
        }
        if (originId != null) {
            query.append(" AND ct.xuatXu.id = :originId");
            params.put("originId", originId);
        }
        if (materialId != null) {
            query.append(" AND ct.chatLieu.id = :materialId");
            params.put("materialId", materialId);
        }
        if (styleId != null) {
            query.append(" AND ct.kieuDang.id = :styleId");
            params.put("styleId", styleId);
        }
        if (sleeveId != null) {
            query.append(" AND ct.tayAo.id = :sleeveId");
            params.put("sleeveId", sleeveId);
        }
        if (collarId != null) {
            query.append(" AND ct.coAo.id = :collarId");
            params.put("collarId", collarId);
        }
        if (brandId != null) {
            query.append(" AND ct.thuongHieu.id = :brandId");
            params.put("brandId", brandId);
        }
        if (gender != null && !gender.isEmpty()) {
            query.append(" AND ct.gioiTinh = :gender");
            params.put("gender", gender);
        }
        if (status != null) {
            query.append(" AND ct.trangThai = :status");
            params.put("status", status);
        }

        return chiTietSanPhamRepo.findByDynamicQuery(query.toString(), params);
    }

    @Transactional
    public void saveChiTietSanPhamVariationsDto(ChiTietSanPhamBatchDto batchDto) {
        SanPham sanPham = sanPhamRepo.findById(batchDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại ID: " + batchDto.getProductId()));
        XuatXu xuatXu = xuatXuRepo.findById(batchDto.getOriginId())
                .orElseThrow(() -> new RuntimeException("Xuất xứ không tồn tại ID: " + batchDto.getOriginId()));
        ChatLieu chatLieu = chatLieuRepo.findById(batchDto.getMaterialId())
                .orElseThrow(() -> new RuntimeException("Chất liệu không tồn tại ID: " + batchDto.getMaterialId()));
        KieuDang kieuDang = kieuDangRepo.findById(batchDto.getStyleId())
                .orElseThrow(() -> new RuntimeException("Kiểu dáng không tồn tại ID: " + batchDto.getStyleId()));
        TayAo tayAo = tayAoRepo.findById(batchDto.getSleeveId())
                .orElseThrow(() -> new RuntimeException("Tay áo không tồn tại ID: " + batchDto.getSleeveId()));
        CoAo coAo = coAoRepo.findById(batchDto.getCollarId())
                .orElseThrow(() -> new RuntimeException("Cổ áo không tồn tại ID: " + batchDto.getCollarId()));
        ThuongHieu thuongHieu = thuongHieuRepo.findById(batchDto.getBrandId())
                .orElseThrow(() -> new RuntimeException("Thương hiệu không tồn tại ID: " + batchDto.getBrandId()));

        for (ChiTietSanPhamVariationDto variationDto : batchDto.getVariations()) {
            if (variationDto.getColorId() == null || variationDto.getSizeId() == null ||
                    variationDto.getPrice() == null || variationDto.getStockQuantity() == null) {
                continue;
            }

            MauSac mauSac = mauSacRepo.findById(variationDto.getColorId())
                    .orElseThrow(() -> new RuntimeException("Màu sắc không tồn tại ID: " + variationDto.getColorId()));
            KichCo kichCo = kichCoRepo.findById(variationDto.getSizeId())
                    .orElseThrow(() -> new RuntimeException("Kích cỡ không tồn tại ID: " + variationDto.getSizeId()));

            ChiTietSanPham existing = chiTietSanPhamRepo.findBySanPhamIdAndMauSacIdAndKichCoId(
                    sanPham.getId(), mauSac.getId(), kichCo.getId());
            if (existing != null) {
                logger.warn("Biến thể đã tồn tại: productId={}, colorId={}, sizeId={}",
                        sanPham.getId(), mauSac.getId(), kichCo.getId());
                continue;
            }

            ChiTietSanPham pd = new ChiTietSanPham();
            pd.setSanPham(sanPham);
            pd.setXuatXu(xuatXu);
            pd.setChatLieu(chatLieu);
            pd.setKieuDang(kieuDang);
            pd.setTayAo(tayAo);
            pd.setCoAo(coAo);
            pd.setThuongHieu(thuongHieu);
            pd.setMauSac(mauSac);
            pd.setKichCo(kichCo);
            pd.setGia(variationDto.getPrice());
            pd.setSoLuongTonKho(variationDto.getStockQuantity());
            pd.setGioiTinh(batchDto.getGender());
            pd.setThoiGianTao(LocalDateTime.now());
            pd.setTrangThai(variationDto.getStockQuantity() > 0);

            ChiTietSanPham savedDetail = chiTietSanPhamRepo.save(pd);

            try {
                QRCodeUtil.generateQRCodeForProduct(savedDetail.getId());
            } catch (IOException | WriterException e) {
                logger.error("Không thể tạo QR Code cho biến thể sản phẩm ID: {}", savedDetail.getId(), e);
            }

            List<MultipartFile> variationImages = batchDto.getColorImages() != null
                    ? batchDto.getColorImages().getOrDefault(variationDto.getColorId(), new ArrayList<>())
                    : new ArrayList<>();
            if (!variationImages.isEmpty()) {
                saveImagesToLocal(savedDetail, variationImages.stream().limit(3).collect(Collectors.toList()));
            }
        }

        // Gửi thông báo WebSocket đến topic của sản phẩm
        messagingTemplate.convertAndSend("/topic/product/" + batchDto.getProductId(), Map.of("action", "refresh"));
    }

    @Transactional
    public void saveSingleChiTietSanPham(ChiTietSanPhamUpdateDto dto, List<MultipartFile> imageFiles) {
        SanPham sanPham = sanPhamRepo.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại ID: " + dto.getProductId()));
        MauSac mauSac = mauSacRepo.findById(dto.getColorId())
                .orElseThrow(() -> new RuntimeException("Màu sắc không tồn tại ID: " + dto.getColorId()));
        KichCo kichCo = kichCoRepo.findById(dto.getSizeId())
                .orElseThrow(() -> new RuntimeException("Kích cỡ không tồn tại ID: " + dto.getSizeId()));
        XuatXu xuatXu = xuatXuRepo.findById(dto.getOriginId())
                .orElseThrow(() -> new RuntimeException("Xuất xứ không tồn tại ID: " + dto.getOriginId()));
        ChatLieu chatLieu = chatLieuRepo.findById(dto.getMaterialId())
                .orElseThrow(() -> new RuntimeException("Chất liệu không tồn tại ID: " + dto.getMaterialId()));
        KieuDang kieuDang = kieuDangRepo.findById(dto.getStyleId())
                .orElseThrow(() -> new RuntimeException("Kiểu dáng không tồn tại ID: " + dto.getStyleId()));
        TayAo tayAo = tayAoRepo.findById(dto.getSleeveId())
                .orElseThrow(() -> new RuntimeException("Tay áo không tồn tại ID: " + dto.getSleeveId()));
        CoAo coAo = coAoRepo.findById(dto.getCollarId())
                .orElseThrow(() -> new RuntimeException("Cổ áo không tồn tại ID: " + dto.getCollarId()));
        ThuongHieu thuongHieu = thuongHieuRepo.findById(dto.getBrandId())
                .orElseThrow(() -> new RuntimeException("Thương hiệu không tồn tại ID: " + dto.getBrandId()));

        // Kiểm tra trạng thái khi thêm mới
        boolean newStatus = dto.getStatus() != null ? dto.getStatus() : dto.getStockQuantity() > 0;
        if (newStatus && dto.getStockQuantity() == 0) {
            throw new RuntimeException("Không thể bật trạng thái 'Đang Bán' khi số lượng tồn kho bằng 0!");
        }

        ChiTietSanPham pd = new ChiTietSanPham();
        pd.setSanPham(sanPham);
        pd.setMauSac(mauSac);
        pd.setKichCo(kichCo);
        pd.setXuatXu(xuatXu);
        pd.setChatLieu(chatLieu);
        pd.setKieuDang(kieuDang);
        pd.setTayAo(tayAo);
        pd.setCoAo(coAo);
        pd.setThuongHieu(thuongHieu);
        pd.setGia(dto.getPrice());
        pd.setSoLuongTonKho(dto.getStockQuantity());
        pd.setGioiTinh(dto.getGender());
        pd.setThoiGianTao(LocalDateTime.now());
        pd.setTrangThai(newStatus);

        ChiTietSanPham savedDetail = chiTietSanPhamRepo.save(pd);

        if (imageFiles != null && !imageFiles.isEmpty()) {
            saveImagesToLocal(savedDetail, imageFiles);
        }

        // Gửi thông báo WebSocket đến topic của sản phẩm
        messagingTemplate.convertAndSend("/topic/product/" + dto.getProductId(), Map.of("action", "refresh"));
    }

    @Transactional
    public void updateChiTietSanPham(ChiTietSanPhamUpdateDto updateDto, MultipartFile[] imageFiles, List<UUID> deletedImageIds) {
        ChiTietSanPham existingDetail = chiTietSanPhamRepo.findById(updateDto.getId())
                .orElseThrow(() -> new RuntimeException("Chi tiết sản phẩm không tồn tại ID: " + updateDto.getId()));

        // Cập nhật các thuộc tính sản phẩm
        existingDetail.setSanPham(sanPhamRepo.findById(updateDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại")));
        existingDetail.setMauSac(mauSacRepo.findById(updateDto.getColorId())
                .orElseThrow(() -> new RuntimeException("Màu sắc không tồn tại")));
        existingDetail.setKichCo(kichCoRepo.findById(updateDto.getSizeId())
                .orElseThrow(() -> new RuntimeException("Kích cỡ không tồn tại")));
        existingDetail.setXuatXu(xuatXuRepo.findById(updateDto.getOriginId())
                .orElseThrow(() -> new RuntimeException("Xuất xứ không tồn tại")));
        existingDetail.setChatLieu(chatLieuRepo.findById(updateDto.getMaterialId())
                .orElseThrow(() -> new RuntimeException("Chất liệu không tồn tại")));
        existingDetail.setKieuDang(kieuDangRepo.findById(updateDto.getStyleId())
                .orElseThrow(() -> new RuntimeException("Kiểu dáng không tồn tại")));
        existingDetail.setTayAo(tayAoRepo.findById(updateDto.getSleeveId())
                .orElseThrow(() -> new RuntimeException("Tay áo không tồn tại")));
        existingDetail.setCoAo(coAoRepo.findById(updateDto.getCollarId())
                .orElseThrow(() -> new RuntimeException("Cổ áo không tồn tại")));
        existingDetail.setThuongHieu(thuongHieuRepo.findById(updateDto.getBrandId())
                .orElseThrow(() -> new RuntimeException("Thương hiệu không tồn tại")));
        existingDetail.setGia(updateDto.getPrice());
        existingDetail.setSoLuongTonKho(updateDto.getStockQuantity());
        existingDetail.setGioiTinh(updateDto.getGender());

        // Kiểm tra trạng thái: Không cho phép bật trạng thái nếu số lượng tồn kho = 0
        boolean newStatus = updateDto.getStatus() != null ? updateDto.getStatus() : updateDto.getStockQuantity() > 0;
        if (newStatus && updateDto.getStockQuantity() == 0) {
            throw new RuntimeException("Không thể bật trạng thái 'Đang Bán' khi số lượng tồn kho bằng 0!");
        }
        existingDetail.setTrangThai(newStatus);

        // Lưu chi tiết sản phẩm trước để đảm bảo ID tồn tại
        chiTietSanPhamRepo.save(existingDetail);

        // Xóa các ảnh được chọn
        if (deletedImageIds != null && !deletedImageIds.isEmpty()) {
            for (UUID imageId : deletedImageIds) {
                deleteImage(imageId);
            }
            // Xóa danh sách ảnh hiện tại trong đối tượng để đảm bảo đồng bộ
            existingDetail.setHinhAnhSanPhams(new ArrayList<>());
            chiTietSanPhamRepo.save(existingDetail);
        }

        // Thêm ảnh mới nếu có
        if (imageFiles != null && imageFiles.length > 0) {
            saveImagesToLocal(existingDetail, Arrays.asList(imageFiles));
        }

        // Làm mới danh sách ảnh từ cơ sở dữ liệu
        List<HinhAnhSanPham> updatedImages = hinhAnhSanPhamRepo.findByChiTietSanPhamIdOrderByThuTu(existingDetail.getId());
        logger.info("Danh sách ảnh sau cập nhật cho ID {}: {} ảnh", existingDetail.getId(), updatedImages.size());
        existingDetail.setHinhAnhSanPhams(updatedImages);

        // Lưu lại lần cuối để đảm bảo tất cả thay đổi được áp dụng
        chiTietSanPhamRepo.save(existingDetail);

        // Gửi thông báo WebSocket đến topic của sản phẩm
        UUID sanPhamId = existingDetail.getSanPham().getId();
        messagingTemplate.convertAndSend("/topic/product/" + sanPhamId, Map.of("action", "refresh"));
    }

    @Transactional
    public void updateChiTietSanPhamRestricted(ChiTietSanPhamUpdateDto updateDto,
                                               MultipartFile[] imageFiles,
                                               List<UUID> deletedImageIds) {
        ChiTietSanPham existing = chiTietSanPhamRepo.findById(updateDto.getId())
                .orElseThrow(() -> new RuntimeException("Chi tiết sản phẩm không tồn tại ID: " + updateDto.getId()));

        // chỉ cho phép sửa:
        existing.setGia(updateDto.getPrice());
        existing.setSoLuongTonKho(updateDto.getStockQuantity());

        boolean newStatus = updateDto.getStatus() != null ? updateDto.getStatus()
                : (updateDto.getStockQuantity() != null && updateDto.getStockQuantity() > 0);
        if (newStatus && (updateDto.getStockQuantity() == null || updateDto.getStockQuantity() == 0)) {
            throw new RuntimeException("Không thể bật trạng thái 'Đang Bán' khi số lượng tồn kho bằng 0!");
        }
        existing.setTrangThai(newStatus);

        chiTietSanPhamRepo.save(existing);

        // Ảnh: y hệt logic cũ
        if (deletedImageIds != null && !deletedImageIds.isEmpty()) {
            for (UUID imageId : deletedImageIds) deleteImage(imageId);
            existing.setHinhAnhSanPhams(new ArrayList<>());
            chiTietSanPhamRepo.save(existing);
        }
        if (imageFiles != null && imageFiles.length > 0) {
            saveImagesToLocal(existing, Arrays.asList(imageFiles));
        }

        List<HinhAnhSanPham> updatedImages =
                hinhAnhSanPhamRepo.findByChiTietSanPhamIdOrderByThuTu(existing.getId());
        existing.setHinhAnhSanPhams(updatedImages);
        chiTietSanPhamRepo.save(existing);

        // Gửi thông báo WebSocket đến topic của sản phẩm
        UUID sanPhamId = existing.getSanPham().getId();
        messagingTemplate.convertAndSend("/topic/product/" + sanPhamId, Map.of("action", "refresh"));
    }


    @Transactional
    public int updateBulkFullAttributes(List<UUID> ids, ChiTietSanPhamUpdateDto dto) {
        int count = 0;
        Set<UUID> sanPhamIds = new HashSet<>(); // Để gửi WebSocket cho các sản phẩm liên quan
        for (UUID id : ids) {
            ChiTietSanPham existing = chiTietSanPhamRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết sản phẩm: " + id));

            // cập nhật full thuộc tính
            existing.setXuatXu(xuatXuRepo.findById(dto.getOriginId())
                    .orElseThrow(() -> new RuntimeException("Xuất xứ không tồn tại")));
            existing.setChatLieu(chatLieuRepo.findById(dto.getMaterialId())
                    .orElseThrow(() -> new RuntimeException("Chất liệu không tồn tại")));
            existing.setKieuDang(kieuDangRepo.findById(dto.getStyleId())
                    .orElseThrow(() -> new RuntimeException("Kiểu dáng không tồn tại")));
            existing.setTayAo(tayAoRepo.findById(dto.getSleeveId())
                    .orElseThrow(() -> new RuntimeException("Tay áo không tồn tại")));
            existing.setCoAo(coAoRepo.findById(dto.getCollarId())
                    .orElseThrow(() -> new RuntimeException("Cổ áo không tồn tại")));
            existing.setThuongHieu(thuongHieuRepo.findById(dto.getBrandId())
                    .orElseThrow(() -> new RuntimeException("Thương hiệu không tồn tại")));
            existing.setGioiTinh(dto.getGender());
            existing.setGia(dto.getPrice());
            existing.setSoLuongTonKho(dto.getStockQuantity());

            boolean newStatus = dto.getStatus() != null ? dto.getStatus() : dto.getStockQuantity() > 0;
            if (newStatus && dto.getStockQuantity() == 0) {
                throw new RuntimeException("Không thể bật 'Đang Bán' khi tồn kho bằng 0!");
            }
            existing.setTrangThai(newStatus);

            chiTietSanPhamRepo.save(existing);
            count++;
            sanPhamIds.add(existing.getSanPham().getId());
        }

        // Gửi thông báo WebSocket đến tất cả các topic sản phẩm liên quan
        for (UUID sanPhamId : sanPhamIds) {
            messagingTemplate.convertAndSend("/topic/product/" + sanPhamId, Map.of("action", "refresh"));
        }

        return count;
    }


    @Transactional
    public void deleteImage(UUID imageId) {
        Optional<HinhAnhSanPham> optionalImage = hinhAnhSanPhamRepo.findById(imageId);

        if (optionalImage.isEmpty()) {
            logger.warn("Ảnh với ID {} không tồn tại trong CSDL, bỏ qua xóa.", imageId);
            return;
        }

        HinhAnhSanPham image = optionalImage.get();

        try {
            String fileName = null;
            if (image.getUrlHinhAnh() != null) {
                fileName = image.getUrlHinhAnh().replace("/images/", "");
            }

            if (fileName != null) {
                Path filePath = Paths.get(UPLOAD_DIR, fileName);
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    logger.info("Đã xóa tệp ảnh: {}", filePath);
                } else {
                    logger.warn("Tệp ảnh không tồn tại trên ổ đĩa: {}", filePath);
                }
            }

            hinhAnhSanPhamRepo.delete(image);
            logger.info("Đã xóa bản ghi ảnh trong CSDL: {}", imageId);

        } catch (IOException e) {
            throw new RuntimeException("Không thể xóa ảnh từ thư mục local: " + e.getMessage());
        }
    }


    public void saveImagesToLocal(ChiTietSanPham chiTietSanPham, List<MultipartFile> imageFiles) {
        if (imageFiles == null || imageFiles.isEmpty()) {
            logger.warn("Không có ảnh để lưu cho chi tiết sản phẩm ID: {}", chiTietSanPham.getId());
            return;
        }

        List<HinhAnhSanPham> existingImages = hinhAnhSanPhamRepo.findByChiTietSanPhamIdOrderByThuTu(chiTietSanPham.getId());
        int nextOrder = existingImages.size(); // Bắt đầu thứ tự sau ảnh cũ

        for (MultipartFile imageFile : imageFiles) {
            if (imageFile == null || imageFile.isEmpty()) {
                logger.warn("Tệp ảnh rỗng, bỏ qua.");
                continue;
            }

            String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
            Path filePath = Paths.get(UPLOAD_DIR, fileName);
            try {
                Files.createDirectories(filePath.getParent());
                Files.copy(imageFile.getInputStream(), filePath);
                String imageUrl = "/images/" + fileName;

                HinhAnhSanPham newImage = new HinhAnhSanPham();
                newImage.setChiTietSanPham(chiTietSanPham);
                newImage.setUrlHinhAnh(imageUrl);
                newImage.setThuTu(nextOrder++);
                hinhAnhSanPhamRepo.save(newImage);

                logger.info("Đã lưu ảnh mới: {} cho chi tiết sản phẩm ID: {}", imageUrl, chiTietSanPham.getId());
            } catch (IOException e) {
                logger.error("Không thể lưu ảnh {}: {}", fileName, e.getMessage());
                throw new RuntimeException("Không thể lưu ảnh mới: " + e.getMessage(), e);
            }
        }
    }

    public List<HinhAnhSanPham> findHinhAnhSanPhamByChiTietSanPhamIdOrdered(UUID chiTietSanPhamId) {
        List<HinhAnhSanPham> images = hinhAnhSanPhamRepo.findByChiTietSanPhamIdOrderByThuTu(chiTietSanPhamId);
        logger.info("Tìm thấy {} ảnh cho chi tiết sản phẩm ID: {}", images.size(), chiTietSanPhamId);
        images.forEach(img -> logger.info("Ảnh ID: {}, URL: {}, ThuTu: {}", img.getId(), img.getUrlHinhAnh(), img.getThuTu()));
        return images;
    }

    public List<ChiTietSanPham> findAll() {
        return chiTietSanPhamRepo.findAll();
    }

    public List<ChiTietSanPham> findByProductId(UUID productId) {
        return chiTietSanPhamRepo.findBySanPhamId(productId);
    }

    public ChiTietSanPham findById(UUID id) {
        ChiTietSanPham chiTiet = chiTietSanPhamRepo.findById(id).orElse(null);
        return chiTiet;
    }

    public ChiTietSanPham findBySanPhamIdAndMauSacIdAndKichCoId(UUID productId, UUID mauSacId, UUID kichCoId) {
        return chiTietSanPhamRepo.findBySanPhamIdAndMauSacIdAndKichCoId(productId, mauSacId, kichCoId);
    }

    public List<MauSac> findColorsByProductId(UUID productId) {
        return chiTietSanPhamRepo.findBySanPhamId(productId)
                .stream()
                .map(ChiTietSanPham::getMauSac)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<KichCo> findSizesByProductId(UUID productId) {
        return chiTietSanPhamRepo.findBySanPhamId(productId)
                .stream()
                .map(ChiTietSanPham::getKichCo)
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional
    public ChiTietSanPham save(ChiTietSanPham chiTietSanPham) {
        ChiTietSanPham saved = chiTietSanPhamRepo.save(chiTietSanPham);

        // Gửi thông báo WebSocket đến topic của sản phẩm
        UUID sanPhamId = chiTietSanPham.getSanPham().getId();
        messagingTemplate.convertAndSend("/topic/product/" + sanPhamId, Map.of("action", "refresh"));

        return saved;
    }

    public List<ChiTietSanPham> findAllByTrangThai() {
        return chiTietSanPhamRepo.findAllByTrangThai();
    }

    @Transactional
    public void saveChiTietSanPhamWithImages(ChiTietSanPham pd, MultipartFile[] imageFiles) {
        ChiTietSanPham savedDetail = chiTietSanPhamRepo.save(pd);

        try {
            QRCodeUtil.generateQRCodeForProduct(savedDetail.getId());
        } catch (IOException | WriterException e) {
            logger.error("Không thể tạo QR Code cho sản phẩm ID: {}", savedDetail.getId(), e);
        }

        if (imageFiles != null && imageFiles.length > 0) {
            saveImagesToLocal(savedDetail, Arrays.asList(imageFiles));
        }

        // Gửi thông báo WebSocket đến topic của sản phẩm
        UUID sanPhamId = pd.getSanPham().getId();
        messagingTemplate.convertAndSend("/topic/product/" + sanPhamId, Map.of("action", "refresh"));
    }

    @Transactional
    public ChiTietSanPham updateStockAndStatus(UUID productDetailId, int quantityChange) {
        ChiTietSanPham productDetail = chiTietSanPhamRepo.findById(productDetailId)
                .orElseThrow(() -> new RuntimeException("Chi tiết sản phẩm không tồn tại ID: " + productDetailId));

        int newStock = productDetail.getSoLuongTonKho() + quantityChange;
        if (newStock < 0) {
            throw new RuntimeException("Số lượng tồn kho không đủ cho sản phẩm: " + productDetail.getSanPham().getTenSanPham());
        }

        productDetail.setSoLuongTonKho(newStock);
        productDetail.setTrangThai(newStock > 0);

        ChiTietSanPham saved = chiTietSanPhamRepo.save(productDetail);

        // Gửi thông báo WebSocket đến topic của sản phẩm
        UUID sanPhamId = productDetail.getSanPham().getId();
        messagingTemplate.convertAndSend("/topic/product/" + sanPhamId, Map.of("action", "refresh"));

        return saved;
    }

    public void updateStock(UUID productDetailId, int quantity) {
        ChiTietSanPham chiTiet = chiTietSanPhamRepo.findById(productDetailId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        int newStock = chiTiet.getSoLuongTonKho() - quantity;
        if (newStock < 0) {
            throw new IllegalStateException("Số lượng tồn kho không đủ cho sản phẩm: " + chiTiet.getSanPham().getTenSanPham());
        }
        chiTiet.setSoLuongTonKho(newStock);
        chiTietSanPhamRepo.save(chiTiet);

        // Gửi thông báo WebSocket đến topic của sản phẩm
        UUID sanPhamId = chiTiet.getSanPham().getId();
        messagingTemplate.convertAndSend("/topic/product/" + sanPhamId, Map.of("action", "refresh"));
    }

    public String layAnhDauTien(ChiTietSanPham chiTiet) {
        if (chiTiet.getHinhAnhSanPhams() != null && !chiTiet.getHinhAnhSanPhams().isEmpty()) {
            return chiTiet.getHinhAnhSanPhams().get(0).getUrlHinhAnh();
        }
        return "https://via.placeholder.com/50";
    }

}