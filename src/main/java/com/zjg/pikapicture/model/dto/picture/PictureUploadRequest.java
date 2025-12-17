package com.zjg.pikapicture.model.dto.picture;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 图片id
     */
    private Long id;
    /**
     * 空间id
     */
    private Long spaceId;
    /**
     * 图片地址
     */
    private String fileUrl;
    /**
     * 图片名称
     */
    private String picName;
}
