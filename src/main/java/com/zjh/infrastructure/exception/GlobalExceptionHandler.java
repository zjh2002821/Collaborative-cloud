package com.zjh.infrastructure.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.zjh.infrastructure.common.BaseResponse;
import com.zjh.infrastructure.common.ResultUtils;
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
    @ExceptionHandler(NotLoginException.class)
    public BaseResponse<?> notLoginException(NotLoginException e) {
        log.error("NotLoginException", e);
        return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR, e.getMessage());
    }

    @ExceptionHandler(NotPermissionException.class)
    public BaseResponse<?> notPermissionExceptionHandler(NotPermissionException e) {
        log.error("NotPermissionException", e);
        return ResultUtils.error(ErrorCode.NO_AUTH_ERROR, e.getMessage());
    }

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
