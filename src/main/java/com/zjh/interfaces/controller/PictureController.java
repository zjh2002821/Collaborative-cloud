package com.zjh.interfaces.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zjh.infrastructure.annotation.AuthCheck;
import com.zjh.infrastructure.api.aliyunai.ALiYunAiApi;
import com.zjh.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.zjh.infrastructure.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.zjh.infrastructure.api.imagesearch.ImageSearchApiFacade;
import com.zjh.infrastructure.api.imagesearch.model.ImageSearchResult;
import com.zjh.infrastructure.common.BaseResponse;
import com.zjh.infrastructure.common.DeleteRequest;
import com.zjh.infrastructure.common.ResultUtils;
import com.zjh.domain.user.constant.UserConstant;
import com.zjh.infrastructure.exception.BusinessException;
import com.zjh.infrastructure.exception.ErrorCode;
import com.zjh.infrastructure.exception.ThrowUtils;
import com.zjh.interfaces.assembler.PictureAssembler;
import com.zjh.interfaces.dto.picture.*;
import com.zjh.shared.auth.SpaceUserAuthManager;
import com.zjh.shared.auth.annotation.SaSpaceCheckPermission;
import com.zjh.shared.auth.model.SpaceUserPermissionConstant;
import com.zjh.domain.picture.entity.Picture;
import com.zjh.domain.space.entity.Space;
import com.zjh.domain.user.entity.User;
import com.zjh.domain.picture.valueobject.PictureReviewStatusEnum;
import com.zjh.interfaces.vo.picture.PictureTagCategory;
import com.zjh.interfaces.vo.picture.PictureVO;
import com.zjh.application.service.PictureApplicationService;
import com.zjh.application.service.SpaceApplicationService;
import com.zjh.application.service.UserApplicationService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author zjh
 * @version 1.0
 * 图片接口
 */
@RestController
@RequestMapping("/picture")
public class PictureController {
    @Resource
    private UserApplicationService userApplicationService;
    @Resource
    private PictureApplicationService pictureApplicationService;
    @Resource
    private SpaceApplicationService spaceApplicationService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ALiYunAiApi aLiYunAiApi;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;
    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)
            .maximumSize(10000L)
            // 缓存 5 分钟移除
            .expireAfterWrite(5L, TimeUnit.MINUTES)
            .build();

    /**
     * 上传图片（可重新上传）
     */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userApplicationService.getLoginUser(request);
        PictureVO pictureVO = pictureApplicationService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 上传图片（可重新上传）
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userApplicationService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureApplicationService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 管理员抓取图片
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(
            @RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null,ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        Integer pictureVO = pictureApplicationService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 以图识图
     */
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(
            @RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        ThrowUtils.throwIf(searchPictureByPictureRequest == null,ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null,ErrorCode.PARAMS_ERROR);
        Picture picture = pictureApplicationService.getById(pictureId);
        ThrowUtils.throwIf(picture == null,ErrorCode.PARAMS_ERROR);
        String thumbnailUrl = picture.getThumbnailUrl();
        ThrowUtils.throwIf(thumbnailUrl == null,ErrorCode.PARAMS_ERROR);
        List<ImageSearchResult> imageSearchResults = ImageSearchApiFacade.searchImage(thumbnailUrl);
        return ResultUtils.success(imageSearchResults);
    }

    /**
     * 根据颜色搜索图片
     */
    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByColor(
            @RequestBody SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(searchPictureByColorRequest == null,ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        String picColor = searchPictureByColorRequest.getPicColor();
        List<PictureVO> pictureVOList = pictureApplicationService.searchPictureByColor(spaceId, picColor, loginUser);
        return ResultUtils.success(pictureVOList);
    }

    /**
     * 删除图片
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userApplicationService.getLoginUser(request);
        long id = deleteRequest.getId();
        pictureApplicationService.deletePicture(id,loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 编辑图片（给用户使用）
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest,
                                             HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userApplicationService.getLoginUser(request);
        // 在此处将实体类和 DTO 进行转换
        Picture picture = PictureAssembler.toPictureEntity(pictureEditRequest);
        pictureApplicationService.editPicture(picture, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片（仅管理员可用）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest,
                                               HttpServletRequest request) {
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 数据校验
        pictureApplicationService.validPicture(picture);
        // 判断是否存在
        long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureApplicationService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //填充更新后的图片信息，管理员自动过审需要填充审核信息，普通用户需要将审核状态修改为待审核
        User loginUser = userApplicationService.getLoginUser(request);
        pictureApplicationService.fillReviewParams(picture,loginUser);
        // 操作数据库
        boolean result = pictureApplicationService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        //清除云存储图片地址
        pictureApplicationService.clearPictureFile(oldPicture);
        return ResultUtils.success(true);
    }

    /**
     * 批量更新图片标签和分类
     * @param pictureEditByBatchRequest
     * @param request
     */
    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        pictureApplicationService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 创建扩图任务
     * @param createPictureOutPaintingTaskRequest
     * @param request
     * @return
     */
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(
            @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
            HttpServletRequest request) {
        if (createPictureOutPaintingTaskRequest == null || createPictureOutPaintingTaskRequest.getPictureId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userApplicationService.getLoginUser(request);
        CreateOutPaintingTaskResponse response = pictureApplicationService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 查询扩图任务
     * @param taskId
     * @return
     */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId){
        ThrowUtils.throwIf(StrUtil.isBlank(taskId),ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse outPaintingTask = aLiYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(outPaintingTask);
    }


    /**
     * 根据 id 获取图片（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureApplicationService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(picture);
    }

    /**
     * 根据 id 获取图片（封装类）
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureApplicationService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 空间的图片，需要校验权限
        Space space = null;
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
//            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
//            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
//             User loginUser = userService.getLoginUser(request);
//             pictureService.checkPictureAuth(loginUser, picture);
            space = spaceApplicationService.getById(spaceId);
            ThrowUtils.throwIf(space == null, "空间不存在", ErrorCode.NOT_FOUND_ERROR);
        }
        // 获取权限列表
        User loginUser = userApplicationService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        PictureVO pictureVO = pictureApplicationService.getPictureVO(picture, request);
        pictureVO.setPermissionList(permissionList);
        // 获取封装类
        return ResultUtils.success(pictureVO);
    }


    /**
     * 分页获取图片列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 查询数据库
        Page<Picture> picturePage = pictureApplicationService.page(new Page<>(current, size),
                pictureApplicationService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Long spaceId = pictureQueryRequest.getSpaceId();
        //如果是分页查询公共空间
        if (spaceId == null){
            //普通用户只能看到过审的图片
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        }else {//如果分页查询私有空间
            //只允许空间本人
//            boolean b = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
//            ThrowUtils.throwIf(!b,ErrorCode.NO_AUTH_ERROR);
//                        // 私有空间
//            User loginUser = userService.getLoginUser(request);
//            Space space = spaceService.getById(spaceId);
//            ThrowUtils.throwIf(space == null, "空间不存在", ErrorCode.NOT_FOUND_ERROR);
//            if (!loginUser.getId().equals(space.getUserId())) {
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
//            }
        }
        // 查询数据库
        Page<Picture> picturePage = pictureApplicationService.page(new Page<>(current, size),
                pictureApplicationService.getQueryWrapper(pictureQueryRequest));
        // 获取封装类
        return ResultUtils.success(pictureApplicationService.getPictureVOPage(picturePage, request));
    }

    /**
     * 分页获取图片列表从缓存查询（封装类）
     */
    @PostMapping("/list/page/vo/cache")
    @Deprecated
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(
            @RequestBody PictureQueryRequest pictureQueryRequest,
            HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        //普通用户只能看到过审的图片
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        //构建redis缓存key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        //使用md5加密
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        //拼接key
        String cacheKey = "collaborative-cloud:listPictureVOByPageWithCache:" + hashKey;
        //先去本地缓存中查询数据
        String cacheValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cacheValue != null){
            //添加本地缓存
            LOCAL_CACHE.put(cacheKey,cacheValue);
            //将查询的数据反序列化
            Page<PictureVO> cachepage = JSONUtil.toBean(cacheValue, Page.class);
            return ResultUtils.success(cachepage);
        }
        //本地缓存未命中，去redis缓存查询该数据
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        cacheValue = opsForValue.get(cacheKey);
        if (cacheValue != null){
            //将查询的数据反序列化
            Page<PictureVO> cachepage = JSONUtil.toBean(cacheValue, Page.class);
            return ResultUtils.success(cachepage);
        }
        //本地缓存和redis缓存都未命中，查询数据库
        Page<Picture> picturePage = pictureApplicationService.page(new Page<>(current, size),
                pictureApplicationService.getQueryWrapper(pictureQueryRequest));
        // 获取封装类
        Page<PictureVO> pictureVOPage = pictureApplicationService.getPictureVOPage(picturePage, request);
        //将封装类序列化,存入redis缓存，和本地缓存
        String cachedValue = JSONUtil.toJsonStr(pictureVOPage);
        //设置redis过期时间5-10分钟，防止出现缓存雪崩问题
        int cacheTimeOut = 300 + (RandomUtil.randomInt(0,300));
        opsForValue.set(cacheKey,cachedValue,cacheTimeOut, TimeUnit.SECONDS);
        LOCAL_CACHE.put(cacheKey,cachedValue);
        return ResultUtils.success(pictureVOPage);
    }

    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }


    /**
     * 图片审核
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(
            @RequestBody PictureReviewRequest pictureReviewRequest,
            HttpServletRequest request) {
        if (pictureReviewRequest == null || pictureReviewRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //查询登录人信息
        User loginUser = userApplicationService.getLoginUser(request);
        pictureApplicationService.doPictureReview(pictureReviewRequest,loginUser);

        return ResultUtils.success(true);
    }



}
