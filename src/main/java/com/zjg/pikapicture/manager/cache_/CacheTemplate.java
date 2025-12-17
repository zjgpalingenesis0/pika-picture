package com.zjg.pikapicture.manager.cache_;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjg.pikapicture.model.dto.picture.PictureQueryRequest;

import com.zjg.pikapicture.model.vo.PictureVO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class CacheTemplate {
    /**
     * 从缓存中读
     *
     * @param pictureQueryRequest
     * @return
     */
    public abstract Page<PictureVO> readCache(PictureQueryRequest pictureQueryRequest);


    /**
     * 缓存写
     *
     * @param pictureVOPage
     * @param pictureQueryRequest
     */
    public abstract void writeCache(Page<PictureVO> pictureVOPage, PictureQueryRequest pictureQueryRequest);

    /**
     * 删除缓存
     *
     * @param redisKey
     * @return
     */
    public abstract Boolean deleteCache(String redisKey);
}
