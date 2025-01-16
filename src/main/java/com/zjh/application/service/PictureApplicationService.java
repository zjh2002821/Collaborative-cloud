package com.zjh.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjh.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.zjh.interfaces.dto.picture.*;
import com.zjh.domain.picture.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zjh.domain.user.entity.User;
import com.zjh.interfaces.vo.picture.PictureVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author zjh20
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2024-12-29 00:26:52
*/
public interface PictureApplicationService extends IService<Picture> {
    /**
     *图片上传
     * @param inputSource 文件资源
     * @param pictureUploadRequest 文件id
     * @param loginUser 登录用户
     * @return PictureVO 返回图片信息视图
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 删除图片
     * @param pictureId
     * @param loginUser
     */
    void deletePicture(long pictureId, User loginUser);

    /**
     * 编辑图片
     * @param picture
     * @param loginUser
     */
    void editPicture(Picture picture, User loginUser);

    /**
     * 批量更新图片标签和分类
     * @param pictureEditByBatchRequest
     * @param loginUser
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    /**
     * 创建扩图任务
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     * @return
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);

    /**
     * 获取图片封装类（单条）
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 获取图片封装（分页）
     * @param picturePage 分页参数
     * @param request 请求
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 根据颜色搜索图片
     * @param spaceId
     * @param picColor
     * @param loginUser
     * @return
     */
    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);

    /**
     * 校验图片信息
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 校验权限
     * @param loginUser
     * @param picture
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 分页查询，按照pictureQueryRequest参数作为查询条件拼接
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充更新后的图片信息，
     *      管理员自动过审需要填充审核信息，
     *      普通用户需要将审核状态修改为待审核
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser
    );


    /**
     * 清除该图片的cos存储
     * @param oldPicture
     */
    void clearPictureFile(Picture oldPicture);
}
