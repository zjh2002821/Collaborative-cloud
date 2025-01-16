package com.zjh.infrastructure.common;

import lombok.Data;

import java.io.Serializable;

/**
 * @author zjh
 * @version 1.0
 * 通用的删除请求
 */
@Data
public class DeleteRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;

}
