package com.zjg.pikapicture.model.dto.spaceuser;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 编辑空间成员请求
 */
@Data
public class SpaceUserEditRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;


    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;
}
