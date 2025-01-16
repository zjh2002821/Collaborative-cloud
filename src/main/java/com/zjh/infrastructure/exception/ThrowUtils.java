package com.zjh.infrastructure.exception;

/**
 * @author zjh
 * @version 1.0
 */
public class ThrowUtils {
    /**
     *
     * @param condition 判断条件
     * @param runtimeException 运行时异常
     */
    public static void throwIf(boolean condition,RuntimeException runtimeException){
        if(condition){
            throw runtimeException;
        }
    }

    /**
     *
     * @param condition 条件
     * @param message 自定义异常信息
     * @param errorCode 错误码
     */
    public static void throwIf(boolean condition,String message,ErrorCode errorCode){
        throwIf(condition,new BusinessException( errorCode,message));
    }

    /**
     *
     * @param condition 条件
     * @param errorCode 错误码
     */
    public static void throwIf(boolean condition,ErrorCode errorCode){
        throwIf(condition,new BusinessException(errorCode));
    }
}
