    package com.example.AsmGD1.service.NguoiDung;

    import com.example.AsmGD1.entity.DiaChiNguoiDung;
    import com.example.AsmGD1.entity.NguoiDung;
    import com.example.AsmGD1.repository.NguoiDung.DiaChiNguoiDungRepository;
    import com.example.AsmGD1.repository.NguoiDung.KHNguoiDungRepository;
    import jakarta.transaction.Transactional;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.security.crypto.password.PasswordEncoder;
    import org.springframework.stereotype.Service;

    import java.util.UUID;

    @Service
    public class DiaChiKhachHangService {

        @Autowired
        private KHNguoiDungService khNguoiDungService;     // dùng để validate + encode khi tạo mới

        @Autowired
        private DiaChiNguoiDungRepository diaChiRepo;

        @Autowired
        private KHNguoiDungRepository nguoiDungRepository; // save lại user sau khi null địa chỉ

        @Autowired
        private PasswordEncoder passwordEncoder;            // (tuỳ chọn) encode khi đổi mật khẩu lúc cập nhật

        /**
         * Lấy địa chỉ mặc định của một khách hàng
         */
        public DiaChiNguoiDung getDefaultAddress(UUID nguoiDungId) {
            return diaChiRepo.findByNguoiDung_IdAndMacDinhTrue(nguoiDungId)
                    .orElse(null);
        }

        @Transactional
        public NguoiDung saveCustomerWithDefaultAddress(NguoiDung customer) {
            // Set role & status chuẩn
            customer.setVaiTro("customer");
            customer.setTrangThai(true);

            // 1) Lưu user (service này đã validate + encode + set thoiGianTao)
            khNguoiDungService.save(customer); // sau call này customer đã có ID

            // 2) Nếu form có địa chỉ -> tạo địa chỉ mặc định
            if (hasAddress(customer)) {
                // Hạ cờ mặc định cũ (nếu có)
                diaChiRepo.removeDefaultFlag(customer.getId());

                DiaChiNguoiDung dc = new DiaChiNguoiDung();
                dc.setNguoiDung(customer);
                dc.setChiTietDiaChi(customer.getChiTietDiaChi());
                dc.setPhuongXa(customer.getPhuongXa());
                dc.setQuanHuyen(customer.getQuanHuyen());
                dc.setTinhThanhPho(customer.getTinhThanhPho());
                dc.setNguoiNhan(customer.getHoTen());
                dc.setSoDienThoaiNguoiNhan(customer.getSoDienThoai());
                dc.setMacDinh(true);
                diaChiRepo.save(dc);

//                // 3) Xoá địa chỉ khỏi bảng nguoi_dung (đảm bảo chỉ lưu 1 nơi)
//                customer.setChiTietDiaChi(null);
//                customer.setPhuongXa(null);
//                customer.setQuanHuyen(null);
//                customer.setTinhThanhPho(null);
//                nguoiDungRepository.save(customer);
            }

            return customer;
        }


        @Transactional
        public NguoiDung updateCustomerAndAppendAddress(NguoiDung customer) {
            // Lấy thông tin khách hàng hiện tại từ cơ sở dữ liệu để giữ nguyên các trường không thay đổi
            NguoiDung existingCustomer = nguoiDungRepository.findById(customer.getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + customer.getId()));

            // Cập nhật các trường không phải địa chỉ từ đối tượng customer vào existingCustomer
            existingCustomer.setHoTen(customer.getHoTen());
            existingCustomer.setTenDangNhap(customer.getTenDangNhap());
            existingCustomer.setEmail(customer.getEmail());
            existingCustomer.setSoDienThoai(customer.getSoDienThoai());
            existingCustomer.setMatKhau(customer.getMatKhau()); // Encode lại nếu cần
            existingCustomer.setNgaySinh(customer.getNgaySinh());
            existingCustomer.setGioiTinh(customer.getGioiTinh());
            // Luôn đặt vai_tro là "CUSTOMER" và bỏ qua giá trị từ form
            existingCustomer.setVaiTro("customer");
            existingCustomer.setTrangThai(customer.getTrangThai());

            // (Tùy chọn) Nếu mật khẩu trong form không rỗng và có vẻ là plaintext -> encode lại
            if (customer.getMatKhau() != null && !customer.getMatKhau().isBlank()) {
                existingCustomer.setMatKhau(passwordEncoder.encode(customer.getMatKhau()));
            }

            // Lưu cập nhật cơ bản của user
            NguoiDung updated = nguoiDungRepository.save(existingCustomer);

            // Nếu form kèm địa chỉ -> thêm địa chỉ mặc định mới
            if (hasAddress(customer)) {
                diaChiRepo.removeDefaultFlag(updated.getId());

                DiaChiNguoiDung dc = new DiaChiNguoiDung();
                dc.setNguoiDung(updated);
                dc.setChiTietDiaChi(customer.getChiTietDiaChi());
                dc.setPhuongXa(customer.getPhuongXa());
                dc.setQuanHuyen(customer.getQuanHuyen());
                dc.setTinhThanhPho(customer.getTinhThanhPho());
                dc.setNguoiNhan(updated.getHoTen());
                dc.setSoDienThoaiNguoiNhan(updated.getSoDienThoai());
                dc.setMacDinh(true);
                diaChiRepo.save(dc);

                // Không xóa địa chỉ khỏi bảng nguoi_dung vì nó đã được chuyển sang dia_chi_nguoi_dung
            }

            return updated;
        }

        // ===== Helpers =====

        private boolean hasAddress(NguoiDung user) {
            return notBlank(user.getChiTietDiaChi())
                    || notBlank(user.getPhuongXa())
                    || notBlank(user.getQuanHuyen())
                    || notBlank(user.getTinhThanhPho());
        }

        private boolean notBlank(String s) {
            return s != null && !s.trim().isEmpty();
        }
    }
