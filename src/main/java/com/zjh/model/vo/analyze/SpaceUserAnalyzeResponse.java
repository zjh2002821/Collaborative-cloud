package com.zjh.model.vo.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
/**
 * @author zjh
 * @version 1.0
 * 空间用户行为分析模块响应类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceUserAnalyzeResponse implements Serializable {

    /**
     * 时间区间
     */
    private String period;

    /**
     * 上传数量
     */
    private Long count;

    private static final long serialVersionUID = 1L;
}
