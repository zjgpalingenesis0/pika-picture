package com.zjg.pikapicture.model.dto.space;

import com.zjg.pikapicture.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理员更新空间请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 空间id
     */
    private Long id;

    /**
     * 空间名
     */
    private String spaceName;
    /**
     * 空间级别
     */
    private int spaceLevel;
    /**
     * 用户id
     */
    private Long userId;

    /**
     * 空间类型：0为私有空间 1为团队空间
     */
    private Integer spaceType;
}
