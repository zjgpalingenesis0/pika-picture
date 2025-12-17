package com.zjg.pikapicture.model.dto.picture;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class PictureReviewRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 、
     * 图片id
     */
    private Long id;
    /**
     * 审核状态
     */
    private Integer reviewStatus;
    /**
     * 审核信息
     */
    private String reviewMessage;

}
