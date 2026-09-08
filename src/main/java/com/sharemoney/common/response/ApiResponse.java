package com.sharemoney.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sharemoney.common.error.ErrorCode;

public record ApiResponse<T>(
        String status,
        String errorCode,
        String errorDesc,
        String displayMessage,
        @JsonInclude(JsonInclude.Include.ALWAYS) T data
) {

    private static final String STATUS_SUCCESS = "C";
    private static final String STATUS_ERROR = "E";

    public static <T> ApiResponse<T> success(T data) {
        return success(data, ErrorCode.SUCCESS.getDisplayMessage());
    }

    public static <T> ApiResponse<T> success(T data, String displayMessage) {
        return new ApiResponse<>(STATUS_SUCCESS, ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getDesc(), displayMessage, data);
    }

    public static <T> ApiResponse<T> businessError(ErrorCode errorCode) {
        return businessError(errorCode, errorCode.getDisplayMessage());
    }

    public static <T> ApiResponse<T> businessError(ErrorCode errorCode, String displayMessage) {
        return new ApiResponse<>(STATUS_ERROR, errorCode.getCode(), errorCode.getDesc(), displayMessage, null);
    }

    public static <T> ApiResponse<T> systemError(int httpStatus, String message) {
        return new ApiResponse<>(STATUS_ERROR, String.valueOf(httpStatus), message, message, null);
    }
}
