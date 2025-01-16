package com.zjh.infrastructure.api.imagesearch.sub;

import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.zjh.infrastructure.exception.BusinessException;
import com.zjh.infrastructure.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zjh
 * @version 1.0
 * 获取百度以图识图跳转的识图页面
 */
@Slf4j
public class GetImagePageUrlApi {
    public static String getImagePageUrl(String imageUrl){
        //1.定义识图的请求格式
        HashMap<String, Object> formData = new HashMap<>();
        formData.put("image",imageUrl);
        formData.put("tn","pc");
        formData.put("from","pc");
        formData.put("image_source","PC_UPLOAD_URL");
        //2.获取当前时间戳
        long upTime = System.currentTimeMillis();
        //3.请求地址
        String url = "https://graph.baidu.com/upload?uptime"+upTime;
        try {
            //4.发送post请求
            HttpResponse response = HttpRequest.post(url)
                    .form(formData)
                    .timeout(5000)
                    .execute();
            if (response.getStatus() != HttpStatus.HTTP_OK){
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"接口调用失败！");
            }
            //5.解析响应
            String body = response.body();
            Map<String,Object> result = JSONUtil.toBean(body, Map.class);
            //5,判断响应状态
            if (result == null || !Integer.valueOf(0).equals(result.get("status"))){
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"接口调用失败！");
            }
            //6.获取响应的url
            Map<String, Object> data = JSONUtil.parseObj(result.get("data")).toBean(Map.class);
            String rawUrl = (String) data.get("url");
            //7.对url进行解析
            String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
            if (searchResultUrl == null){
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"返回结果无效！");
            }
            return searchResultUrl;
        } catch (Exception e) {
            log.error("搜索失败！",e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"搜索失败！");
        }
    }

    public static void main(String[] args) {
        String url = "https://www.codefather.cn/logo.png";
        String imagePageUrl = getImagePageUrl(url);
        System.out.println("搜索成功，结果 = "+imagePageUrl);
    }
}


