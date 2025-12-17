package com.zjg.pikapicture.manager.cache_;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zjg.pikapicture.model.dto.picture.PictureQueryRequest;
import com.zjg.pikapicture.model.vo.PictureVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CaffeineCache extends CacheTemplate {

    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)
            .maximumSize(10000L)
            //缓存5分钟后移除
            .expireAfterWrite(300, TimeUnit.SECONDS)
            .build();

    @Override
    public Page<PictureVO> readCache(PictureQueryRequest pictureQueryRequest) {
        try {
            // 构建缓存key（与RedisCache保持一致）
            String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
            String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
            String cacheKey = "listPictureVOPage: " + hashKey;

            // 从本地缓存中查询
            String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
            if (cachedValue != null) {
                // 如果缓存命中，返回结果
                Page<PictureVO> pagePictureVO = JSONUtil.toBean(cachedValue, Page.class);
                log.info("本地缓存命中，key: {}", cacheKey);
                return pagePictureVO;
            }

            // 缓存未命中
            log.info("本地缓存未命中，key: {}", cacheKey);
            return null;

        } catch (Exception e) {
            log.error("读取本地缓存时发生异常: {}", e.getMessage(), e);
            // 读缓存失败不影响主流程，返回空
            return null;
        }
    }

    @Override
    public void writeCache(Page<PictureVO> pictureVOPage, PictureQueryRequest pictureQueryRequest) {
        try {
            // 参数校验
            if (pictureVOPage == null || pictureQueryRequest == null) {
                log.warn("写入本地缓存参数不合法，pictureVOPage: {}, pictureQueryRequest: {}", pictureVOPage, pictureQueryRequest);
                return;
            }

            // 构建缓存key（与readCache保持一致）
            String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
            String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
            String cacheKey = "listPictureVOPage: " + hashKey;

            // 存入本地缓存
            String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
            LOCAL_CACHE.put(cacheKey, cacheValue);

            log.info("本地缓存写入成功，key: {}", cacheKey);

        } catch (Exception e) {
            log.error("写入本地缓存时发生异常: {}", e.getMessage(), e);
            // 写入缓存失败不应该影响主流程
        }
    }

    @Override
    public Boolean deleteCache(String cacheKey) {
        try {
            // 参数校验
            if (cacheKey == null || cacheKey.trim().isEmpty()) {
                log.warn("删除本地缓存参数不合法，cacheKey: {}", cacheKey);
                return false;
            }

            // 删除本地缓存
            LOCAL_CACHE.invalidate(cacheKey);

            log.info("本地缓存删除成功，key: {}", cacheKey);
            return true;

        } catch (Exception e) {
            log.error("删除本地缓存时发生异常，key: {}, 错误: {}", cacheKey, e.getMessage(), e);
            // 删除缓存失败不应该影响主流程，返回false
            return false;
        }
    }
}
