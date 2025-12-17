package com.zjg.pikapicture.model.dto.space;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建空间请求
 */
@Data
public class SpaceAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 空间名
     */
    private String spaceName;
    /**
     * 空间级别
     */
    private int spaceLevel;
}
