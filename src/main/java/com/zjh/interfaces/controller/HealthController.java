package com.zjh.interfaces.controller;

import com.zjh.infrastructure.common.BaseResponse;
import com.zjh.infrastructure.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zjh
 * @version 1.0
 * 健康检查
 */
@RestController
@RequestMapping("/")
public class HealthController {
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");
    }
}
