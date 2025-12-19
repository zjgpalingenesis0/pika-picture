package com.zjg.pikapicture.model.dto.spaceuser;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 查询空间成员请求
 */
@Data
public class SpaceUserQueryRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;


    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;
}
