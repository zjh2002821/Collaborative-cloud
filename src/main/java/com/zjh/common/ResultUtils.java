package com.zjh.common;

import com.zjh.exception.ErrorCode;

/**
 * @author zjh
 * @version 1.0
 * 封装请求结果
 */
public class ResultUtils {
    /**
     *
     * @param data 数据
     * @return 响应
     * @param <T> 泛型声明
     */
    public static <T> BaseResponse<T> success(T data){
        return new BaseResponse<T>(0,data,"ok");
    }

    /**
     *
     * @param code 自定义错误码
     * @param message 自定义异常信息
     * @return 响应
     * @param <T> 泛型声明
     */
    public static <T> BaseResponse<T> error(int code,String message){
        return new BaseResponse<T>(code,null,message);
    }

    /**
     *
     * @param errorCode 错误码
     * @return 响应
     * @param <T> 泛型声明
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode){
        return new BaseResponse<T>(errorCode.getCode(),null,errorCode.getMessage());
    }

    /**
     *
     * @param errorCode 错误码
     * @param message 自定义异常信息
     * @return 响应
     * @param <T> 泛型声明
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode,String message){
        return new BaseResponse<T>(errorCode.getCode(),null,message);
    }
}
