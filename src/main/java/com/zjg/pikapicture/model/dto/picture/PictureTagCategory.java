package com.zjg.pikapicture.model.dto.picture;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class PictureTagCategory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 标签列表
     */
    private List<String> tagList;
    /**
     * 目录列表
     */
    private List<String> categoryList;
}
