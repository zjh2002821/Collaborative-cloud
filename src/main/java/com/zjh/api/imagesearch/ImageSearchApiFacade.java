package com.zjh.api.imagesearch;

import com.zjh.api.imagesearch.model.ImageSearchResult;
import com.zjh.api.imagesearch.sub.GetImageFirstUrlApi;
import com.zjh.api.imagesearch.sub.GetImageListApi;
import com.zjh.api.imagesearch.sub.GetImagePageUrlApi;

import java.util.List;

/**
 * @author zjh
 * @version 1.0
 * 集成以图识图api调用，提供外部统一接口
 */
public class ImageSearchApiFacade {
    public static List<ImageSearchResult> searchImage(String imageUrl){
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;
    }
}
