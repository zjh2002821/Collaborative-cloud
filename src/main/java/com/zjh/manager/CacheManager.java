package com.zjh.manager;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zjh.model.dto.picture.PictureQueryRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author zjh
 * @version 1.0
 */
@Component
@Deprecated
public class CacheManager {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
//    private ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)
            .maximumSize(10000L)
            // 缓存 5 分钟移除
            .expireAfterWrite(5L, TimeUnit.MINUTES)
            .build();
    private String cacheKey = null;
    private String cacheValue = null;

    /**
     * redis查询缓存数据
     * @param hashKey
     * @return
     */
//    public String redisGetData(String hashKey){
//        cacheValue = opsForValue.get(cacheKey);
//        return cacheValue;
//    }
//
//    /**
//     * caffeine查询本地缓存数据
//     * @param hashKey
//     * @return
//     */
//    public String caffeineGetData(String hashKey){
//        cacheValue = LOCAL_CACHE.getIfPresent(cacheKey);
//        return cacheValue;
//    }
//
//    public void redisSetData(String cacheKey,String cachedValue){
//        //设置redis过期时间5-10分钟，防止出现缓存雪崩问题
//        int cacheTimeOut = 300 + (RandomUtil.randomInt(0,300));
//        opsForValue.set(cachedValue,cachedValue,cacheTimeOut,TimeUnit.SECONDS);
//    }
//
//    public void caffeinePutData(String cacheKey,String cachedValue){
//        LOCAL_CACHE.put(cacheKey,cachedValue);
//    }
//
//    public void constructCache(PictureQueryRequest pictureQueryRequest){
//        //构建redis缓存key
//        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
//        //使用md5加密
//        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
//        //拼接key
//        cacheKey = "collaborative-cloud:listPictureVOByPageWithCache:" + hashKey;
//
//    }
}
