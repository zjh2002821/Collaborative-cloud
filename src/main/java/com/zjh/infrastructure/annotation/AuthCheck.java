package com.zjh.infrastructure.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author zjh
 * @version 1.0
 * 权限校验注解
 */
@Target(ElementType.METHOD)//限制该注解的作用域
@Retention(RetentionPolicy.RUNTIME)//在运行时也通过反射机制可访问。
public @interface AuthCheck {
    /**
     * 必须有某一个角色
     * @return
     */
    String mustRole() default "";
}
