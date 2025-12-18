package com.zjg.pikapicture.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.util.Objects;

@Getter
public enum SpaceTypeEnum {

    PERSONAL("私人空间", 0),
    TEAM("团队空间", 1);

    private final String text;

    private final Integer value;

    SpaceTypeEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 通过value获取枚举类
     * @param value
     * @return
     */
    public static SpaceTypeEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (SpaceTypeEnum spaceTypeEnum : SpaceTypeEnum.values()) {
            if (Objects.equals(value, spaceTypeEnum.getValue())) {
                return spaceTypeEnum;
            }
        }
        return null;
    }
}
