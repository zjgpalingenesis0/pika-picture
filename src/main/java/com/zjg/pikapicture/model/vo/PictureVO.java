package com.zjg.pikapicture.model.vo;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.zjg.pikapicture.model.entity.Picture;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class PictureVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

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
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（JSON 数组）
     */
    private List<String> tags;

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

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 所在空间id
     */
    private Long spaceId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;
    /**
     * 创建用户信息
     */
    private UserVO user;

    /**
     * 封装类转对象
     *
     * @param pictureVO
     * @return
     */
    public static Picture voToObj(PictureVO pictureVO) {
//        1. 判断a是否为空，返回空
        if (pictureVO == null) {
            return null;
        }
//        2. 创建b类对象
        Picture picture = new Picture();
//        3. 复制a到b
        BeanUtils.copyProperties(pictureVO, picture);
//        4. 类型不同，需要转换（这里就是tags）
        picture.setTags(JSONUtil.toJsonStr(pictureVO.getTags()));
//        5. 返回
        return picture;
    }

    /**
     * 对象转封装类
     *
     * @param picture
     * @return
     */
    public static PictureVO objToVo(Picture picture) {
//        1. 判断a是否为空，返回空
        if (picture == null) {
            return null;
        }
//        2. 创建b类对象
        PictureVO pictureVO = new PictureVO();
//        3. 复制a到b
        BeanUtils.copyProperties(picture, pictureVO);
//        4. 类型不同，需要转换（这里就是tags）
        pictureVO.setTags(JSONUtil.toList(picture.getTags(), String.class));
//        5. 返回
        return pictureVO;
    }
}
