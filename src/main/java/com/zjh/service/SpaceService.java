package com.zjh.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjh.model.dto.space.SpaceAddRequest;
import com.zjh.model.dto.space.SpaceQueryRequest;
import com.zjh.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zjh.model.entity.User;
import com.zjh.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author zjh20
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-01-04 04:23:33
*/
public interface SpaceService extends IService<Space> {

    /**
     * 创建空间（普通用户）
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 针对空间实体类参数的校验
     * @param space
     * @param add
     */
    void validSpace(Space space, boolean add);

    /**
     * 根据空间级别，自动填充限额
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 将空间实体类转换成vo类
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取空间封装（分页）
     * @param SpacePage 分页参数
     * @param request 请求
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> SpacePage, HttpServletRequest request);

    /**
     * 分页查询，按照pictureQueryRequest参数作为查询条件拼接
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);
}
