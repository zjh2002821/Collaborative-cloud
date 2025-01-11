package com.zjh.model.vo.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
/**
 * @author zjh
 * @version 1.0
 * 空间图片标签分析模块响应类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceTagAnalyzeResponse implements Serializable {

    /**
     * 标签名称
     */
    private String tag;

    /**
     * 使用次数
     */
    private Long count;

    private static final long serialVersionUID = 1L;
}
