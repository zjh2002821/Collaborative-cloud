package com.zjh.infrastructure.aop;

import com.zjh.infrastructure.annotation.AuthCheck;
import com.zjh.infrastructure.exception.BusinessException;
import com.zjh.infrastructure.exception.ErrorCode;
import com.zjh.infrastructure.exception.ThrowUtils;
import com.zjh.domain.user.entity.User;
import com.zjh.domain.user.valueobject.UserRoleEnum;
import com.zjh.application.service.UserApplicationService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @author zjh
 * @version 1.0
 * aop权限校验代理
 */
@Aspect//定义该类为切面
@Component//将该类注入bean容器中
public class AuthInterceptor {
    @Resource
    private UserApplicationService userApplicationService;

    /**
     *
     * @param pjp 切入点
     * @param authCheck 权限校验注解
     * @return
     * @throws Throwable
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint pjp, AuthCheck authCheck) throws Throwable {
        //1.获取该注解必须存在的权限值
        String mustRoleValue = authCheck.mustRole();
        //2.全局请求上下文，获取当前请求
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
            //向下转型从 ServletRequestAttributes 对象中获取具体的 HttpServletRequest 对象
        HttpServletRequest request = ((ServletRequestAttributes)requestAttributes).getRequest();
        //3.调用user业务方法获取登录用户信息,拿到该用户的角色枚举类型
        User loginUser = userApplicationService.getLoginUser(request);
        String userRoleValue = loginUser.getUserRole();
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(userRoleValue);
        //4.拿到注解中定义的枚举类型
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRoleValue);
        //5.如果该注解为默认权限（默认权限为""），不需要权限那么就放行
        if (mustRoleEnum == null){
            return pjp.proceed();
        }
        //6.以下为：必须具有该权限才可以通过
            //没有权限，拒绝
        ThrowUtils.throwIf(userRoleEnum == null, ErrorCode.NO_AUTH_ERROR);
            //必须要有管理员权限，但用户没有管理员权限
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //7.通过权限校验，放行
        return pjp.proceed();

    }
}
