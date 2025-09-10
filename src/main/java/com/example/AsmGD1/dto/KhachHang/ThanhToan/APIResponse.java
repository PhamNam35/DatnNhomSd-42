package com.example.AsmGD1.dto.KhachHang.ThanhToan;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class APIResponse<T> {
    private String message;
    private T data;

    public APIResponse(String message) {
        this.message = message;
    }

    public APIResponse(String message, T data) {
        this.message = message;
        this.data = data;
    }
}