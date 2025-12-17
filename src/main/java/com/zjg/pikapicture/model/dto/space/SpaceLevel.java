package com.zjg.pikapicture.model.dto.space;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class SpaceLevel implements Serializable {

    private String text;
    private Integer value;
    private Long maxCount;
    private Long maxSize;

}
