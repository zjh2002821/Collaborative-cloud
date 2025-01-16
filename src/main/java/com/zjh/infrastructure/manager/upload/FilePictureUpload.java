package com.zjh.infrastructure.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.zjh.infrastructure.exception.ErrorCode;
import com.zjh.infrastructure.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Service
public class FilePictureUpload extends PictureUploadTemplate {  
  
    @Override  
    protected void validPicture(Object inputSource) {  
        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowUtils.throwIf(multipartFile == null, "文件不能为空", ErrorCode.PARAMS_ERROR);
        // 1. 校验文件大小  
        long fileSize = multipartFile.getSize();  
        final long ONE_M = 1024 * 1024L;  
        ThrowUtils.throwIf(fileSize > 2 * ONE_M, "文件大小不能超过 2M", ErrorCode.PARAMS_ERROR);
        // 2. 校验文件后缀  
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        // 允许上传的文件后缀  
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "jpg", "png", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), "文件类型错误", ErrorCode.PARAMS_ERROR);
    }  
  
    @Override  
    protected String getOriginFilename(Object inputSource) {  
        MultipartFile multipartFile = (MultipartFile) inputSource;  
        return multipartFile.getOriginalFilename();  
    }  
  
    @Override  
    protected void processFile(Object inputSource, File file) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;  
        multipartFile.transferTo(file);  
    }  
}
