package com.zjg.pikapicture.manager.auth.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class SpaceUserAuthConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 角色列表
     */
    private List<SpaceUserRole> roles;
    /**
     * 权限列表
     */
    private List<SpaceUserPermission> permissions;


}
