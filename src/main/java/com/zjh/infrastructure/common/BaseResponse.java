package com.zjh.infrastructure.common;

import com.zjh.infrastructure.exception.ErrorCode;
import lombok.Data;

/**
 * @author zjh
 * @version 1.0
 * 封装数据响应格式
 */
@Data
public class BaseResponse<T> {
    private int code;
    private T data;
    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data){
        this(code,data,"");
    }

    public BaseResponse(ErrorCode errorCode){
        this(errorCode.getCode(),null,errorCode.getMessage());
    }
}
