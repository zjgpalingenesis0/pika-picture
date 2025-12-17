package com.zjg.pikapicture.model.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

import static com.zjg.pikapicture.constant.UserConstant.ADMIN_ROLE;
import static com.zjg.pikapicture.constant.UserConstant.DEFAULT_ROLE;

@Getter
public enum UserRoleEnum {

    ADMIN("管理员", ADMIN_ROLE),
    USER("用户", DEFAULT_ROLE);

    private final String text;

    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据value获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static UserRoleEnum getEnumByVaule(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        for (UserRoleEnum item : UserRoleEnum.values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }
}
