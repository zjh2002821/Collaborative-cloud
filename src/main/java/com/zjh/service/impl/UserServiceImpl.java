package com.zjh.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjh.constant.UserConstant;
import com.zjh.exception.BusinessException;
import com.zjh.exception.ErrorCode;
import com.zjh.exception.ThrowUtils;
import com.zjh.model.dto.user.UserQueryRequest;
import com.zjh.model.enums.UserRoleEnum;
import com.zjh.model.vo.LoginUserVO;
import com.zjh.model.vo.UserVO;
import com.zjh.service.UserService;
import com.zjh.model.entity.User;
import com.zjh.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author zjh20
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2024-12-25 22:35:47
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        //1.校验
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount,userPassword,checkPassword),"参数为空", ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userAccount.length() < 4,"用户账号过短", ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userPassword.length() < 4,"用户密码过短", ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(!checkPassword.equals(userPassword),"两次密码不一致", ErrorCode.PARAMS_ERROR);
        //2.检验该账户是否存在
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount",userAccount);
        Long l = baseMapper.selectCount(userQueryWrapper);
        ThrowUtils.throwIf(l > 0,"该用户已存在",ErrorCode.PARAMS_ERROR);
        //3.加密
        String encryptPassword = getEncryptPassword(userPassword);
        //4.插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("hello!");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean save = this.save(user);
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
        //1.校验账户和密码是否正确
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount,userPassword),"参数为空", ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userAccount.length() < 4,"用户账号过短", ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userPassword.length() < 4,"用户密码过短", ErrorCode.PARAMS_ERROR);
        //2.密码加密
        String encryptPassword = getEncryptPassword(userPassword);
        //3.查询该用户是否存在
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount",userAccount);
        userQueryWrapper.eq("userPassword",encryptPassword);
        User user = baseMapper.selectOne(userQueryWrapper);
        //4.用户不存在
        if (user == null){
            log.info("Check user is null");
            ThrowUtils.throwIf(user == null,"该用户不存在或密码错误",ErrorCode.PARAMS_ERROR);
        }
        //5.记录用户登录态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE,user);
        return getLoginUserVO(user);
    }

    /**
     *
     * @param request
     * @return
     */
    @Override
    public LoginUserVO getLoginUser(HttpServletRequest request) {
        //1.获取session判断是否已经登录
        Object attribute = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User user = (User) attribute;
        ThrowUtils.throwIf(user == null || user.getId() == null,ErrorCode.NOT_LOGIN_ERROR);
        //2.脱敏数据
        return getLoginUserVO(user);
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
    private static String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "zjh";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    /**
     * 将user信息脱敏
     * @param user
     * @return
     */
    private static LoginUserVO getLoginUserVO(User user){
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



}




