package com.zjh.interfaces.vo.picture;

import lombok.Data;

import java.util.List;

/**
 * @author zjh
 * @version 1.0
 * 图片标签，分类列表视图
 */
@Data
public class PictureTagCategory {
    /**
     * 标签列表
     */
    private List<String> tagList;
    /**
     * 分类列表
     */
    private List<String> categoryList;
}
