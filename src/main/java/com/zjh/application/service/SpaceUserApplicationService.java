package com.zjh.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zjh.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.zjh.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.zjh.domain.space.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zjh.interfaces.vo.space.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author zjh20
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-01-12 22:34:45
*/
public interface SpaceUserApplicationService extends IService<SpaceUser> {

    /**
     * 团队空间添加成员
     * @param spaceUserAddRequest
     * @return
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 校验参数
     * @param spaceUser
     * @param add
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 拼接查询参数
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 查询单个空间成员封装类
     * @param spaceUser
     * @param request
     * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * 查询成员封装类列表
     * @param spaceUserList
     * @return
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);
}
