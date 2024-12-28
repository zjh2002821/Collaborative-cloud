package com.zjh.model.enums;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

import java.util.HashMap;

/**
 * @author zjh
 * @version 1.0
 */
@Getter
public enum UserRoleEnum {

    USER("用户","user"),
    VIP_USER("VIP用户","vipUser"),
    ADMIN("管理员","admin");

    private final String text;
    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     *
     * @param value 用户角色参数
     * @return UserRoleEnum 返回值
     */
    public static UserRoleEnum getEnumByValue(String value){
        //判断是否为空
        if (ObjectUtil.isEmpty(value)){
            return null;
        }
        //根据条件便取出对应枚举值
        HashMap<String,UserRoleEnum> userRoleEnumHashMap = new HashMap<>();
        userRoleEnumHashMap.put("user",USER);
        userRoleEnumHashMap.put("vipUser",VIP_USER);
        userRoleEnumHashMap.put("admin",ADMIN);

        return userRoleEnumHashMap.get(value);
    }
}
