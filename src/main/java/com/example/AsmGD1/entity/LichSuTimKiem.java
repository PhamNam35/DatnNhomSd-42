package com.example.AsmGD1.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lich_su_tim_kiem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LichSuTimKiem {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tu_khoa")
    private String tuKhoa;

    @ManyToOne
    @JoinColumn(name = "id_nguoi_dung")
    private NguoiDung nguoiDung;

    @Column(name = "thoi_gian_tim_kiem")
    private LocalDateTime thoiGianTimKiem;

    @PrePersist
    public void prePersist() {
        this.thoiGianTimKiem = LocalDateTime.now();
    }
}