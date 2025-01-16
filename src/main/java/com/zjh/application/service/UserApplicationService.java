package com.zjh.application.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjh.infrastructure.common.DeleteRequest;
import com.zjh.interfaces.dto.user.UserLoginRequest;
import com.zjh.interfaces.dto.user.UserQueryRequest;
import com.zjh.domain.user.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zjh.interfaces.dto.user.UserRegisterRequest;
import com.zjh.interfaces.vo.user.LoginUserVO;
import com.zjh.interfaces.vo.user.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
* @author zjh20
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2024-12-25 22:35:47
*/
public interface UserApplicationService{

    /**
     * 用户注册
     *
     * @param userRegisterRequest   用户注册请求
     * @return 新用户 id
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录
     *
     * @param userLoginRequest  用户登录
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(UserLoginRequest userLoginRequest,HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);
    /**
     * 获取脱敏的已登录用户信息
     *
     * @return
     */
    LoginUserVO getLoginUserVO(User user);


    /**
     * 用户注销（退出登录）
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏后单个用户信息
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后用户信息列表
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);


    /**
     * 分页查询，按照userQueryRequest参数作为查询条件拼接
     * @param userQueryRequest
     * @return
     */
    Wrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    long addUser(User user);

    User getUserById(long id);

    UserVO getUserVOById(long id);

    boolean deleteUser(DeleteRequest deleteRequest);

    void updateUser(User user);

    Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest);

    List<User> listByIds(Set<Long> userIdSet);

    long saveUser(User user);
}
