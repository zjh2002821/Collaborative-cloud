package com.zjh.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjh.api.aliyunai.ALiYunAiApi;
import com.zjh.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.zjh.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.zjh.constant.UserConstant;
import com.zjh.exception.BusinessException;
import com.zjh.exception.ErrorCode;
import com.zjh.exception.ThrowUtils;
import com.zjh.manager.CosManager;
import com.zjh.manager.FileManager;
import com.zjh.manager.upload.FilePictureUpload;
import com.zjh.manager.upload.PictureUploadTemplate;
import com.zjh.manager.upload.UrlPictureUpload;
import com.zjh.model.dto.file.UploadPictureResult;
import com.zjh.model.dto.picture.*;
import com.zjh.model.entity.Picture;
import com.zjh.model.entity.Space;
import com.zjh.model.entity.User;
import com.zjh.model.enums.PictureReviewStatusEnum;
import com.zjh.model.vo.PictureVO;
import com.zjh.model.vo.UserVO;
import com.zjh.service.PictureService;
import com.zjh.mapper.PictureMapper;
import com.zjh.service.SpaceService;
import com.zjh.service.UserService;
import com.zjh.utils.ColorSimilarUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author zjh20
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2024-12-29 00:26:52
*/
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService{
    @Resource
    private FileManager fileManager;
    @Resource
    private UserService userService;
    @Resource
    private FilePictureUpload filePictureUpload;
    @Resource
    private UrlPictureUpload urlPictureUpload;
    @Resource
    private CosManager cosManager;
    @Resource
    private SpaceService spaceService;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private ALiYunAiApi aLiYunAiApi;

    /**
     *图片上传
     * @param inputSource 文件资源
     * @param pictureUploadRequest 文件id
     * @param loginUser 登录用户
     * @return PictureVO 返回图片信息视图
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser){
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        //校验空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null){
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(ObjUtil.isNull(space),"空间不存在！",ErrorCode.PARAMS_ERROR);
            //校验是否具有该空间权限，只有创建该空间的人才可以上传
            if (!loginUser.getId().equals(space.getUserId())){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有该空间权限！");
            }
            if (space.getTotalCount() >= space.getMaxCount()){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"空间条数不足！");
            }
            if (space.getTotalSize() >= space.getMaxSize()){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"空间大小不足！");
            }
        }
        //1.判断是新增还是更新
        Long pictureId = null;
        if (pictureUploadRequest != null){
            pictureId = pictureUploadRequest.getId();
        }
        //2.更新图片校验该图片是否存在
        if (pictureId != null){
            //查询该图片信息
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null,"图片不存在！",ErrorCode.NOT_FOUND_ERROR);
            //设置更新图片仅允许本人或者管理员才可以修改
            if (!loginUser.getId().equals(oldPicture.getUserId()) && !loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE)){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            //校验空间是否一致，没有传spaceid则复用原有图片的spaceid
            if (spaceId == null){
                if (oldPicture.getSpaceId() != null){
                    spaceId = oldPicture.getSpaceId();
                }
            }else {
                //如果传了spaceid，就校验和原来图片的spaceid是否一致
                if (!spaceId.equals(oldPicture.getSpaceId())){
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无权限，空间id不一致！");
                }
            }
        }
        //3.如果没传spaceid，就代表添加的是公共图库，按照用户id划分目录
        //  如果传了spaceid，就代表上传私有空间，按照空间id划分目录
        String uploadPathPrefix;
        if (spaceId == null){
            uploadPathPrefix = String.format("public/%s",loginUser.getId());
        }else {
            uploadPathPrefix = String.format("space/%s",spaceId);
        }
        //4.上传图片，得到信息
        //根据inputSource判断是url上传还是本地文件上传
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String){
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        String url = uploadPictureResult.getUrl();//获取的是压缩后的图片地址
        String thumbnailUrl = uploadPictureResult.getThumbnailUrl();//获取的是缩略图的图片地址
        String picName = uploadPictureResult.getPicName();
        Long picSize = uploadPictureResult.getPicSize();
        int picWidth = uploadPictureResult.getPicWidth();
        int picHeight = uploadPictureResult.getPicHeight();
        Double picScale = uploadPictureResult.getPicScale();
        String picFormat = uploadPictureResult.getPicFormat();
        String picColor = uploadPictureResult.getPicColor();
        //构造要入库的图片信息
        Picture picture = new Picture();
        picture.setSpaceId(spaceId);
        picture.setUrl(url);
        picture.setThumbnailUrl(thumbnailUrl);
        if (pictureUploadRequest!= null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())){
            picName = pictureUploadRequest.getPicName();//如果是抓取上传则将文件名更换为抓取默认的名称
        }
        picture.setName(picName);
        picture.setPicSize(picSize);
        picture.setPicWidth(picWidth);
        picture.setPicHeight(picHeight);
        picture.setPicScale(picScale);
        picture.setPicFormat(picFormat);
        picture.setUserId(loginUser.getId());
        picture.setPicColor(picColor);
        //补充审核参数
        fillReviewParams(picture,loginUser);
        //如果pictureId不为null，表示更新，否则表示新增
        if (pictureId != null){
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        // 开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, "图片上传失败", ErrorCode.OPERATION_ERROR);
            if (finalSpaceId != null){
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update, "额度更新失败", ErrorCode.OPERATION_ERROR);
            }
            return picture;
        });
        return PictureVO.objToVo(picture);
    }

    /**
     * 删除图片
     * @param pictureId
     * @param loginUser
     */
    @Override
    public void deletePicture(long pictureId, User loginUser){
        // 判断是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        this.checkPictureAuth(loginUser,oldPicture);
        // 操作数据库
        // 校验权限
        checkPictureAuth(loginUser, oldPicture);
        // 开启事务
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            // 释放额度
            Long spaceId = Long.valueOf(oldPicture.getSpaceId());
            if (spaceId != null){
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, spaceId)
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update, "额度更新失败", ErrorCode.OPERATION_ERROR);
            }
            return true;
        });
        //清除云存储图片地址
        this.clearPictureFile(oldPicture);
    }

    /**
     * 编辑图片
     * @param pictureEditRequest
     * @param loginUser
     */
    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser){
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        //填充更新后的图片信息，管理员自动过审需要填充审核信息，普通用户需要将审核状态修改为待审核
        this.fillReviewParams(picture,loginUser);
        // 判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        this.checkPictureAuth(loginUser,oldPicture);
        // 操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 批量更新图片标签和分类
     * @param pictureEditByBatchRequest
     * @param loginUser
     */
    @Override
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser){
        //1.校验参数
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();
        ThrowUtils.throwIf(spaceId == null || CollUtil.isEmpty(pictureIdList),ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null,ErrorCode.NO_AUTH_ERROR);
        Space space = spaceService.getById(spaceId);
        if (space == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"空间不存在！");
        }
        if (!space.getUserId().equals(loginUser.getId())){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有空间访问权限！");
        }
        //2.查询指定图片
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId)
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList)
                .list();

        //3.遍历图片集合，批量更新标签和分类
        pictureList.forEach(picture -> {
            if (picture.getCategory() != null){
                picture.setCategory(category);
            }
            if (picture.getTags() != null){
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        });
        // 批量重命名
        String nameRule = pictureEditByBatchRequest.getNameRule();
        fillPictureWithNameRule(pictureList, nameRule);

        boolean result = this.updateBatchById(pictureList);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);

    }

    /**
     * 创建扩图任务
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     * @return
     */
    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        // 获取图片信息
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Picture picture = Optional.ofNullable(this.getById(pictureId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR));
        // 权限校验
        checkPictureAuth(loginUser, picture);
        // 构造请求参数
        CreateOutPaintingTaskRequest taskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        taskRequest.setInput(input);
        BeanUtil.copyProperties(createPictureOutPaintingTaskRequest, taskRequest);
        // 创建任务
        return aLiYunAiApi.createOutPaintingTask(taskRequest);
    }


    /**
     * 获取图片封装类（单条）
     * @param picture
     * @param request
     * @return
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 获取图片封装（分页）
     * @param picturePage 分页参数
     * @param request 请求
     * @return
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    /**
     * 根据颜色搜索图片
     * @param spaceId
     * @param picColor
     * @param loginUser
     * @return
     */
    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser){
        //1.校验参数
        ThrowUtils.throwIf(spaceId == null,"空间id为空！",ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null,"登录用户为空！",ErrorCode.PARAMS_ERROR);
        //2.校验空间，必须是本人才可以
        Long id = loginUser.getId();
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(id == null,"空间不存在！",ErrorCode.NOT_FOUND_ERROR);
        Long userId = space.getUserId();
        if (!id.equals(userId)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间创建人和登录人不一致！");
        }
        //3.查询该空间下所有图片（必须有主色调的图片）
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)//查询具有该空间id的图片
                .isNotNull(Picture::getPicColor)//过滤掉没有主色调的图片
                .list();
        //4.将目标颜色转换为color对象
        Color targetColor = Color.decode(picColor);
        //5.计算相似度并排序
        List<Picture> sortedPictures = pictureList.stream()
                .sorted(Comparator.comparingDouble(picture -> {//升序，值越大越往后排
                    String hexColor = picture.getPicColor();
                    //如果没有主色调就返回最大值排在后面
                    if (StrUtil.isBlank(hexColor)) {
                        return Double.MAX_VALUE;
                    }
                    Color pictureColor = Color.decode(hexColor);
                    double v = ColorSimilarUtils.calculateSimilarity(targetColor, pictureColor);//值越大相似度越高
                    return 1 - v;
                }))
                .limit(12)
                .collect(Collectors.toList());
        //6.转换为vo类
        List<PictureVO> pictureVOList = sortedPictures.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
        return pictureVOList;

    }

    /**
     * 校验图片信息
     * @param picture
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), "id 不能为空", ErrorCode.PARAMS_ERROR);
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, "url 过长", ErrorCode.PARAMS_ERROR);
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, "简介过长", ErrorCode.PARAMS_ERROR);
        }
    }

    /**
     * 校验权限
     * @param loginUser
     * @param picture
     */
    @Override
    public void checkPictureAuth(User loginUser, Picture picture){
        Long id = loginUser.getId();
        Long userId = picture.getUserId();
        Long spaceId = picture.getSpaceId();
        //如果是公共空间
        if (spaceId == null){
            //仅本人和管理员可以操作
            if(!id.equals(userId) && !userService.isAdmin(loginUser)){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }else {
            //如果是私有空间，则仅该空间创建人才可以操作
            if (!id.equals(userId)){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }


    /**
     * 分页查询，按照pictureQueryRequest参数作为查询条件拼接
     * @param pictureQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean isNullSpaceId = pictureQueryRequest.isNullSpaceId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();

        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(isNullSpaceId, "spaceId");
        // >= startEditTime
        queryWrapper.ge(ObjectUtil.isNotEmpty(startEditTime),"editTime",startEditTime);
        // <= endEditTime
        queryWrapper.lt(ObjectUtil.isNotEmpty(startEditTime),"editTime",endEditTime);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 审核图片
     * @param pictureReviewRequest
     * @param loginUser
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        //1.校验参数
        ThrowUtils.throwIf(pictureReviewRequest == null,ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        //2.校验id不为空，校验图片是否存在
        if (id == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Picture oldPicture = this.getById(id);
        if (oldPicture == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"该图片不存在");
        }
        //3，校验该审核状态不能为待审核，只允许通过或者拒绝
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum enumByValue = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if ( enumByValue == null || enumByValue.equals(PictureReviewStatusEnum.REVIEWING)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //4.校验是否重复审核
        if (reviewStatus.equals(oldPicture.getReviewStatus())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "重复审核！");
        }
        //5.更新审核状态
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest,picture);
        boolean b = this.updateById(picture);
        ThrowUtils.throwIf(!b,ErrorCode.OPERATION_ERROR);
    }

    /**
     * 填充更新后的图片信息，
     *      管理员自动过审需要填充审核信息，
     *      普通用户需要将审核状态修改为待审核
     * @param picture
     * @param loginUser
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser){
        if (userService.isAdmin(loginUser)){
            picture.setReviewerId(loginUser.getId());
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewTime(new Date());
            picture.setReviewMessage("管理员自动过审");
        }else {
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    /**
     * 抓取图片
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        //1.校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        if (count > 30){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"抓取图片不得超过30条！");
        }
        //2.拼接抓取地址
        String url = String.format("https://cn.bing.com/images/async?q=%25s&mmasync=1",searchText);
        //3.使用jsoup库进行网络抓取
        Document document;
        try {
            document = Jsoup.connect(url).get();
        } catch (IOException e) {
            log.info("获取页面失败",e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"获取页面失败！");
        }
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"获取元素失败！");
        }
        Elements imgElementList = div.select("img.mimg");
        int num = 0;
        for (Element imgElement : imgElementList) {
            String imgUrl = imgElement.attr("src");
            if (StrUtil.isBlank(imgUrl)){
                log.info("当前链接为空，已跳过：{}",imgUrl);
                continue;
            }
            //截取url路径
            int i = imgUrl.indexOf("?");
            if (i > -1){
                imgUrl = imgUrl.substring(0, i);
            }
            //设置抓取图片名称
            String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
            if (StrUtil.isBlank(namePrefix)){
                namePrefix = searchText;
            }
            //调用上传图片方法
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(imgUrl);
            pictureUploadRequest.setPicName(namePrefix+(num+1));
            try {
                PictureVO pictureVO = this.uploadPicture(imgUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功，id = {}",pictureVO.getId());
                num++;
            } catch (Exception e) {
                log.error("图片上传失败",e);
                continue;
            }
            if (num >= count){
                break;
            }
        }

        return num;
    }

    /**
     * 清除该图片的cos存储
     * @param oldPicture
     */
    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断该图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        cosManager.deleteObject(oldPicture.getUrl());
        // 清理缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }

    /**
     * nameRule 格式：图片{序号}
     *
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (CollUtil.isEmpty(pictureList) || StrUtil.isBlank(nameRule)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        } catch (Exception e) {
            log.error("名称解析错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析错误");
        }
    }



}




