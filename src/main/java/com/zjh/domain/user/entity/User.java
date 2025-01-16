package com.zjh.domain.user.entity;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;

import com.zjh.domain.user.constant.UserConstant;
import com.zjh.infrastructure.exception.ErrorCode;
import com.zjh.infrastructure.exception.ThrowUtils;
import lombok.Data;

/**
 * 用户
 * @TableName user
 */
@TableName(value ="user")
@Data
public class User implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)//切换自动生成id策略，使用长整型，防止爬虫
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin/vip
     */
    private String userRole;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic//使用逻辑删除
    private Integer isDelete;

    /**
     * 会员过期时间
     */
    private Date vipExpireTime;

    /**
     * 会员兑换码
     */
    private String vipCode;

    /**
     * 会员编号
     */
    private Long vipNumber;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 校验用户注册信息
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     */
    public static void validUserRegister(String userAccount, String userPassword, String checkPassword){
        //1.校验
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount,userPassword,checkPassword),"参数为空", ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userAccount.length() < 4,"用户账号过短", ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userPassword.length() < 4,"用户密码过短", ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(!checkPassword.equals(userPassword),"两次密码不一致", ErrorCode.PARAMS_ERROR);
    }

    /**
     * 校验用户登录信息
     * @param userAccount
     * @param userPassword
     */
    public static void validUserLogin(String userAccount, String userPassword){
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount,userPassword),"参数为空", ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userAccount.length() < 4,"用户账号过短", ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userPassword.length() < 4,"用户密码过短", ErrorCode.PARAMS_ERROR);
    }

    /**
     * 是否为管理员
     * @return
     */
    public boolean isAdmin() {
        return UserConstant.ADMIN_ROLE.equals(this.getUserRole());
    }
}