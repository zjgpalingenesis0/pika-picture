package com.zjg.pikapicture.model.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum SpaceRoleEnum {

    VIEWER("浏览者", "viewer"),
    EDITOR("编辑者", "editor"),
    ADMIN("管理员", "admin");

    private final String text;

    private final String value;

    SpaceRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据value获取枚举值
     *
     * @param value
     * @return
     */
    public static SpaceRoleEnum getEnumByValue(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        for (SpaceRoleEnum spaceRoleEnum : SpaceRoleEnum.values()) {
            if (value.equals(spaceRoleEnum.getValue())) {
                return spaceRoleEnum;
            }
        }
        return null;
    }

    /**
     * 获取所有枚举的值列表
     *
     * @return
     */
    public static List<String> getAllValues() {
//        List<String> allValues = new ArrayList<>();
//        for (SpaceRoleEnum spaceRoleEnum : SpaceRoleEnum.values()) {
//            allValues.add(spaceRoleEnum.getValue());
//        }
//        return allValues;

        return Arrays.stream(SpaceRoleEnum.values())
                .map(SpaceRoleEnum::getValue)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有枚举的文本列表
     *
     * @return
     */
    public static List<String> getAllTexts() {
//        List<String> allTexts = new ArrayList<>();
//        for (SpaceRoleEnum spaceRoleEnum : SpaceRoleEnum.values()) {
//            allTexts.add(spaceRoleEnum.getText());
//        }
//        return allTexts;

        return Arrays.stream(SpaceRoleEnum.values())
                .map(SpaceRoleEnum::getText)
                .collect(Collectors.toList());
    }

}
