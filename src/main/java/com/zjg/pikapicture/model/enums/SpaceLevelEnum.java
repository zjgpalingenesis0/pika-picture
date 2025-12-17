package com.zjg.pikapicture.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import static com.zjg.pikapicture.constant.SpaceConstant.ONE_MB;

/**
 * 空间级别枚举类
 */
@Getter
public enum SpaceLevelEnum {

    COMMON("普通版", 0, 50 * ONE_MB, 100L),
    PROFESSIONAL("专业版", 1, 200 * ONE_MB, 500L),
    FLAGSHIP("旗舰版", 2, 500 * ONE_MB, 1000L);


    private final String text;

    private final int value;

    private final Long maxSize;

    private final Long maxCount;

    SpaceLevelEnum(String text, int value, Long maxSize, Long maxCount) {
        this.text = text;
        this.value = value;
        this.maxSize = maxSize;
        this.maxCount = maxCount;
    }

    /**
     * 根据value取空间等级枚举
     *
     * @param value
     * @return
     */
    public static SpaceLevelEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (SpaceLevelEnum spaceLevelEnum : SpaceLevelEnum.values()) {
            if (spaceLevelEnum.getValue() == value) {
                return spaceLevelEnum;
            }
        }
        return null;
    }
}
