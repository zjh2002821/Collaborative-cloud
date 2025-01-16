package com.zjh.interfaces.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author zjh
 * @version 1.0
 * 用户登录请求模型
 */
@Data
public class UserLoginRequest implements Serializable {
    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

}
