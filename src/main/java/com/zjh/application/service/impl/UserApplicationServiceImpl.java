package com.zjh.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjh.domain.user.service.UserDomainService;
import com.zjh.infrastructure.common.DeleteRequest;
import com.zjh.infrastructure.exception.BusinessException;
import com.zjh.infrastructure.exception.ErrorCode;
import com.zjh.infrastructure.exception.ThrowUtils;
import com.zjh.interfaces.dto.user.UserLoginRequest;
import com.zjh.interfaces.dto.user.UserQueryRequest;
import com.zjh.interfaces.dto.user.UserRegisterRequest;
import com.zjh.interfaces.vo.user.LoginUserVO;
import com.zjh.interfaces.vo.user.UserVO;
import com.zjh.application.service.UserApplicationService;
import com.zjh.domain.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
* @author zjh20
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2024-12-25 22:35:47
*/
@Service
@Slf4j
public class UserApplicationServiceImpl implements UserApplicationService {
    @Resource
    private UserDomainService userDomainService;

    /**
     *用户注册
     * @param userRegisterRequest   用户注册请求
     * @return 新用户id
     */
    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        //校验
        User.validUserRegister(userAccount,userPassword,checkPassword);
        //执行领域服务层业务接口
        return userDomainService.userRegister(userAccount,userPassword,checkPassword);
    }

    /**
     *用户登录
     * @param loginRequest  用户登录
     * @return 脱敏后数据
     */
    @Override
    public LoginUserVO userLogin(UserLoginRequest loginRequest,HttpServletRequest request) {
        //1.校验账户和密码是否正确
        String userAccount = loginRequest.getUserAccount();
        String userPassword = loginRequest.getUserPassword();
        User.validUserLogin(userAccount,userPassword);
        //2.执行领域服务层业务接口
        return userDomainService.userLogin(userAccount,userPassword,request);
    }

    /**获取登录用户
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        //1.执行领域服务层业务接口
        return userDomainService.getLoginUser(request);
    }

    /**
     *用户注销
     * @param request
     * @return
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        //1.执行领域服务层业务接口
        return userDomainService.userLogout(request);
    }

    /**
     *获取脱敏后用户信息
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        //执行领域服务层业务接口
        return userDomainService.getUserVO(user);
    }

    /**
     *获取脱敏后的用户列表
     * @param userList
     * @return
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        //执行领域服务层业务接口
        return userDomainService.getUserVOList(userList);
    }

    /**
     * 密码加盐加密
     * @param userPassword
     * @return
     */
    public String getEncryptPassword(String userPassword) {
        //执行领域服务层业务接口
        return userDomainService.getEncryptPassword(userPassword);
    }

    /**
     * 将user信息脱敏
     * @param user
     * @return
     */
    public LoginUserVO getLoginUserVO(User user){
        //执行领域服务层业务接口
        return userDomainService.getLoginUserVO(user);
    }

    /**
     * 分页查询，按照userQueryRequest参数作为查询条件拼接
     * @param userQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        //执行领域服务层业务接口
        return userDomainService.getQueryWrapper(userQueryRequest);
    }

    @Override
    public long addUser(User user) {
        return userDomainService.addUser(user);
    }

    @Override
    public User getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userDomainService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return user;
    }

    @Override
    public UserVO getUserVOById(long id) {
        return userDomainService.getUserVO(getUserById(id));
    }

    @Override
    public boolean deleteUser(DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return userDomainService.removeById(deleteRequest.getId());
    }

    @Override
    public void updateUser(User user) {
        boolean result = userDomainService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        Page<User> userPage = userDomainService.page(new Page<>(current, size),
                userDomainService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, size, userPage.getTotal());
        List<UserVO> userVO = userDomainService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVO);
        return userVOPage;
    }

    @Override
    public List<User> listByIds(Set<Long> userIdSet) {
        return userDomainService.listByIds(userIdSet);
    }

    @Override
    public long saveUser(User user) {
        //3.设置默认密码
        String password = "12345678";
        user.setUserPassword(password);
        boolean save = this.userDomainService.saveUser(user);
        ThrowUtils.throwIf(!save,ErrorCode.OPERATION_ERROR);
        return user.getId();
    }
}



