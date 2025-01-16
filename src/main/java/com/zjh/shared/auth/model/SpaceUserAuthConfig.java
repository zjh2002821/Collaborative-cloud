package com.zjh.shared.auth.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 权限配置类（配置哪个角色具有哪些权限）
 */
@Data
public class SpaceUserAuthConfig implements Serializable {

    /**
     * 权限列表
     */
    private List<SpaceUserPermission> permissions;

    /**
     * 角色列表
     */
    private List<SpaceUserRole> roles;

    private static final long serialVersionUID = 1L;
}
