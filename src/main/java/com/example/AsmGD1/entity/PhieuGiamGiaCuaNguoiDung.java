package com.example.AsmGD1.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "phieu_giam_gia_cua_nguoi_dung")
@Data
public class PhieuGiamGiaCuaNguoiDung {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "id_nguoi_dung")
    private NguoiDung nguoiDung;

    @ManyToOne
    @JoinColumn(name = "id_phieu_giam_gia")
    private PhieuGiamGia phieuGiamGia;

    @Column(name = "so_luot_duoc_su_dung")
    private Integer soLuotDuocSuDung;

    @Column(name = "so_luot_con_lai")
    private Integer soLuotConLai;

    @Column(name = "da_gui_mail")
    private Boolean daGuiMail = false;  // mặc định là false
}
