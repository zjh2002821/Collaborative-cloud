package com.zjh.interfaces.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author zjh
 * @version 1.0
 * 用户注册请求模型
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;
}

