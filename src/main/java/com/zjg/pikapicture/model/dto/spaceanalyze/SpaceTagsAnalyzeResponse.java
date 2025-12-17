package com.zjg.pikapicture.model.dto.spaceanalyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceTagsAnalyzeResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 图片标签
     */
    private String tags;
    /**
     * 该标签图片数量（使用次数）
     */
    private Long count;

}
