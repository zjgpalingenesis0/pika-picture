package com.zjg.pikapicture.manager.auth;

import cn.dev33.satoken.stp.StpInterface;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.zjg.pikapicture.exception.BusinessException;
import com.zjg.pikapicture.exception.ErrorCode;
import com.zjg.pikapicture.manager.auth.model.SpaceUserAuthContext;

import com.zjg.pikapicture.manager.auth.model.SpaceUserPermissionConstant;
import com.zjg.pikapicture.model.entity.Picture;
import com.zjg.pikapicture.model.entity.Space;
import com.zjg.pikapicture.model.entity.SpaceUser;
import com.zjg.pikapicture.model.entity.User;
import com.zjg.pikapicture.model.enums.SpaceRoleEnum;
import com.zjg.pikapicture.model.enums.SpaceTypeEnum;
import com.zjg.pikapicture.service.PictureService;
import com.zjg.pikapicture.service.SpaceService;
import com.zjg.pikapicture.service.SpaceUserService;
import com.zjg.pikapicture.service.UserService;
import jakarta.annotation.Resource;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static cn.hutool.core.collection.CollUtil.allMatch;
import static com.zjg.pikapicture.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 自定义权限加载接口实现类
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    @Value("{server.servlet.context-path}")
    private String contextPath;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    /**
     * 返回一个账号所拥有的权限码集合 
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        //判断loginType，只对space的校验
        if (!StpKit.SPACE_TYPE.equals(loginType)) {
            return new ArrayList<>();
        }
        //管理员权限处理
        String spaceRole = SpaceRoleEnum.ADMIN.getValue();
        List<String> ADMIN_PERMISSIONS = spaceUserAuthManager.getPermissionByRole(spaceRole);
        //获取上下文对象
        SpaceUserAuthContext authRequest = getAuthContextByRequest();
        if (isAllFieldsNull(authRequest)) {
            //如果所有字段为空，视为公共图库操作，给管理员权限
            return ADMIN_PERMISSIONS;
        }
        //检验登录状态
        User loginUser = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户未登录");
        }
        Long userId = loginUser.getId();
        //优先从上下文获取spaceUser对象
        SpaceUser spaceUser = authRequest.getSpaceUser();
        if (spaceUser != null) {
            return spaceUserAuthManager.getPermissionByRole(spaceUser.getSpaceRole());
        }
        //如果存在spaceUserId,一定是团队空间
        Long spaceUserId = authRequest.getSpaceUserId();
        if (spaceUserId != null) {
            spaceUser = spaceUserService.getById(spaceUserId);
            if (spaceUser == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间用户信息");
            }
            //取出当前登录用户对应的spaceUser
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            if (loginSpaceUser == null) {
                return new ArrayList<>();
            }
            return spaceUserAuthManager.getPermissionByRole(loginSpaceUser.getSpaceRole());
        }
        //没有spaceUserId，用spaceId或pictureId
        Long spaceId = authRequest.getSpaceId();
        if (spaceId == null) {
            //如果 `spaceId` 不存在：使用 `pictureId` 查询图片信息
            Long pictureId = authRequest.getPictureId();
            if (pictureId == null) {
                return ADMIN_PERMISSIONS;
            }
            Picture picture = pictureService.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .select(Picture::getId, Picture::getUserId, Picture::getSpaceId)
                    .one();
            if (picture == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到图片信息");
            }
            spaceId = picture.getSpaceId();
            //公共图库，仅管理员+本人操作
            if (spaceId == null) {
                if (userService.isAdmin(loginUser) || picture.getUserId().equals(userId)) {
                    return ADMIN_PERMISSIONS;
                }
                else {
                    //不是自己的图片只可以查看
                    return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
                }
            }
        }
        //获取space对象
        Space space = spaceService.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间信息");
        }
        //根据space类型判断权限
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
        if (SpaceTypeEnum.PERSONAL.getValue().equals(spaceType)) {
            //私有空间
            if (userService.isAdmin(loginUser) || space.getUserId().equals(userId)) {
                return ADMIN_PERMISSIONS;
            }
            else {
                return new ArrayList<>();
            }
        }
        //团队空间
        else {
            spaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            if (spaceUser == null) {
                return new ArrayList<>();
            }
            return spaceUserAuthManager.getPermissionByRole(spaceUser.getSpaceRole());
        }
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {

        return new ArrayList<>();
    }

    /**
     * 通过访问地址来决定应该给上下文传递哪些字段
     * @return
     */
    private SpaceUserAuthContext getAuthContextByRequest() {
        //获取请求对象
        HttpServletRequest request = (HttpServletRequest) ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        //获取请求类别
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        SpaceUserAuthContext authRequest;
        //兼容get，post操作
        if (ContentType.JSON.getValue().equals(contentType)) {
            String body = JakartaServletUtil.getBody(request);

            authRequest = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        }
        else {
            Map<String, String> paramMap = JakartaServletUtil.getParamMap(request);

            authRequest = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        //根据请求路径区分id字段的含义
        Long id = authRequest.getId();
        if (ObjUtil.isNotNull(id)) {
            String requestURI = request.getRequestURI();
            String partUri = requestURI.replace(contextPath + "/", "");
            String moduleName = StrUtil.subBefore(partUri, "/", false);
            switch(moduleName) {
                case "picture":
                    authRequest.setPictureId(id);
                    break;
                case "space":
                    authRequest.setSpaceId(id);
                    break;
                case "spaceUser":
                    authRequest.setSpaceUserId(id);
                    break;
                default:
            }

        }
        return authRequest;

    }

    /**
     * 判断所有字段是否为空
     * @param object
     * @return
     */
    private boolean isAllFieldsNull(Object object) {
        if (object == null) {
            return true;// 对象本身为空
        }
        //获取所有字段并检查是否每个字段为空
        return Arrays.stream(ReflectUtil.getFields(object.getClass()))
                .map(field -> ReflectUtil.getFieldValue(object, field))  // 获取字段值
                .allMatch(ObjUtil::isEmpty);   //检查是否为空
    }

}