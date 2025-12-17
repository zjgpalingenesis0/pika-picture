package com.zjg.pikapicture.manager.cache_;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zjg.pikapicture.model.dto.picture.PictureQueryRequest;
import com.zjg.pikapicture.model.vo.PictureVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MultiLevelCache extends CacheTemplate {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)
            .maximumSize(10000L)
            //缓存5分钟后移除
            .expireAfterWrite(300, TimeUnit.SECONDS)
            .build();

    @Override
    public Page<PictureVO> readCache(PictureQueryRequest pictureQueryRequest) {
        try {
            // 构建缓存key
            String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
            String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
            String cacheKey = "listPictureVOPage: " + hashKey;

            // 先从本地缓存中查询
            String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
            if (cachedValue != null) {
                // 如果缓存命中，返回结果
                Page<PictureVO> pagePictureVO = JSONUtil.toBean(cachedValue, Page.class);
                log.info("本地缓存命中，key: {}", cacheKey);
                return pagePictureVO;
            }

            // 缓存未命中，查分布式缓存
            ValueOperations<String, String> opsValue = stringRedisTemplate.opsForValue();
            String redisKey = "pikapicture:listPictureVOPage" + hashKey;
            cachedValue = opsValue.get(redisKey);
            if (cachedValue != null) {
                //如果缓存命中，存入本地缓存，返回结果
                Page<PictureVO> pagePictureVO = JSONUtil.toBean(cachedValue, Page.class);
                LOCAL_CACHE.put(cacheKey, JSONUtil.toJsonStr(pagePictureVO));
                return pagePictureVO;
            }

            log.info("分布式缓存也未命中，key: {}", redisKey);
            return null;

        } catch (Exception e) {
            log.error("读取缓存时发生异常: {}", e.getMessage(), e);
            // 读缓存失败不影响主流程，返回空
            return null;
        }
    }

    @Override
    public void writeCache(Page<PictureVO> pictureVOPage, PictureQueryRequest pictureQueryRequest) {
        try {
            // 参数校验
            if (pictureVOPage == null || pictureQueryRequest == null) {
                log.warn("写入多级缓存参数不合法，pictureVOPage: {}, pictureQueryRequest: {}", pictureVOPage, pictureQueryRequest);
                return;
            }

            // 构建缓存key（与readCache保持一致）
            String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
            String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
            String cacheKey = "listPictureVOPage: " + hashKey;
            String redisKey = "pikapicture:listPictureVOPage" + hashKey;

            String cacheValue = JSONUtil.toJsonStr(pictureVOPage);

            // 1. 先写入本地缓存（L1缓存）
            LOCAL_CACHE.put(cacheKey, cacheValue);
            log.info("本地缓存写入成功，key: {}", cacheKey);

            // 2. 再写入Redis缓存（L2缓存）
            ValueOperations<String, String> opsValue = stringRedisTemplate.opsForValue();
            // 5-10分钟过期，防止雪崩
            int cacheExpireTime = 300 + (int) (Math.random() * 300);
            opsValue.set(redisKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
            log.info("Redis缓存写入成功，key: {}, 过期时间: {}秒", redisKey, cacheExpireTime);

        } catch (Exception e) {
            log.error("写入多级缓存时发生异常: {}", e.getMessage(), e);
            // 写入缓存失败不应该影响主流程
        }
    }

    @Override
    public Boolean deleteCache(String key) {
        try {
            // 参数校验
            if (key == null || key.trim().isEmpty()) {
                log.warn("删除多级缓存参数不合法，key: {}", key);
                return false;
            }

            // 构建缓存key（根据传入的key类型判断）
            String localCacheKey = key;
            String redisKey = key;

            // 如果传入的是查询条件，需要构建完整的key
            if (!key.startsWith("listPictureVOPage:") && !key.startsWith("pikapicture:listPictureVOPage")) {
                String hashKey = DigestUtils.md5DigestAsHex(key.getBytes());
                localCacheKey = "listPictureVOPage: " + hashKey;
                redisKey = "pikapicture:listPictureVOPage" + hashKey;
            }

            boolean allDeleted = true;

            // 1. 删除本地缓存（L1缓存）
            LOCAL_CACHE.invalidate(localCacheKey);
            log.info("本地缓存删除成功，key: {}", localCacheKey);

            // 2. 删除Redis缓存（L2缓存）
            Boolean redisDeleted = stringRedisTemplate.delete(redisKey);
            if (Boolean.TRUE.equals(redisDeleted)) {
                log.info("Redis缓存删除成功，key: {}", redisKey);
            } else {
                log.warn("Redis缓存删除失败或key不存在，key: {}", redisKey);
                allDeleted = false;
            }

            return allDeleted;

        } catch (Exception e) {
            log.error("删除多级缓存时发生异常，key: {}, 错误: {}", key, e.getMessage(), e);
            // 删除缓存失败不应该影响主流程，返回false
            return false;
        }
    }
}
