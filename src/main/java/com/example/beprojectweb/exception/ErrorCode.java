package com.example.beprojectweb.exception;

public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error"),
    USER_EXISTED(1001, "User existed"),
    INVALID_KEY(1000, "Invalid message Key"),
    USERNAME_INVALID(1002, "Username must be at least 3 characters"),
    PASSWORD_INVALID(1003, "Password must be at least 3 characters"),
    USER_NOT_EXISTS(1004, "User not existed"),
    UNATHENTICATIED(1005, "UNAUTHENTICATED"),
    CATEGORY_EXISTED(1006, "CATEGORY EXISTED"),
    EMAIL_INVALID(1007, "Email must be a valid Gmail address"),
    USER_NOT_ENABLED(1008, "Account is not activated. Please check your email"),
    INVALID_FILE_TYPE(1009, "Invalid File Type"),
    INTERNAL_SERVER_ERROR(1010, "Internal Server Error"),
    ;

    private int code;
    private String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
