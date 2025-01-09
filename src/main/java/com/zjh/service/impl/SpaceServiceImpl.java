package com.zjh.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjh.exception.BusinessException;
import com.zjh.exception.ErrorCode;
import com.zjh.exception.ThrowUtils;
import com.zjh.model.dto.space.SpaceAddRequest;
import com.zjh.model.dto.space.SpaceQueryRequest;
import com.zjh.model.entity.Space;
import com.zjh.model.entity.User;
import com.zjh.model.enums.SpaceLevelEnum;
import com.zjh.model.vo.SpaceVO;
import com.zjh.model.vo.UserVO;
import com.zjh.service.SpaceService;
import com.zjh.mapper.SpaceMapper;
import com.zjh.service.UserService;
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
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
* @author zjh20
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-01-04 04:23:33
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService{
    @Resource
    private UserService userService;
    @Resource
    TransactionTemplate transactionTemplate;

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
        //2.填充该级别空间的大小
        this.fillSpaceBySpaceLevel(space);
        //3.数据校验
        this.validSpace(space,true);
        //4.添加创建人
        Long id = loginUser.getId();
        space.setUserId(id);
        //5.校验权限
        if (space.getSpaceLevel() != SpaceLevelEnum.COMMON.getValue() && !userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无法创建指定级别空间，如有需要请联系管理员qq:1243267647");
        }
        //防止用户多次点击，重复创建空间:加锁
        Object lock = lockMap.computeIfAbsent(id, key -> new Object());
        synchronized (lock){
            try {
                //使用spring的编程式事务，防止出现锁释放，但事务还没提交，导致判断该空间还没有创建而出现重复创建的问题
                Long execute = transactionTemplate.execute(status -> {
                    //查找该用户是否已经创建空间
                    boolean exists = this.lambdaQuery()
                            .eq(Space::getUserId, id)
                            .exists();
                    ThrowUtils.throwIf(exists, "请勿重复创建空间！", ErrorCode.OPERATION_ERROR);
                    boolean save = this.save(space);
                    ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR);
                    return space.getUserId();

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
     * 针对空间实体类参数的校验
     * @param space
     * @param add
     */
    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        // 要创建
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
        }
        // 修改数据时，如果要改空间级别
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
    }

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
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
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
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        SpaceVOList.forEach(SpaceVO -> {
            Long userId = SpaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            SpaceVO.setUser(userService.getUserVO(user));
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
        //拼接查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

}




