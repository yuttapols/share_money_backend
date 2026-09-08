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
    ACCOUNT_TEMPORARILY_LOCKED("ERR_ACCOUNT_LOCKED", "Account temporarily locked due to too many failed login attempts", "Too many failed login attempts. Please try again in a few minutes.", HttpStatus.TOO_MANY_REQUESTS),

    USERNAME_DUPLICATE("ERR_USERNAME_DUPLICATE", "Username already exists", "This username is already taken.", HttpStatus.CONFLICT),
    MENU_KEY_DUPLICATE("ERR_MENU_KEY_DUPLICATE", "Menu key already exists", "This menu key is already in use.", HttpStatus.CONFLICT),

    DEBTOR_NOT_OWNED("ERR_DEBTOR_NOT_OWNED", "Debtor does not belong to this creditor", "You do not have permission to manage this debtor.", HttpStatus.FORBIDDEN),

    USER_NOT_FOUND("ERR_USER_NOT_FOUND", "User not found", "The requested user was not found.", HttpStatus.NOT_FOUND),
    MENU_ITEM_NOT_FOUND("ERR_MENU_ITEM_NOT_FOUND", "Menu item not found", "The requested menu item was not found.", HttpStatus.NOT_FOUND),

    DEBT_NOT_FOUND("ERR_DEBT_NOT_FOUND", "Debt not found", "The requested debt was not found.", HttpStatus.NOT_FOUND),
    DEBT_NOT_OWNED("ERR_DEBT_NOT_OWNED", "Debt does not belong to this creditor", "You do not have permission to manage this debt.", HttpStatus.FORBIDDEN),
    INSTALLMENT_NOT_FOUND("ERR_INSTALLMENT_NOT_FOUND", "Installment not found", "The requested installment was not found.", HttpStatus.NOT_FOUND),
    INSTALLMENT_ALREADY_PAID("ERR_INSTALLMENT_ALREADY_PAID", "Installment is already paid", "This installment has already been paid.", HttpStatus.BAD_REQUEST),
    OPEN_RECORD_NOT_FOUND("ERR_OPEN_RECORD_NOT_FOUND", "Open loan record not found", "The requested record was not found.", HttpStatus.NOT_FOUND),
    INVALID_INSTALLMENT_COUNT("ERR_INVALID_INSTALLMENT_COUNT", "Installment count is not an allowed choice", "Please select a valid number of installments.", HttpStatus.BAD_REQUEST),
    INVALID_DEBT_METHOD("ERR_INVALID_DEBT_METHOD", "Action is not valid for this debt method", "This action cannot be performed on this type of debt.", HttpStatus.BAD_REQUEST),

    EMPTY_FILE("ERR_EMPTY_FILE", "Uploaded file is empty", "The uploaded file is empty.", HttpStatus.BAD_REQUEST),
    INVALID_FILE_TYPE("ERR_INVALID_FILE_TYPE", "Uploaded file type is not allowed", "This file type is not allowed.", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE("ERR_FILE_TOO_LARGE", "Uploaded file exceeds the size limit", "The uploaded file is too large.", HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAILED("ERR_FILE_UPLOAD_FAILED", "File upload to storage failed", "File upload failed. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_DELETE_FAILED("ERR_FILE_DELETE_FAILED", "File delete from storage failed", "File delete failed. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR),

    SLIP_NOT_FOUND("ERR_SLIP_NOT_FOUND", "Slip not found", "The requested slip was not found.", HttpStatus.NOT_FOUND),
    SLIP_NOT_OWNED("ERR_SLIP_NOT_OWNED", "Slip does not belong to this user", "You do not have permission to access this slip.", HttpStatus.FORBIDDEN),

    DOCUMENT_NOT_FOUND("ERR_DOCUMENT_NOT_FOUND", "Document not found", "The requested document was not found.", HttpStatus.NOT_FOUND),
    DOCUMENT_NOT_OWNED("ERR_DOCUMENT_NOT_OWNED", "Document does not belong to this creditor", "You do not have permission to access this document.", HttpStatus.FORBIDDEN),

    BANK_ACCOUNT_NOT_FOUND("ERR_BANK_ACCOUNT_NOT_FOUND", "Bank account not found", "The requested bank account was not found.", HttpStatus.NOT_FOUND),
    BANK_ACCOUNT_NOT_OWNED("ERR_BANK_ACCOUNT_NOT_OWNED", "Bank account does not belong to this creditor", "You do not have permission to manage this bank account.", HttpStatus.FORBIDDEN),

    PDF_GENERATION_FAILED("ERR_PDF_GENERATION_FAILED", "PDF generation failed", "Failed to generate the report PDF. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR),

    SETTING_NOT_FOUND("ERR_SETTING_NOT_FOUND", "Setting not found", "This system setting has not been initialized.", HttpStatus.NOT_FOUND);

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
