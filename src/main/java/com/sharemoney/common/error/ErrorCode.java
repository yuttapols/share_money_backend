package com.sharemoney.common.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    SUCCESS("0000", "SUCCESS", "Success", HttpStatus.OK),

    VALIDATION_ERROR("ERR_VALIDATION", "Request validation failed", "Please check your input and try again.", HttpStatus.BAD_REQUEST),

    INVALID_CREDENTIALS("ERR_INVALID_CREDENTIALS", "Username or password is incorrect", "Username or password is incorrect.", HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED("ERR_ACCOUNT_DISABLED", "Account is disabled", "This account has been disabled.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_INVALID("ERR_REFRESH_TOKEN_INVALID", "Refresh token is invalid or revoked", "Your session has expired. Please login again.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED("ERR_REFRESH_TOKEN_EXPIRED", "Refresh token has expired", "Your session has expired. Please login again.", HttpStatus.UNAUTHORIZED),
    OLD_PASSWORD_INCORRECT("ERR_OLD_PASSWORD_INCORRECT", "Old password is incorrect", "Old password is incorrect.", HttpStatus.BAD_REQUEST),

    USERNAME_DUPLICATE("ERR_USERNAME_DUPLICATE", "Username already exists", "This username is already taken.", HttpStatus.CONFLICT),
    MENU_KEY_DUPLICATE("ERR_MENU_KEY_DUPLICATE", "Menu key already exists", "This menu key is already in use.", HttpStatus.CONFLICT),

    DEBTOR_NOT_OWNED("ERR_DEBTOR_NOT_OWNED", "Debtor does not belong to this creditor", "You do not have permission to manage this debtor.", HttpStatus.FORBIDDEN),

    USER_NOT_FOUND("ERR_USER_NOT_FOUND", "User not found", "The requested user was not found.", HttpStatus.NOT_FOUND),
    MENU_ITEM_NOT_FOUND("ERR_MENU_ITEM_NOT_FOUND", "Menu item not found", "The requested menu item was not found.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String desc;
    private final String displayMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String desc, String displayMessage, HttpStatus httpStatus) {
        this.code = code;
        this.desc = desc;
        this.displayMessage = displayMessage;
        this.httpStatus = httpStatus;
    }
}
