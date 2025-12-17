package com.zjg.pikapicture.model.dto.space;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理员更新空间请求
 */
@Data
public class SpaceUpdateRequest implements Serializable {

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
     * 空间容量
     */
    private Long spaceSize;
    /**
     * 空间数量
     */
    private Long spaceCount;
}
