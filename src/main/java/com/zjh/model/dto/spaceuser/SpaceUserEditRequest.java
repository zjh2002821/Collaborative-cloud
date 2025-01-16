package com.zjh.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;
/**
 * @author zjh
 * @version 1.0
 * 编辑空间用户请求模型
 */
@Data
public class SpaceUserEditRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    private static final long serialVersionUID = 1L;
}
