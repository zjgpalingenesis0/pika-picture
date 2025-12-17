package com.zjg.pikapicture.model.dto.spaceanalyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceUserAnalyzeResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 时间范围
     */
    private String timeRange;
    /**
     * 图片数量
     */
    private Long count;

}
