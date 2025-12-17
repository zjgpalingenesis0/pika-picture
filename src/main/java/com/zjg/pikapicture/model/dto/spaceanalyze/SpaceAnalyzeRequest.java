package com.zjg.pikapicture.model.dto.spaceanalyze;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通用的空间分析请求封装类
 */
@Data
public class SpaceAnalyzeRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 是否全空间分析
     */
    private boolean queryAll;

    /**
     * 是否查询公共图库
     */
    private boolean queryPublic;

}
