package com.zjg.pikapicture.model.enums;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;

@Getter
public enum PictureReviewStatusEnum {

    REVIEWING(0, "待审核"),
    PASS(1, "通过"),
    REJECT(2, "拒绝");

    private final String text;

    private final int value;

    PictureReviewStatusEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据value获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static PictureReviewStatusEnum getEnumByVaule(int value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (PictureReviewStatusEnum item : PictureReviewStatusEnum.values()) {
            if (item.value == value) {
                return item;
            }
        }
        return null;
    }
}
