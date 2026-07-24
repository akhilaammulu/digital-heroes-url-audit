package com.digitalheroes.urlaudit.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        Instant timestamp,
        String requestId) {

    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<>(true, data, null, Instant.now(), requestId);
    }

    public static ApiResponse<Void> failure(String code, String message, String requestId) {
        return new ApiResponse<>(false, null, new ApiError(code, message), Instant.now(), requestId);
    }
}
