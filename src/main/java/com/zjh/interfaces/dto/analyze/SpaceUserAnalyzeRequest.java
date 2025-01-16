package com.zjh.interfaces.dto.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * @author zjh
 * @version 1.0
 * 空间用户行为分析请求模型，继承SpaceAnalyzeRequest类
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceUserAnalyzeRequest extends SpaceAnalyzeRequest {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 时间维度：day / week / month
     */
    private String timeDimension;
}
