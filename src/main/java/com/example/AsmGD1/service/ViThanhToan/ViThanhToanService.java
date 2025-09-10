package com.example.AsmGD1.service.ViThanhToan;

import com.example.AsmGD1.controller.WebKhachHang.KHThanhToanController;
import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.BanHang.DonHangRepository;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.repository.ViThanhToan.LichSuGiaoDichViRepository;
import com.example.AsmGD1.repository.BanHang.PhuongThucThanhToanRepository;
import com.example.AsmGD1.repository.ViThanhToan.ViThanhToanRepository;
import com.example.AsmGD1.repository.ViThanhToan.YeuCauRutTienRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ViThanhToanService {

    private static final Logger logger = LoggerFactory.getLogger(ViThanhToanService.class);

    @Autowired
    private ViThanhToanRepository viThanhToanRepo;

    @Autowired
    private LichSuGiaoDichViRepository lichSuRepo;

    @Autowired
    private DonHangRepository donHangRepo;

    @Autowired
    private PhuongThucThanhToanRepository phuongThucRepo;

    @Autowired
    private YeuCauRutTienRepository rutTienRepo;
    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    public BigDecimal tongTienDangCho(UUID idVi) {
        BigDecimal tong = rutTienRepo.tongTienDangCho(idVi);
        return tong != null ? tong : BigDecimal.ZERO;
    }

    public ViThanhToan findByUser(UUID idNguoiDung) {
        NguoiDung nguoiDung = nguoiDungRepository.findById(idNguoiDung)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        return viThanhToanRepo.findByNguoiDung(nguoiDung).orElse(null);
    }


    @Transactional
    public void napTien(UUID idNguoiDung, BigDecimal soTien) {
        if (soTien == null || soTien.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0");
        }

        // Tìm ví
        NguoiDung nguoiDung = nguoiDungRepository.findById(idNguoiDung)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        ViThanhToan vi = viThanhToanRepo.findByNguoiDung(nguoiDung)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví của người dùng"));


        vi.setSoDu(vi.getSoDu().add(soTien));
        vi.setThoiGianCapNhat(LocalDateTime.now());
        viThanhToanRepo.save(vi);

        LichSuGiaoDichVi lichSu = new LichSuGiaoDichVi();
        lichSu.setIdViThanhToan(vi.getId());
        lichSu.setLoaiGiaoDich("Nạp tiền");
        lichSu.setSoTien(soTien);
        lichSu.setMoTa("Nạp tiền vào ví");
        lichSu.setCreatedAt(LocalDateTime.now()); // nếu DB cần
        lichSu.setThoiGianGiaoDich(LocalDateTime.now()); // ✅ Đây là dòng bạn thiếu

        lichSuRepo.save(lichSu);
    }

    @Transactional
    public boolean truTienTrongVi(UUID idNguoiDung, BigDecimal soTien, DonHang donHang) {
        // Tìm ví
        NguoiDung nguoiDung = nguoiDungRepository.findById(idNguoiDung)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        ViThanhToan vi = viThanhToanRepo.findByNguoiDung(nguoiDung)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví của người dùng"));


        if (vi.getSoDu().compareTo(soTien) < 0) {
            return false; //Không đủ tiền
        }

        // Trừ tiền
        vi.setSoDu(vi.getSoDu().subtract(soTien));
        vi.setThoiGianCapNhat(LocalDateTime.now());
        viThanhToanRepo.save(vi);

        // Ghi lịch sử giao dịch ví
        LichSuGiaoDichVi lichSu = new LichSuGiaoDichVi();
        lichSu.setIdViThanhToan(vi.getId());
        lichSu.setSoTien(soTien);
        lichSu.setLoaiGiaoDich("Thanh toán");
        lichSu.setMoTa("Thanh toán đơn hàng: " + donHang.getMaDonHang());
        lichSu.setIdDonHang(donHang.getId());
        lichSu.setThoiGianGiaoDich(LocalDateTime.now());
        lichSu.setCreatedAt(LocalDateTime.now());

        lichSuRepo.save(lichSu);
        return true;
    }


    public ViThanhToan taoViMoi(UUID idNguoiDung) {
        // B1: Lấy đối tượng NguoiDung từ id
        NguoiDung nguoiDung = nguoiDungRepository.findById(idNguoiDung)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        // B2: Tạo ví mới và gán người dùng
        ViThanhToan vi = new ViThanhToan();
        vi.setNguoiDung(nguoiDung); // ✅ Sử dụng entity thay vì UUID
        vi.setSoDu(BigDecimal.ZERO);
        vi.setThoiGianTao(LocalDateTime.now());
        vi.setTrangThai(true);

        return viThanhToanRepo.save(vi);
    }




    @Transactional(rollbackFor = Exception.class)
    public boolean thanhToanBangVi(UUID idNguoiDung, UUID idDonHang, BigDecimal tongTien) {
        try {
            // Tìm ví
            NguoiDung nguoiDung = nguoiDungRepository.findById(idNguoiDung)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            ViThanhToan vi = viThanhToanRepo.findByNguoiDung(nguoiDung)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ví của người dùng"));


            if (tongTien == null || tongTien.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Số tiền thanh toán không hợp lệ: " + tongTien);
            }

            if (vi.getSoDu().compareTo(tongTien) < 0) {
                logger.warn("Insufficient balance: userId={}, orderId={}, balance={}, required={}",
                        idNguoiDung, idDonHang, vi.getSoDu(), tongTien);
                return false;
            }

            logger.info("Ví trước khi trừ: {}", vi.getSoDu());
            logger.info("Số tiền cần trừ: {}", tongTien);
            logger.info("Ví sau khi trừ: {}", vi.getSoDu().subtract(tongTien));

            // Deduct amount
            vi.setSoDu(vi.getSoDu().subtract(tongTien));
            vi.setThoiGianCapNhat(LocalDateTime.now());
            viThanhToanRepo.save(vi);


            // Log transaction
            LichSuGiaoDichVi ls = new LichSuGiaoDichVi();
            ls.setCreatedAt(LocalDateTime.now());
            ls.setIdViThanhToan(vi.getId());
            ls.setLoaiGiaoDich("Thanh toán");
            ls.setSoTien(tongTien);
            ls.setIdDonHang(idDonHang);
            ls.setMoTa("Thanh toán đơn hàng: DH" + idDonHang);
            ls.setThoiGianGiaoDich(LocalDateTime.now());
            lichSuRepo.save(ls);

            // Update order payment status
            DonHang dh = donHangRepo.findById(idDonHang)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + idDonHang));
            dh.setTrangThaiThanhToan(true);
            dh.setThoiGianThanhToan(LocalDateTime.now());
            donHangRepo.save(dh);

            logger.info("Wallet payment successful: userId={}, orderId={}, amount={}", idNguoiDung, idDonHang, tongTien);
            return true;
        } catch (Exception e) {
            logger.error("Wallet payment failed: userId={}, orderId={}, error={}", idNguoiDung, idDonHang, e.getMessage());
            throw new RuntimeException("Lỗi thanh toán ví: " + e.getMessage());
        }
    }
}