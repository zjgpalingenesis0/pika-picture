package com.zjg.pikapicture.manager.websocket.model;

import com.zjg.pikapicture.model.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 图片编辑响应信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PictureEditResponseMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息类型  INFO  ERROR  ENTER_EDIT  EXIT_EDIT EDIT_ACTION
     */
    private String messageType;

    /**
     * 信息
     */
    private String message;

    /**
     * 编辑动作
     */
    private String editAction;

    /**
     * 用户信息
     */
    private UserVO user;


}
