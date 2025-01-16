package com.zjh.interfaces.dto.picture;

import lombok.Data;
/**
 * @author zjh
 * @version 1.0
 * 批量抓取图片请求模型
 */
@Data
public class PictureUploadByBatchRequest {
  
    /**  
     * 搜索词  
     */  
    private String searchText;  
  
    /**  
     * 抓取数量  
     */  
    private Integer count = 10;

    /**
     * 名称前缀
     */
    private String namePrefix;

}
