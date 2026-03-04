package com.zjg.pikapicture.manager.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 图片编辑请求信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PictureEditRequestMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息类型 ENTER_EDIT  EXIT_EDIT   EDIT_ACTION
     */
    private String messageType;

    /**
     * 编辑动作
     */
    private String editAction;

}
