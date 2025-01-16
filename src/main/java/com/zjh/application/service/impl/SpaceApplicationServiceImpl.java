package com.zjh.application.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjh.domain.space.service.SpaceDomainService;
import com.zjh.infrastructure.exception.BusinessException;
import com.zjh.infrastructure.exception.ErrorCode;
import com.zjh.infrastructure.exception.ThrowUtils;
import com.zjh.interfaces.dto.space.SpaceAddRequest;
import com.zjh.interfaces.dto.space.SpaceQueryRequest;
import com.zjh.domain.space.entity.Space;
import com.zjh.domain.space.entity.SpaceUser;
import com.zjh.domain.user.entity.User;
import com.zjh.domain.space.valueobject.SpaceLevelEnum;
import com.zjh.domain.space.valueobject.SpaceRoleEnum;
import com.zjh.domain.space.valueobject.SpaceTypeEnum;
import com.zjh.interfaces.vo.space.SpaceVO;
import com.zjh.interfaces.vo.user.UserVO;
import com.zjh.application.service.SpaceApplicationService;
import com.zjh.infrastructure.mapper.SpaceMapper;
import com.zjh.application.service.SpaceUserApplicationService;
import com.zjh.application.service.UserApplicationService;
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
public class SpaceApplicationServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceApplicationService {
    @Resource
    private SpaceDomainService spaceDomainService;
    @Resource
    private UserApplicationService userApplicationService;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private SpaceUserApplicationService spaceUserApplicationService;

    private Map<Long,Object> lockMap = new ConcurrentHashMap<>();


    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        //1.设置默认值
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest,space);
        if (StrUtil.isBlank(spaceAddRequest.getSpaceName())){
            space.setSpaceName("默认空间");
        }
        if (spaceAddRequest.getSpaceLevel() == null){
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        //设置默认空间类型为私有空间
        if (spaceAddRequest.getSpaceType() == null){
            space.setSpaceLevel(SpaceTypeEnum.PRIVATE.getValue());
        }
        //2.填充该级别空间的大小
        this.fillSpaceBySpaceLevel(space);
        //3.数据校验
        space.validSpace(true);
        //4.添加创建人
        Long id = loginUser.getId();
        space.setUserId(id);
        //5.校验权限
        if (space.getSpaceLevel() != SpaceLevelEnum.COMMON.getValue() && !loginUser.isAdmin()){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无法创建指定级别空间，如有需要请联系管理员qq:1243267647");
        }
        //防止用户多次点击，重复创建空间:加锁
        Object lock = lockMap.computeIfAbsent(id, key -> new Object());
        synchronized (lock){
            try {
                //使用spring的编程式事务，防止出现锁释放，但事务还没提交，导致判断该空间还没有创建而出现重复创建的问题
                Long execute = transactionTemplate.execute(status -> {
                    //查找该用户是否已经创建同类型的空间
                    boolean exists = this.lambdaQuery()
                            .eq(Space::getUserId, id)
                            .eq(Space::getSpaceType,spaceAddRequest.getSpaceType())
                            .exists();
                    ThrowUtils.throwIf(exists, "请勿重复创建空间！", ErrorCode.OPERATION_ERROR);
                    boolean save = this.save(space);
                    ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR);
                    //如果是创建团队空间，关联新增团队成员记录为创建人
                    if (SpaceTypeEnum.TEAM.getValue() == spaceAddRequest.getSpaceType()){
                        SpaceUser spaceUser = new SpaceUser();
                        spaceUser.setSpaceId(space.getId());
                        spaceUser.setUserId(id);
                        spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                        boolean result = spaceUserApplicationService.save(spaceUser);
                        ThrowUtils.throwIf(!result,"创建团队成员记录失败！",ErrorCode.OPERATION_ERROR);
                    }
                    //创建分表(仅对团队空间有效)
//                    dynamicShardingManager.createSpacePictureTable(space);
                    return space.getId();

                });
                return Optional.ofNullable(execute).orElse(-1L);
            } catch (TransactionException e) {
                throw new RuntimeException(e);
            } finally {
                lockMap.remove(id);
            }
        }

    }

    /**
     * 根据空间级别，自动填充限额
     * @param space
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
      spaceDomainService.fillSpaceBySpaceLevel(space);
    }


    /**
     * 将空间实体类转换成vo类（单条）
     * @param space
     * @param request
     * @return
     */
    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userApplicationService.getUserById(userId);
            UserVO userVO = userApplicationService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    /**
     * 获取空间封装（分页）
     * @param SpacePage 分页参数
     * @param request 请求
     * @return
     */
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> SpacePage, HttpServletRequest request) {
        List<Space> SpaceList = SpacePage.getRecords();
        Page<SpaceVO> SpaceVOPage = new Page<>(SpacePage.getCurrent(), SpacePage.getSize(), SpacePage.getTotal());
        if (CollUtil.isEmpty(SpaceList)) {
            return SpaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> SpaceVOList = SpaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = SpaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userApplicationService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        SpaceVOList.forEach(SpaceVO -> {
            Long userId = SpaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            SpaceVO.setUser(userApplicationService.getUserVO(user));
        });
        SpaceVOPage.setRecords(SpaceVOList);
        return SpaceVOPage;
    }

    /**
     * 分页查询，按照pictureQueryRequest参数作为查询条件拼接
     * @param spaceQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        return spaceDomainService.getQueryWrapper(spaceQueryRequest);
    }

    /**
     * 校验该空间是否为本人或管理员操作
     * @param space
     * @param loginUser
     */
    @Override
    public void checkSpaceAuth(Space space, User loginUser){
        spaceDomainService.checkSpaceAuth(space,loginUser);
    }

}




