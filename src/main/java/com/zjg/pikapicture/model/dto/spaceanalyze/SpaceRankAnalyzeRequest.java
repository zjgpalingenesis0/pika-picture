package com.zjg.pikapicture.model.dto.spaceanalyze;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 空间使用量排行榜请求类
 */
@Data
public class SpaceRankAnalyzeRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 空间id
     */
    private Integer topN = 10;



}
