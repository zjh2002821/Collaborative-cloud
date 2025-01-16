package com.zjh.interfaces.dto.space;

import lombok.AllArgsConstructor;
import lombok.Data;
/**
 * @author zjh
 * @version 1.0
 * 空间级别模型
 */

@Data
@AllArgsConstructor
public class SpaceLevel {

    private int value;

    private String text;

    private long maxCount;

    private long maxSize;
}
