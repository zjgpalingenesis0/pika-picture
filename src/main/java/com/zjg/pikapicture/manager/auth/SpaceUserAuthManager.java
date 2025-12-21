package com.zjg.pikapicture.manager.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.zjg.pikapicture.manager.auth.model.SpaceUserAuthConfig;
import com.zjg.pikapicture.manager.auth.model.SpaceUserRole;
import com.zjg.pikapicture.model.entity.Space;
import com.zjg.pikapicture.model.entity.SpaceUser;
import com.zjg.pikapicture.model.entity.User;
import com.zjg.pikapicture.model.enums.SpaceRoleEnum;
import com.zjg.pikapicture.model.enums.SpaceTypeEnum;
import com.zjg.pikapicture.service.SpaceUserService;
import com.zjg.pikapicture.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.SameLen;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 空间成员权限管理类
 */
@Component
@Slf4j
public class SpaceUserAuthManager {

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private UserService userService;

    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static {
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
    }

    /**
     * 根据角色获取权限列表
     * @param spaceUserRole
     * @return
     */
    public List<String> getPermissionByRole(String spaceUserRole){
        //校验
        if (StrUtil.isBlank(spaceUserRole)){
            return new ArrayList<>();
        }
        //找到匹配角色
        SpaceUserRole role = SPACE_USER_AUTH_CONFIG.getRoles().stream()
                .filter(r -> spaceUserRole.equals(r.getKey()))
                .findFirst()
                .orElse(null);
        if (role == null) {
            return new ArrayList<>();
        }
        return role.getPermissions();
    }

    /**
     * 获取权限列表
     * @param space
     * @param loginUser
     * @return
     */
    public List<String> getPermissionList(Space space, User loginUser) {
        if (loginUser == null) {
            return new ArrayList<>();
        }
        //获取管理员权限
        List<String> ADMIN_PERMISSIONS = this.getPermissionByRole(SpaceRoleEnum.ADMIN.getValue());

        //公共图库
        if (space == null) {
            if (userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            }
            else {
                return new ArrayList<>();
            }
        }

        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
        if (spaceTypeEnum == null) {
            return new ArrayList<>();
        }
        //根据空间获取对应权限
        switch (spaceTypeEnum) {
            case PERSONAL:
                if (space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                }
                else {
                    return new ArrayList<>();
                }
            case TEAM:
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .one();
                if (spaceUser == null) {
                    return new ArrayList<>();
                }
                else {
                    return getPermissionByRole(spaceUser.getSpaceRole());
                }
        }
        return new ArrayList<>();
    }

}
