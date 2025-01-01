package com.zjh.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjh.constant.UserConstant;
import com.zjh.exception.BusinessException;
import com.zjh.exception.ErrorCode;
import com.zjh.exception.ThrowUtils;
import com.zjh.manager.FileManager;
import com.zjh.manager.upload.FilePictureUpload;
import com.zjh.manager.upload.PictureUploadTemplate;
import com.zjh.manager.upload.UrlPictureUpload;
import com.zjh.model.dto.file.UploadPictureResult;
import com.zjh.model.dto.picture.PictureQueryRequest;
import com.zjh.model.dto.picture.PictureReviewRequest;
import com.zjh.model.dto.picture.PictureUploadByBatchRequest;
import com.zjh.model.dto.picture.PictureUploadRequest;
import com.zjh.model.entity.Picture;
import com.zjh.model.entity.User;
import com.zjh.model.enums.PictureReviewStatusEnum;
import com.zjh.model.vo.PictureVO;
import com.zjh.model.vo.UserVO;
import com.zjh.service.PictureService;
import com.zjh.mapper.PictureMapper;
import com.zjh.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
            if (!loginUser.getId().equals(oldPicture.getUserId()) || !loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE)){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
        //3.按照用户id划分目录
        String uploadPathPrefix = String.format("public/%s",loginUser.getId());
        //4.上传图片，得到信息
        //根据inputSource判断是url上传还是本地文件上传
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String){
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        String url = uploadPictureResult.getUrl();
        String picName = uploadPictureResult.getPicName();
        Long picSize = uploadPictureResult.getPicSize();
        int picWidth = uploadPictureResult.getPicWidth();
        int picHeight = uploadPictureResult.getPicHeight();
        Double picScale = uploadPictureResult.getPicScale();
        String picFormat = uploadPictureResult.getPicFormat();
        //构造要入库的图片信息
        Picture picture = new Picture();
        picture.setUrl(url);
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
        //如果pictureId不为null，表示更新，否则表示新增
        if (pictureId != null){
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        //填充更新后的图片信息，管理员自动过审需要填充审核信息，普通用户需要将审核状态修改为待审核
        fillReviewParams(picture,loginUser);
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result,"图片上传失败",ErrorCode.OPERATION_ERROR);
        return PictureVO.objToVo(picture);
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
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();

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

}




