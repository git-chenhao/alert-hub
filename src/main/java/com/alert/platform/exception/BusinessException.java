package com.alert.platform.exception;

import lombok.Getter;

/**
 * 业务异常
 *
 * @author Alert Platform Team
 * @version 1.0.0
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = -1;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = -1;
    }
}
