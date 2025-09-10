package com.example.AsmGD1.dto.BanHang;

import java.util.UUID;

public class CartAddDto {
    private UUID id;
    private int quantity;

    // Getters và Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
