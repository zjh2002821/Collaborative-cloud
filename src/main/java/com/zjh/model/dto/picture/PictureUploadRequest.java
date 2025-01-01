package com.zjh.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
/**
 * @author zjh
 * @version 1.0
 * 图片上传请求模型
 */
@Data
public class PictureUploadRequest implements Serializable {
  
    /**  
     * 图片 id（用于修改）  
     */  
    private Long id;
    /**
     * 文件url地址
     */
    private String fileUrl;
    /**
     * 文件名称
     */
    private String picName;

    private static final long serialVersionUID = 1L;  
}
