package com.zjg.pikapicture.model.dto.file;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UploadPictureResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 缩略图url
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;


}
