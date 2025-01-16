package com.zjh.interfaces.dto.picture;

import lombok.Data;

import java.io.Serializable;
/**
 * @author zjh
 * @version 1.0
 * 以图识图功能请求模型
 */
@Data
public class SearchPictureByPictureRequest implements Serializable {
  
    /**  
     * 图片 id  
     */  
    private Long pictureId;  
  
    private static final long serialVersionUID = 1L;  
}
