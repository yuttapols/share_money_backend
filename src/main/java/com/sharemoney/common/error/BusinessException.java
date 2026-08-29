package com.sharemoney.common.error;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String displayMessage;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDesc());
        this.errorCode = errorCode;
        this.displayMessage = errorCode.getDisplayMessage();
    }

    public BusinessException(ErrorCode errorCode, String displayMessage) {
        super(errorCode.getDesc());
        this.errorCode = errorCode;
        this.displayMessage = displayMessage;
    }
}
