package com.zjg.pikapicture.model.dto.picture;

import com.zjg.pikapicture.api.aliyunai.model.CreateOutPaintingTaskRequest;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class CreatePictureOutPaintingTaskRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 图片id
     */
    private Long pictureId;
    /**
     * 扩图参数
     */
    private CreateOutPaintingTaskRequest.Parameters parameters;
}
