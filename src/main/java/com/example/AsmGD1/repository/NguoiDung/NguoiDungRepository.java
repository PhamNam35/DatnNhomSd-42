    package com.example.AsmGD1.repository.NguoiDung;

    import com.example.AsmGD1.entity.NguoiDung;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.Query;
    import org.springframework.data.repository.query.Param;

    import java.util.List;
    import java.util.Optional;
    import java.util.UUID;

    public interface NguoiDungRepository extends JpaRepository<NguoiDung, UUID> {
        @Query("SELECT u FROM NguoiDung u WHERE u.vaiTro = :vaiTro AND (u.hoTen LIKE %:keyword% OR u.email LIKE %:keyword% OR u.soDienThoai LIKE %:keyword%)")
        Page<NguoiDung> findByVaiTroAndKeyword(String vaiTro, String keyword, Pageable pageable);

        List<NguoiDung> findByVaiTro(String vaiTro);
        @Query("SELECT u FROM NguoiDung u WHERE LOWER(u.vaiTro) = LOWER(:vaiTro) AND u.trangThai = :trangThai")
        List<NguoiDung> findByVaiTroAndTrangThai(@Param("vaiTro") String vaiTro, @Param("trangThai") boolean trangThai);


        @Query("SELECT u FROM NguoiDung u WHERE u.vaiTro = 'CUSTOMER' AND u.trangThai = true AND (" +
                "LOWER(u.hoTen) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                "LOWER(u.soDienThoai) LIKE LOWER(CONCAT('%', :keyword, '%')) )")
        List<NguoiDung> searchByKeywordNoPaging(@Param("keyword") String keyword);

        @Query("SELECT u FROM NguoiDung u WHERE u.vaiTro = 'CUSTOMER' AND u.trangThai = true AND (" +
                "LOWER(u.hoTen) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                "LOWER(u.soDienThoai) LIKE LOWER(CONCAT('%', :keyword, '%')) )")
        Page<NguoiDung> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

        Page<NguoiDung> findByVaiTroAndTrangThaiTrue(String vaiTro, Pageable pageable);

        Page<NguoiDung> findByHoTenContainingIgnoreCaseAndVaiTroAndTrangThaiTrue(String hoTen, String vaiTro, Pageable pageable);

        boolean existsBySoDienThoai(String soDienThoai);

        Optional<NguoiDung> findBySoDienThoai(String soDienThoai);

        @Query("SELECT u FROM NguoiDung u WHERE u.tenDangNhap = :tenDangNhap AND u.trangThai = true")
        Optional<NguoiDung> findByTenDangNhap(@Param("tenDangNhap") String tenDangNhap);

        Optional<NguoiDung> findByEmail(String email);

        boolean existsByEmail(String email);

        @Query("SELECT u FROM NguoiDung u WHERE u.vaiTro IN ('employee', 'admin') AND (u.hoTen LIKE %:keyword% OR u.email LIKE %:keyword% OR u.soDienThoai LIKE %:keyword%)")
        Page<NguoiDung> findByVaiTroNotCustomer(@Param("keyword") String keyword, Pageable pageable);
        List<NguoiDung> findByFaceDescriptorIsNotNullAndFaceRegisteredTrue();

    }

