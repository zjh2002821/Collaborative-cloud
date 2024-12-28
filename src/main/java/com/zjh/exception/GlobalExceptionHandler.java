package com.zjh.exception;

import com.zjh.common.BaseResponse;
import com.zjh.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author zjh
 * @version 1.0
 * 全局异常处理
 */
@RestControllerAdvice//定义请求环绕切面
@Slf4j
public class GlobalExceptionHandler {
    //处理业务异常返回
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException businessException){
        log.error("BusinessException"+businessException);
        return ResultUtils.error(businessException.getCode(),businessException.getMessage());
    }

    //处理运行时异常返回
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException runtimeException){
        log.error("RuntimeException"+runtimeException);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR,runtimeException.getMessage());
    }
}
