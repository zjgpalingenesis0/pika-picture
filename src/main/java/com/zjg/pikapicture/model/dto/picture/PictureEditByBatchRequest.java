package com.zjg.pikapicture.model.dto.picture;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class PictureEditByBatchRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<Long> pictureIdList;
    /**
     * 空间id
     */
    private Long spaceId;
    /**
     * 分类
     */
    private String category;
    /**
     * 标签
     */
    private List<String> tags;
    /**
     * 命名规则
     */
    private String nameRule;

}
