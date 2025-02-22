package com.zjh.interfaces.assembler;

import cn.hutool.json.JSONUtil;
import com.zjh.domain.picture.entity.Picture;
import com.zjh.interfaces.dto.picture.PictureEditRequest;
import com.zjh.interfaces.dto.picture.PictureUpdateRequest;
import org.springframework.beans.BeanUtils;
/**
 * 图片对象转换
 */
public class PictureAssembler {

    public static Picture toPictureEntity(PictureEditRequest request) {
        Picture picture = new Picture();
        BeanUtils.copyProperties(request, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(request.getTags()));
        return picture;
    }

    public static Picture toPictureEntity(PictureUpdateRequest request) {
        Picture picture = new Picture();
        BeanUtils.copyProperties(request, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(request.getTags()));
        return picture;
    }
}
