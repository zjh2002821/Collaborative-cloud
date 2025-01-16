package com.zjh.domain.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjh.domain.user.constant.UserConstant;
import com.zjh.domain.user.entity.User;
import com.zjh.domain.user.repository.UserRepository;
import com.zjh.domain.user.service.UserDomainService;
import com.zjh.domain.user.valueobject.UserRoleEnum;
import com.zjh.infrastructure.exception.BusinessException;
import com.zjh.infrastructure.exception.ErrorCode;
import com.zjh.infrastructure.exception.ThrowUtils;
import com.zjh.interfaces.dto.user.UserQueryRequest;
import com.zjh.interfaces.vo.user.LoginUserVO;
import com.zjh.interfaces.vo.user.UserVO;
import com.zjh.shared.auth.StpKit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author zjh20
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2024-12-25 22:35:47
*/
@Service
@Slf4j
public class UserDomainServiceImpl implements UserDomainService {
    @Resource
    private UserRepository userRepository;

    /**
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        //2.检验该账户是否存在
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount",userAccount);
        Long l = userRepository.getBaseMapper().selectCount(userQueryWrapper);
        ThrowUtils.throwIf(l > 0,"该用户已存在",ErrorCode.PARAMS_ERROR);
        //3.加密
        String encryptPassword = getEncryptPassword(userPassword);
        //4.插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("hello!");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean save = userRepository.save(user);
        ThrowUtils.throwIf(!save,"注册失败，数据库错误！",ErrorCode.SYSTEM_ERROR);

        return user.getId();
    }

    /**
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后数据
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //2.密码加密
        String encryptPassword = getEncryptPassword(userPassword);
        //3.查询该用户是否存在
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount",userAccount);
        userQueryWrapper.eq("userPassword",encryptPassword);
        User user = userRepository.getBaseMapper().selectOne(userQueryWrapper);
        //4.用户不存在
        if (user == null){
            log.info("Check user is null");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户不存在或密码错误");
        }
        //5.记录用户登录态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE,user);
        //记录用户登录态到sa-token，便于空间权限校验使用
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE,user);
        return getLoginUserVO(user);
    }

    /**
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        //1.获取session判断是否已经登录
        Object attribute = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User user = (User) attribute;
        ThrowUtils.throwIf(user == null || user.getId() == null,ErrorCode.NOT_LOGIN_ERROR);
        return user;
    }

    /**
     *
     * @param request
     * @return
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(attribute == null,ErrorCode.NOT_LOGIN_ERROR);
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    /**
     *
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    /**
     *
     * @param userList
     * @return
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    /**
     * 密码加盐加密
     * @param userPassword
     * @return
     */
    @Override
    public  String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "zjh";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    /**
     * 将user信息脱敏
     * @param user
     * @return
     */
    public LoginUserVO getLoginUserVO(User user){
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user,loginUserVO);
        return loginUserVO;
    }

    /**
     * 分页查询，按照userQueryRequest参数作为查询条件拼接
     * @param userQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        //将接受的参数拆分
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        //判断对应参数是否为空，如果为空则不拼接该参数作为查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public Long addUser(User user) {
        // 默认密码 12345678
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = this.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userRepository.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return user.getId();
    }

    @Override
    public Boolean removeById(Long id) {
        return userRepository.removeById(id);
    }

    @Override
    public boolean updateById(User user) {
        return userRepository.updateById(user);
    }

    @Override
    public User getById(long id) {
        return userRepository.getById(id);
    }

    @Override
    public Page<User> page(Page<User> userPage, QueryWrapper<User> queryWrapper) {
        return userRepository.page(userPage, queryWrapper);
    }

    @Override
    public List<User> listByIds(Set<Long> userIdSet) {
        return userRepository.listByIds(userIdSet);
    }

    @Override
    public boolean saveUser(User user) {
        return userRepository.save(user);
    }


}




