package com.zjh;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.zjh.mapper")
@ComponentScan("com.zjh.*")
@EnableAspectJAutoProxy(exposeProxy = true)//开启aop代理
public class CollaborativeCloudApplication {

    public static void main(String[] args) {
        SpringApplication.run(CollaborativeCloudApplication.class, args);
    }

}
