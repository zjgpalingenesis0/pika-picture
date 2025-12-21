package com.zjg.pikapicture.manager.auth;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

/**
 * 定义空间账号体系
 */
@Component
public class StpKit {

    public static final String SPACE_TYPE = "space";
    /**
     * 默认原生会话对象，本项目没使用
     */
    public static final StpLogic DEFAULT = StpUtil.stpLogic;
    /**
     * space会话对象，管理space表所有的账号登录，权限认证
     */
    public static final StpLogic SPACE = new StpLogic(SPACE_TYPE);

}
