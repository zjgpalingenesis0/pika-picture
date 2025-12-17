package com.zjg.pikapicture.model.dto.picture;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 批量导入图片请求
 */
@Data
public class PictureUploadByBatchRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 搜索词
     */
    private String searchText;
    /**
     * 抓取数量
     */
    private Integer count = 10;
    /**
     * 文件名字前缀
     */
    private String namePrefix;
}
