package com.zjh.api.imagesearch.model;

import lombok.Data;
/**
 * 以图识图抓取图片url模型
 * @TableName picture
 */
@Data
public class ImageSearchResult {  
  
    /**  
     * 缩略图地址  
     */  
    private String thumbUrl;  
  
    /**  
     * 来源地址  
     */  
    private String fromUrl;  
}
