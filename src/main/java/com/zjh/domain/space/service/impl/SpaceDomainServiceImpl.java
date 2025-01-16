package com.zjh.domain.space.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjh.application.service.SpaceApplicationService;
import com.zjh.application.service.SpaceUserApplicationService;
import com.zjh.application.service.UserApplicationService;
import com.zjh.domain.space.entity.Space;
import com.zjh.domain.space.entity.SpaceUser;
import com.zjh.domain.space.repository.SpaceRepository;
import com.zjh.domain.space.service.SpaceDomainService;
import com.zjh.domain.space.valueobject.SpaceLevelEnum;
import com.zjh.domain.space.valueobject.SpaceRoleEnum;
import com.zjh.domain.space.valueobject.SpaceTypeEnum;
import com.zjh.domain.user.entity.User;
import com.zjh.infrastructure.exception.BusinessException;
import com.zjh.infrastructure.exception.ErrorCode;
import com.zjh.infrastructure.exception.ThrowUtils;
import com.zjh.infrastructure.mapper.SpaceMapper;
import com.zjh.interfaces.dto.space.SpaceAddRequest;
import com.zjh.interfaces.dto.space.SpaceQueryRequest;
import com.zjh.interfaces.vo.space.SpaceVO;
import com.zjh.interfaces.vo.user.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
* @author zjh20
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-01-04 04:23:33
*/
@Service
public class SpaceDomainServiceImpl implements SpaceDomainService {
    @Resource
    private SpaceRepository spaceRepository;

    private Map<Long,Object> lockMap = new ConcurrentHashMap<>();

    /**
     * 根据空间级别，自动填充限额
     * @param space
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 根据空间级别，自动填充限额
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    /**
     * 分页查询，按照pictureQueryRequest参数作为查询条件拼接
     * @param spaceQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        //拼接查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceType", spaceType);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 校验该空间是否为本人或管理员操作
     * @param space
     * @param loginUser
     */
    @Override
    public void checkSpaceAuth(Space space, User loginUser){
        // 仅本人或管理员可删除
        if (!space.getUserId().equals(loginUser.getId()) && !loginUser.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }

}




