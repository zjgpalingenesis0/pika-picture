package com.zjg.pikapicture.model.dto.space;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户编辑空间请求
 */
@Data
public class SpaceEditRequest implements Serializable {

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

}
