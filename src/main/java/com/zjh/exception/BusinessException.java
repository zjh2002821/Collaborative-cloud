package com.zjh.exception;

import lombok.Getter;

/**
 * @author zjh
 * @version 1.0
 * 封装业务异常
 */
@Getter
public class BusinessException extends RuntimeException{
    /**
     * 错误码
     */
    private final int code;

    /**
     *
     * @param code 用户自己定义了一个新的错误码
     * @param message 用户自己定义了一个新的异常信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     *
     * @param errorCode 用户传入了错误码
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     *
     * @param errorCode 用户传入错误码
     * @param message 用户自定义异常信息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
