package com.zjg.pikapicture.model.dto.spaceanalyze;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class SpaceUsageAnalyzeResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 已使用大小
     */
    private Long usedSize;
    /**
     * 最大大小
     */
    private Long maxSize;
    /**
     * 空间大小使用比例
     */
    private Double sizeUsageRatio;
    /**
     * 已使用图片数量
     */
    private Long usedCount;
    /**
     * 最大图片数量
     */
    private Long maxCount;
    /**
     * 图片数量占比
     */
    private Double countUsageRatio;
}
