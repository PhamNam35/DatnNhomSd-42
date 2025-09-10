package com.example.AsmGD1.controller.GiaoHangNhanh;

public class APIResponseGHN {
    private int code;
    private String message;
    private Object data;

    public APIResponseGHN(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // Getters và setters
    public int getCode() { return code; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
}
