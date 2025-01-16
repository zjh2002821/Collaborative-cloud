package com.zjh.interfaces.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjh.infrastructure.annotation.AuthCheck;
import com.zjh.infrastructure.common.BaseResponse;
import com.zjh.infrastructure.common.DeleteRequest;
import com.zjh.infrastructure.common.ResultUtils;
import com.zjh.domain.user.constant.UserConstant;
import com.zjh.infrastructure.exception.ErrorCode;
import com.zjh.infrastructure.exception.ThrowUtils;
import com.zjh.interfaces.assembler.UserAssembler;
import com.zjh.interfaces.dto.user.*;
import com.zjh.domain.user.entity.User;
import com.zjh.interfaces.vo.user.LoginUserVO;
import com.zjh.interfaces.vo.user.UserVO;
import com.zjh.application.service.UserApplicationService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @author zjh
 * @version 1.0
 * 用户接口
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserApplicationService userApplicationService;

    /**
     * 用户注册
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest){
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        long l = userApplicationService.userRegister(userRegisterRequest);
        return ResultUtils.success(l);
    }

    /**
     * 用户登录
     * @param loginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest loginRequest, HttpServletRequest request){
        ThrowUtils.throwIf(loginRequest == null,ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUserVO = userApplicationService.userLogin(loginRequest,request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userApplicationService.getLoginUser(request);
        return ResultUtils.success(userApplicationService.getLoginUserVO(loginUser));
    }

    /**
     * 用户注销
     * @param request
     * @return
     */
//    @AuthCheck(mustRole = "admin")
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request){
        boolean b = userApplicationService.userLogout(request);
        return ResultUtils.success(b);
    }

    /**
     * 添加用户（管理员）
     * @param userAddRequest
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest){
        //1.校验参数
        ThrowUtils.throwIf(userAddRequest == null,ErrorCode.PARAMS_ERROR);
        //2.将请求模型转换成实体模型
        User user = UserAssembler.toUserEntity(userAddRequest);
        return ResultUtils.success(userApplicationService.saveUser(user));
    }

    /**
     * 删除用户（管理员）
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest){
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0,ErrorCode.PARAMS_ERROR);
        boolean b = this.userApplicationService.deleteUser(deleteRequest);
        ThrowUtils.throwIf(!b,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(b);
    }

    /**
     * 更新用户（管理员）
     * @param userUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest){
        ThrowUtils.throwIf(userUpdateRequest == null,ErrorCode.PARAMS_ERROR);
        User user = UserAssembler.toUserEntity(userUpdateRequest);
        this.userApplicationService.updateUser(user);
        return ResultUtils.success(true);
    }

    /**

     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userApplicationService.getUserById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**

     * 根据 id 获取包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userApplicationService.getUserVO(user));
    }

    /**

     * 分页获取用户封装列表（仅管理员）
     *
     * @param userQueryRequest 查询请求参数
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success( userApplicationService.listUserVOByPage(userQueryRequest));
    }
}
