package com.zjh.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import com.zjh.manager.auth.SpaceUserAuthManager;
import com.zjh.manager.auth.model.SpaceUserPermissionConstant;
import com.zjh.model.entity.Picture;
import com.zjh.model.entity.Space;
import com.zjh.model.entity.User;
import com.zjh.model.enums.SpaceTypeEnum;
import com.zjh.service.PictureService;
import com.zjh.service.SpaceService;
import com.zjh.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * @author zjh
 * @version 1.0
 * websocket拦截器，进行权限校验
 * 校验有没有该空间编辑权限，有的话就允许建立会话
 */
@Component
@Slf4j
public class WsHandshakeInterceptor implements HandshakeInterceptor {
    @Resource
    private PictureService pictureService;
    @Resource
    private UserService userService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     *在请求之前进行权限校验
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            // TODO: 校验权限
            String pictureId = httpRequest.getParameter("pictureId");
            if (pictureId == null) {
                log.error("缺少图片信息 拒绝握手");
                return false;
            }
            //用户未登录，不允许进行会话
            User loginUser = userService.getLoginUser(httpRequest);
            if (ObjUtil.isEmpty(loginUser)) {
                log.error("用户未登录 拒绝握手");
                return false;
            }
            //根据图片id找到对应空间，只有团队空间才可以进行websocket会话
            Picture picture = pictureService.getById(pictureId);
            if (picture == null) {
                log.error("图片不存在 拒绝握手");
                return false;
            }
            Long spaceId = picture.getSpaceId();
            Space space = null;
            if (spaceId != null) {
                space = spaceService.getById(spaceId);
                if (space == null) {
                    log.error("空间不存在 拒绝握手");
                    return false;
                }
                if (space.getSpaceType() != SpaceTypeEnum.TEAM.getValue()) {
                    log.error("非团队空间 拒绝握手");
                    return false;
                }
            }
            //对登录人和要编辑的团队空间校验该登录人是否有编辑权限
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
                log.error("没有编辑权限 拒绝握手");
                return false;
            }
            //允许建立会话，将基本属性添加到会话中
            attributes.put("pictureId", Long.valueOf(pictureId));
            attributes.put("user",loginUser);
            attributes.put("userId", loginUser.getId());
        }
        return true;

    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
