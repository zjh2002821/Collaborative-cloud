package com.zjh.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjh.model.dto.picture.PictureQueryRequest;
import com.zjh.model.dto.picture.PictureReviewRequest;
import com.zjh.model.dto.picture.PictureUploadByBatchRequest;
import com.zjh.model.dto.picture.PictureUploadRequest;
import com.zjh.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zjh.model.entity.User;
import com.zjh.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
* @author zjh20
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2024-12-29 00:26:52
*/
public interface PictureService extends IService<Picture> {
    /**
     *图片上传
     * @param inputSource 文件资源
     * @param pictureUploadRequest 文件id
     * @param loginUser 登录用户
     * @return PictureVO 返回图片信息视图
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

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
     * 校验图片信息
     * @param picture
     */
    void validPicture(Picture picture);

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

}
