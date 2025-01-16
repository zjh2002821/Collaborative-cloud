package com.zjh.domain.space.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zjh.domain.space.entity.SpaceUser;
import com.zjh.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.zjh.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.zjh.interfaces.vo.space.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author zjh20
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-01-12 22:34:45
*/
public interface SpaceUserDomainService{
    /**
     * 拼接查询参数
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

}
