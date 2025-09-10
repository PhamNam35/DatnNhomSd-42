package com.example.AsmGD1.service.GiamGia;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.PhieuGiamGia;
import com.example.AsmGD1.entity.PhieuGiamGiaCuaNguoiDung;
import com.example.AsmGD1.repository.GiamGia.PhieuGiamGiaCuaNguoiDungRepository;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhieuGiamGiaCuaNguoiDungService {

    private static final Logger logger = LoggerFactory.getLogger(PhieuGiamGiaCuaNguoiDungService.class);

    private final NguoiDungRepository nguoiDungRepository;
    private final PhieuGiamGiaCuaNguoiDungRepository phieuGiamGiaCuaNguoiDungRepository;
    @Autowired
    private PhieuGiamGiaService phieuGiamGiaService;

    public List<NguoiDung> layTatCaKhachHang() {
        return nguoiDungRepository.findByVaiTro("CUSTOMER").stream()
                .filter(Objects::nonNull)
                .filter(kh -> kh.getHoTen() != null)
                .toList();
    }

    public Page<NguoiDung> layTatCaKhachHangPhanTrang(Pageable pageable) {
        return nguoiDungRepository.findByVaiTroAndTrangThaiTrue("CUSTOMER", pageable);
    }

    public List<NguoiDung> layNguoiDungTheoIds(List<UUID> ids) {
        return nguoiDungRepository.findAllById(ids);
    }

    public void ganPhieuChoNguoiDung(NguoiDung nguoiDung, PhieuGiamGia phieuGiamGia) {
        boolean daTonTai = phieuGiamGiaCuaNguoiDungRepository.findAll().stream()
                .anyMatch(item ->
                        item.getNguoiDung().getId().equals(nguoiDung.getId()) &&
                                item.getPhieuGiamGia().getId().equals(phieuGiamGia.getId())
                );

        if (!daTonTai) {
            if ("ca_nhan".equalsIgnoreCase(phieuGiamGia.getKieuPhieu())) {
                phieuGiamGia.setSoLuong(1);
                phieuGiamGia.setGioiHanSuDung(1);
                phieuGiamGiaService.luu(phieuGiamGia);
            }

            PhieuGiamGiaCuaNguoiDung entity = new PhieuGiamGiaCuaNguoiDung();
            entity.setNguoiDung(nguoiDung);
            entity.setPhieuGiamGia(phieuGiamGia);

            if ("ca_nhan".equalsIgnoreCase(phieuGiamGia.getKieuPhieu())) {
                entity.setSoLuotConLai(1);
                entity.setSoLuotDuocSuDung(1);
            }

            phieuGiamGiaCuaNguoiDungRepository.save(entity);
        }
    }

    public List<UUID> layIdKhachHangDaGanPhieu(UUID phieuId) {
        return phieuGiamGiaCuaNguoiDungRepository.findByPhieuGiamGia_Id(phieuId).stream()
                .map(p -> p.getNguoiDung().getId())
                .distinct()
                .collect(Collectors.toList());
    }

    public List<NguoiDung> layNguoiDungTheoPhieu(UUID phieuId) {
        return phieuGiamGiaCuaNguoiDungRepository.findByPhieuGiamGia_Id(phieuId).stream()
                .map(PhieuGiamGiaCuaNguoiDung::getNguoiDung)
                .collect(Collectors.toList());
    }

    @Transactional
    public void xoaTatCaGanKetTheoPhieu(UUID phieuId) {
        List<PhieuGiamGiaCuaNguoiDung> list = phieuGiamGiaCuaNguoiDungRepository.findByPhieuGiamGia_Id(phieuId);
        phieuGiamGiaCuaNguoiDungRepository.deleteAll(list);
    }

    public List<NguoiDung> timKiemKhachHang(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return layTatCaKhachHang();
        }
        return nguoiDungRepository.searchByKeywordNoPaging(keyword.trim());
    }

    public Page<NguoiDung> timKiemKhachHangPhanTrang(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return layTatCaKhachHangPhanTrang(pageable);
        }
        return nguoiDungRepository.findByHoTenContainingIgnoreCaseAndVaiTroAndTrangThaiTrue(keyword.trim(), "CUSTOMER", pageable);
    }

    public List<PhieuGiamGiaCuaNguoiDung> findByNguoiDungId(UUID nguoiDungId) {
        return phieuGiamGiaCuaNguoiDungRepository.findByNguoiDungId(nguoiDungId);
    }

    @Transactional
    public boolean suDungPhieu(UUID nguoiDungId, UUID phieuId) {
        var optional = phieuGiamGiaCuaNguoiDungRepository
                .findByPhieuGiamGia_IdAndNguoiDung_Id(phieuId, nguoiDungId);

        if (optional.isEmpty()) {
            logger.warn("Không tìm thấy phiếu giảm giá cá nhân cho người dùng {} và phiếu {}", nguoiDungId, phieuId);
            return false;
        }

        PhieuGiamGiaCuaNguoiDung p = optional.get();
        PhieuGiamGia phieu = p.getPhieuGiamGia();

        boolean conLuotCaNhan = p.getSoLuotConLai() != null && p.getSoLuotConLai() > 0;
        boolean conSoLuongTong = phieu != null && phieu.getSoLuong() != null && phieu.getSoLuong() > 0;

        if (conLuotCaNhan && conSoLuongTong) {
            p.setSoLuotConLai(p.getSoLuotConLai() - 1);
            phieuGiamGiaCuaNguoiDungRepository.save(p);

            phieu.setSoLuong(phieu.getSoLuong() - 1);
            phieuGiamGiaService.luu(phieu);
            logger.info("Áp dụng phiếu cá nhân thành công: userId={}, phieuId={}, soLuong moi={}, soLuotConLai moi={}",
                    nguoiDungId, phieuId, phieu.getSoLuong(), p.getSoLuotConLai());
            return true;
        }

        logger.warn("Phiếu cá nhân không khả dụng: soLuotConLai={}, soLuong={}", p.getSoLuotConLai(), phieu.getSoLuong());
        return false;
    }

    public boolean kiemTraPhieuCaNhan(UUID nguoiDungId, UUID phieuId) {
        var optional = phieuGiamGiaCuaNguoiDungRepository
                .findByPhieuGiamGia_IdAndNguoiDung_Id(phieuId, nguoiDungId);

        if (optional.isEmpty()) {
            logger.warn("Không tìm thấy phiếu giảm giá cá nhân cho người dùng {} và phiếu {}", nguoiDungId, phieuId);
            return false;
        }

        PhieuGiamGiaCuaNguoiDung p = optional.get();
        PhieuGiamGia phieu = p.getPhieuGiamGia();

        boolean conLuotCaNhan = p.getSoLuotConLai() != null && p.getSoLuotConLai() > 0;
        boolean conSoLuongTong = phieu != null && phieu.getSoLuong() != null && phieu.getSoLuong() > 0;
        boolean conHan = (phieu.getNgayBatDau() == null || !LocalDateTime.now().isBefore(phieu.getNgayBatDau()))
                && (phieu.getNgayKetThuc() == null || !LocalDateTime.now().isAfter(phieu.getNgayKetThuc()));

        if (!conLuotCaNhan || !conSoLuongTong || !conHan) {
            logger.warn("Phiếu cá nhân không hợp lệ: soLuotConLai={}, soLuong={}, conHan={}",
                    p.getSoLuotConLai(), phieu.getSoLuong(), conHan);
            return false;
        }

        logger.info("Phiếu cá nhân hợp lệ: userId={}, phieuId={}", nguoiDungId, phieuId);
        return true;
    }

    public List<PhieuGiamGia> layPhieuCaNhanConHan(UUID nguoiDungId) {
        List<PhieuGiamGiaCuaNguoiDung> list = phieuGiamGiaCuaNguoiDungRepository.findByNguoiDungId(nguoiDungId);
        LocalDateTime now = LocalDateTime.now();
        List<PhieuGiamGia> result = new ArrayList<>();

        for (PhieuGiamGiaCuaNguoiDung item : list) {
            PhieuGiamGia phieu = item.getPhieuGiamGia();
            if (phieu == null) continue;

            boolean conLuot = item.getSoLuotConLai() != null && item.getSoLuotConLai() > 0;
            boolean conSoLuong = phieu.getSoLuong() != null && phieu.getSoLuong() > 0;
            boolean conHan = (phieu.getNgayBatDau() == null || !now.isBefore(phieu.getNgayBatDau()))
                    && (phieu.getNgayKetThuc() == null || !now.isAfter(phieu.getNgayKetThuc()));

            if (conLuot && conSoLuong && conHan) {
                result.add(phieu);
            }
        }

        return result;
    }
}