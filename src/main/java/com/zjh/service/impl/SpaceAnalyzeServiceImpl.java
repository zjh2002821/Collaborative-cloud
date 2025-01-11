package com.zjh.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjh.exception.BusinessException;
import com.zjh.exception.ErrorCode;
import com.zjh.exception.ThrowUtils;
import com.zjh.mapper.SpaceMapper;
import com.zjh.model.dto.analyze.*;
import com.zjh.model.entity.Picture;
import com.zjh.model.entity.Space;
import com.zjh.model.entity.User;
import com.zjh.model.vo.analyze.*;
import com.zjh.service.PictureService;
import com.zjh.service.SpaceAnalyzeService;
import com.zjh.service.SpaceService;
import com.zjh.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zjh
 * @version 1.0
 * 分析模块
 */
@Service
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper,Space> implements SpaceAnalyzeService {
    @Resource
    private UserService userService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private PictureService pictureService;

    /**
     * 获取空间使用分析数据
     * @param spaceUsageAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null,ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
        boolean queryPublic = spaceUsageAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceUsageAnalyzeRequest.isQueryAll();

        //1.查询全部或者公共空间逻辑
        //仅管理员
        if (queryAll || queryPublic){
            //校验权限
            checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest,loginUser);
            //指定查询的列
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");
            //如果查询公共空间，就添加一个spaceId = null条件
            if (!queryAll){
                queryWrapper.isNull("spaceId");
            }
            //使用查询图片的queryWrapper，查询图片对象
            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            //将结果四舍五入保存两位小数
            long usedSize = pictureObjList.stream().mapToLong(result -> result instanceof Long ? (Long) result : 0).sum();
            Long usedCount = (long) pictureObjList.size();
            //封装返回响应类
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);
            spaceUsageAnalyzeResponse.setUsedCount(usedCount);
            //公共图库无上限，无比例
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            return spaceUsageAnalyzeResponse;
        }
        //2.查询个人空间逻辑
        //仅管理员或者本人
        if (spaceId != null){
            //校验权限
            checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest,loginUser);
            //去空间类中查询参数
            Space space = spaceService.getById(spaceId);
            Long maxSize = space.getMaxSize();
            Long maxCount = space.getMaxCount();
            Long totalSize = space.getTotalSize();
            Long totalCount = space.getTotalCount();
            double countUsageRatio = NumberUtil.round(totalCount * 100.0 / maxCount,2).doubleValue();
            double sizeUsageRatio = NumberUtil.round(totalSize * 100.0 / maxSize,2).doubleValue();
            //封装返回响应类
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(totalSize);
            spaceUsageAnalyzeResponse.setUsedCount(totalCount);
            spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
            spaceUsageAnalyzeResponse.setMaxSize(maxSize);
            spaceUsageAnalyzeResponse.setMaxCount(maxCount);
            return spaceUsageAnalyzeResponse;
        }
        return null;
    }

    /**
     * 按照分类分析空间
     * @param spaceCategoryAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(
            SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest,
            User loginUser){
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null,ErrorCode.PARAMS_ERROR);
        //校验权限
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest,loginUser);
        //构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest,queryWrapper);
        //使用mybatis-plus分组查询
        queryWrapper.select("category AS category","COUNT(*) AS count","SUM(picSize) AS totalSize")
                .groupBy("category");

        List<SpaceCategoryAnalyzeResponse> collect = pictureService.getBaseMapper().selectMaps(queryWrapper).stream()
                .map(result -> {
                    String category = result.get("category") != null ? result.get("category").toString() : "未分类";
                    Long count = ((Number)result.get("count")).longValue();
                    Long totalSize =((Number)result.get("totalSize")).longValue();
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                })
                .collect(Collectors.toList());
        return collect;
    }

    /**
     * 按照标签分析空间
     * @param spaceTagAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser){
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null,ErrorCode.PARAMS_ERROR);
        //校验权限
        checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest,loginUser);
        //构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest,queryWrapper);

        //查询所有符合条件的标签
        queryWrapper.select("tags");
        List<String> tagsJsonList = pictureService.getBaseMapper().selectObjs(queryWrapper).stream()
                .filter(ObjUtil::isNotNull)
                .map(Object::toString)
                .collect(Collectors.toList());
        //合并所有标签并统计使用次数
        Map<String, Long> tagCountMap = tagsJsonList.stream()
                .flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

        List<SpaceTagAnalyzeResponse> collect = tagCountMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        return collect;
    }

    /**
     * 按照大小分析空间
     * @param spaceSizeAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

        // 检查权限
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);

        // 查询所有符合条件的图片大小
        queryWrapper.select("picSize");
        List<Long> picSizes = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .map(size -> ((Number) size).longValue())
                .collect(Collectors.toList());

        // 定义分段范围，注意使用有序 Map
        Map<String, Long> sizeRanges = new LinkedHashMap<>();
        sizeRanges.put("<100KB", picSizes.stream().filter(size -> size < 100 * 1024).count());
        sizeRanges.put("100KB-500KB", picSizes.stream().filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());
        sizeRanges.put("500KB-1MB", picSizes.stream().filter(size -> size >= 500 * 1024 && size < 1 * 1024 * 1024).count());
        sizeRanges.put(">1MB", picSizes.stream().filter(size -> size >= 1 * 1024 * 1024).count());

        // 转换为响应对象
        return sizeRanges.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 按照用户行为分析空间
     * @param spaceUserAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 检查权限
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        Long userId = spaceUserAnalyzeRequest.getUserId();
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);

        // 分析维度：每日、每周、每月
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') AS period", "COUNT(*) AS count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime) AS period", "COUNT(*) AS count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') AS period", "COUNT(*) AS count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度");
        }

        // 分组和排序
        queryWrapper.groupBy("period").orderByAsc("period");

        // 查询结果并转换
        List<Map<String, Object>> queryResult = pictureService.getBaseMapper().selectMaps(queryWrapper);
        return queryResult.stream()
                .map(result -> {
                    String period = result.get("period").toString();
                    Long count = ((Number) result.get("count")).longValue();
                    return new SpaceUserAnalyzeResponse(period, count);
                })
                .collect(Collectors.toList());
    }

    /**
     * 按照使用排行分析空间
     * @param spaceRankAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

        // 仅管理员可查看空间排行
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), "无权查看空间排行", ErrorCode.NO_AUTH_ERROR);

        // 构造查询条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "spaceName", "userId", "totalSize")
                .orderByDesc("totalSize")
                .last("LIMIT " + spaceRankAnalyzeRequest.getTopN()); // 取前 N 名

        // 查询结果
        return spaceService.list(queryWrapper);
    }


    /**
     * 根据不通过使用场景，拼接不同参数
     * @param spaceAnalyzeRequest
     * @param queryWrapper
     */
    public static void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper){
        //如果管理员要查看全部空间占用率,就不拼接参数，直接去图片表查询全部的图片
        if (spaceAnalyzeRequest.isQueryAll()){
            return;
        }
        //如果管理员查看公共空间占用率，就拼接一个空间id为null的条件
        if (spaceAnalyzeRequest.isQueryPublic()){
            queryWrapper.isNull("spaceId");
            return;
        }
        //如果管理员或用户查看自身空间占用率，就拼接空间id的条件
        if (spaceAnalyzeRequest.getSpaceId() != null){
            queryWrapper.eq("spaceId",spaceAnalyzeRequest.getSpaceId());
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR);
    }

    /**
     * 校验分析模块权限
     * @param spaceAnalyzeRequest
     * @param loginUser
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser){
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        //1.校验权限
        //如果queryPublic，queryAll为true，管理员可访问
        if (queryAll || queryPublic){
            ThrowUtils.throwIf(!userService.isAdmin(loginUser),"无权访问公共图库", ErrorCode.NO_AUTH_ERROR);
        }else {//如果空间id不为空，管理员和本人可以访问
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(ObjUtil.isNull(space),ErrorCode.NOT_FOUND_ERROR);
            // 仅本人或管理员可操作
            spaceService.checkSpaceAuth(space,loginUser);
        }
    }

}
