package com.zjh.domain.space.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjh.application.service.SpaceApplicationService;
import com.zjh.application.service.SpaceUserApplicationService;
import com.zjh.application.service.UserApplicationService;
import com.zjh.domain.space.entity.Space;
import com.zjh.domain.space.entity.SpaceUser;
import com.zjh.domain.space.service.SpaceUserDomainService;
import com.zjh.domain.space.valueobject.SpaceRoleEnum;
import com.zjh.domain.user.entity.User;
import com.zjh.infrastructure.exception.BusinessException;
import com.zjh.infrastructure.exception.ErrorCode;
import com.zjh.infrastructure.exception.ThrowUtils;
import com.zjh.infrastructure.mapper.SpaceUserMapper;
import com.zjh.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.zjh.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.zjh.interfaces.vo.space.SpaceUserVO;
import com.zjh.interfaces.vo.space.SpaceVO;
import com.zjh.interfaces.vo.user.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author zjh20
* @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
* @createDate 2025-01-12 22:34:45
*/
@Service
public class SpaceUserDomainServiceImpl implements SpaceUserDomainService {

    /**
     * 拼接查询参数
     * @param spaceUserQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceRole), "spaceRole", spaceRole);
        return queryWrapper;
    }


}




