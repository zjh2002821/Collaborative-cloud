package com.zjh.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.zjh.exception.BusinessException;
import com.zjh.exception.ErrorCode;
import com.zjh.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Service
public class UrlPictureUpload extends PictureUploadTemplate {  
    @Override  
    protected void validPicture(Object inputSource) {  
        String fileUrl = (String) inputSource;  
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), "文件地址不能为空", ErrorCode.PARAMS_ERROR);
        // ... 跟之前的校验逻辑保持一致
        //1.使用java自带的方法，验证url是否合法
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"文件地址格式不合法！");
        }
        //2.验证url协议，必须是http或者是https
        if (!(fileUrl.startsWith("http://") || fileUrl.startsWith("https://"))){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"仅支持 HTTP 或 HTTPS 协议的文件地址！");
        }
        //3.发送HEAD请求查看文件是否存在
        HttpResponse response = null;
        try {
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            if (response.getStatus() != HttpStatus.HTTP_OK){
                //这块不要报错或者抛异常，有可能只是浏览器不支持HEAD请求，但是他数据是存在的,直接结束方法就好
                return;
            }
            //4.校验文件类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)){
                //允许的图片类型
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg","image/jpg","image/png","image/webp");
                if (!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase())){
                    throw new BusinessException(ErrorCode.PARAMS_ERROR,"文件类型错误!");
                }
            }
            //5.校验文件大小
            String contentLengthStr = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)){
                try {
                    long parseLong = Long.parseLong(contentLengthStr);
                    final long TWO_MB = 2 * 1024 * 1024L;
                    ThrowUtils.throwIf(parseLong > TWO_MB,"文件大小不能超过2MB!",ErrorCode.PARAMS_ERROR);
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR,"文件大小格式错误！");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            //释放http请求资源
            if (response != null) {
                response.close();
            }
        }
    }  
  
    @Override  
    protected String getOriginFilename(Object inputSource) {  
        String fileUrl = (String) inputSource;  
        // 从 URL 中提取文件名  
        return FileUtil.mainName(fileUrl);
    }  
  
    @Override  
    protected void processFile(Object inputSource, File file) throws Exception {
        String fileUrl = (String) inputSource;  
        // 下载文件到临时目录  
        HttpUtil.downloadFile(fileUrl, file);
    }  
}
