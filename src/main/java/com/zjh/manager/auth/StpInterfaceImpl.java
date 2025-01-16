package com.zjh.manager.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.zjh.exception.BusinessException;
import com.zjh.exception.ErrorCode;
import com.zjh.manager.auth.model.SpaceUserAuthContext;
import com.zjh.manager.auth.model.SpaceUserPermissionConstant;
import com.zjh.model.entity.Picture;
import com.zjh.model.entity.Space;
import com.zjh.model.entity.SpaceUser;
import com.zjh.model.entity.User;
import com.zjh.model.enums.SpaceRoleEnum;
import com.zjh.model.enums.SpaceTypeEnum;
import com.zjh.service.PictureService;
import com.zjh.service.SpaceService;
import com.zjh.service.SpaceUserService;
import com.zjh.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.zjh.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author zjh
 * @version 1.0
 */
public class StpInterfaceImpl implements StpInterface {
    @Value("${server.servlet.context-path}")
    private String contextPath;//默认为/api/picture/xxx
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;
    @Resource
    private UserService userService;
    @Resource
    private PictureService pictureService;
    @Resource
    private SpaceUserService spaceUserService;
    @Resource
    private SpaceService spaceService;

    /**
     * 从请求url中获取上下文对象
     * @return
     */
    public SpaceUserAuthContext getAuthContextByRequest(){
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes()).getRequest();
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        SpaceUserAuthContext authContext;
        //兼容get/post操作
        if (ContentType.JSON.getValue().equals(contentType)){//如果是post，就将json数据转化成权限上下文类对象
            String body = ServletUtil.getBody(request);
            authContext = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        }else { //如果为get，就将请求参数封装到map中，在转化成权限上下文类对象
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            authContext = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        //根据请求路径区分id含义
        Long id = authContext.getId();
        if (ObjUtil.isNotNull(id)){
            //获取url路径
            String requestURI = request.getRequestURI();
            //2.截取/api/设置为空
            String replace = requestURI.replace(contextPath + "/", "");
            //3.截取第一个“/”之前的值
            String moduleName = StrUtil.subBefore(replace, "/", false);
            switch (moduleName){
                case "picture":
                    authContext.setPictureId(id);
                    break;
                case "spaceUser":
                    authContext.setSpaceUserId(id);
                    break;
                case "space":
                    authContext.setSpaceId(id);
                    break;
                default:
            }
        }
        return authContext;
    }

    /**
     * 根据url参数，最终要找到这个账号在spaceUser表中的对应空间的权限
     * 首先看spaceUser是否存在，如果存在则直接获取该账号在这个团队空间的权限
     * 如果首先看spaceUser不存在，就看spaceUserId是否存在，如果存在，直接找到这个spaceUser，获取该账号在这个团队空间的权限
     * 如果不存在，尝试通过 spaceId 或 pictureId 获取 Space 对象，配合userid找到该账号在团队空间的权限
     * 注意：如果spaceId为空，就看pictureId是否为空，如果都为空，则为公共空间，只需要校验是否是本人或者是管理员，如果是就返回最高权限
     * 如果不是空，就找到这个图片对象，如果图片对象的spaceId为空，则是公共空间，返回最高权限
     * 然后如果spaceId不为空则需要根据是私人空间还是团队空间判断
     * @param loginId
     * @param loginType
     * @return
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 判断 loginType，仅对类型为 "space" 进行权限校验
        if (!StpKit.SPACE_TYPE.equals(loginType)) {
            return new ArrayList<>();
        }
        // 管理员权限，表示权限校验通过
        List<String> ADMIN_PERMISSIONS = spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 获取上下文对象
        SpaceUserAuthContext authContext = getAuthContextByRequest();
        // 如果所有字段都为空，表示查询公共图库，可以通过
        if (isAllFieldsNull(authContext)) {
            return ADMIN_PERMISSIONS;
        }
        // 获取 userId
        User loginUser = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户未登录");
        }
        Long userId = loginUser.getId();
        // 优先从上下文中获取 SpaceUser 对象
        SpaceUser spaceUser = authContext.getSpaceUser();
        if (spaceUser != null) {
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
        // 如果有 spaceUserId，必然是团队空间，通过数据库查询 SpaceUser 对象
        Long spaceUserId = authContext.getSpaceUserId();
        if (spaceUserId != null) {
            spaceUser = spaceUserService.getById(spaceUserId);
            if (spaceUser == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间用户信息");
            }
            // 取出当前登录用户对应的 spaceUser
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            if (loginSpaceUser == null) {
                return new ArrayList<>();
            }
            // 这里会导致管理员在私有空间没有权限，可以再查一次库处理
            return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
        }
        // 如果没有 spaceUserId，尝试通过 spaceId 或 pictureId 获取 Space 对象并处理
        Long spaceId = authContext.getSpaceId();
        if (spaceId == null) {
            // 如果没有 spaceId，通过 pictureId 获取 Picture 对象和 Space 对象
            Long pictureId = authContext.getPictureId();
            // 图片 id 也没有，则默认通过权限校验
            if (pictureId == null) {
                return ADMIN_PERMISSIONS;
            }
            Picture picture = pictureService.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .select(Picture::getId, Picture::getSpaceId, Picture::getUserId)
                    .one();
            if (picture == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到图片信息");
            }
            spaceId = picture.getSpaceId();
            // 公共图库，仅本人或管理员可操作
            if (spaceId == null) {
                if (picture.getUserId().equals(userId) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    // 不是自己的图片，仅可查看
                    return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
                }
            }
        }
        // 获取 Space 对象
        Space space = spaceService.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间信息");
        }
        // 根据 Space 类型判断权限
        if (space.getSpaceType() == SpaceTypeEnum.PRIVATE.getValue()) {
            // 私有空间，仅本人或管理员有权限
            if (space.getUserId().equals(userId) || userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            } else {
                return new ArrayList<>();
            }
        } else {
            // 团队空间，查询 SpaceUser 并获取角色和权限
            spaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            if (spaceUser == null) {
                return new ArrayList<>();
            }
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
    }


    @Override
    public List<String> getRoleList(Object o, String s) {
        return null;
    }

    /**
     *校验所有字段是否为空
     * @param object
     * @return
     */
    private boolean isAllFieldsNull(Object object) {
        if (object == null) {
            return true; // 对象本身为空
        }
        // 获取所有字段并判断是否所有字段都为空
        return Arrays.stream(ReflectUtil.getFields(object.getClass()))
                // 获取字段值
                .map(field -> ReflectUtil.getFieldValue(object, field))
                // 检查是否所有字段都为空
                .allMatch(ObjectUtil::isEmpty);
    }

}
