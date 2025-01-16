package com.zjh.interfaces.dto.analyze;

import lombok.Data;

import java.io.Serializable;
/**
 * @author zjh
 * @version 1.0
 * 空间分析请求模型( spaceId有值：个人用户查看自己的空间使用率分析，或者管理员查看用户的空间
 *                queryPublic为true：管理员查看公共空间占用大小分析
 *                queryAll为true：管理员查看云存储服务器空间占用大小分析
 *                  )
 */
@Data
public class SpaceAnalyzeRequest implements Serializable {

    /**
     * 空间 ID
     */
    private Long spaceId;

    /**
     * 是否查询公共图库
     */
    private boolean queryPublic;

    /**
     * 全空间分析
     */
    private boolean queryAll;

    private static final long serialVersionUID = 1L;
}
