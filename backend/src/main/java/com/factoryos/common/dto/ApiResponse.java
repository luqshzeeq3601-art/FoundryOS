package com.factoryos.common.dto;

import java.time.Instant;

public class ApiResponse<T> {
    private T data;
    private String message;
    private Instant timestamp;

    public ApiResponse() {
        this.timestamp = Instant.now();
    }

    public ApiResponse(T data, String message) {
        this.data = data;
        this.message = message;
        this.timestamp = Instant.now();
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(data, message);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, "Operation completed");
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return new ApiResponse<>(data, message);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(data, "Resource created successfully");
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
