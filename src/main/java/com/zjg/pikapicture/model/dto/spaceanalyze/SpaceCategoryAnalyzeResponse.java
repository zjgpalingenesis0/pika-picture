package com.zjg.pikapicture.model.dto.spaceanalyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceCategoryAnalyzeResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 图片分类
     */
    private String category;
    /**
     * 分类图片数量
     */
    private Long count;
    /**
     * 分类图片总大小
     */
    private Long totalSize;
}
