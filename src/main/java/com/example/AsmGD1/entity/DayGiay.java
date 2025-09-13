package com.example.AsmGD1.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Entity
@Table(name = "day_giay")
@Data
public class DayGiay {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "UNIQUEIDENTIFIER")
    private UUID id;

    @Column(name = "ten_day_giay", nullable = false, length = 100)
    private String tenDayGiay;
}