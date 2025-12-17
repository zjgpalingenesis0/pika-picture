package com.zjg.pikapicture.model.dto.spaceanalyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceUserAnalyzeRequest extends SpaceAnalyzeRequest {
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 时间维度  day-week-month
     */
    private String timeDimension;

}
