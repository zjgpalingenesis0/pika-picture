package com.zjg.pikapicture.manager.cache_;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
public class RedisCache extends CacheTemplate {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Page<PictureVO> readCache(PictureQueryRequest pictureQueryRequest) {
        String redisKey = null;

        try {
            //使用缓存
            //构建缓存key
            String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
            String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
            redisKey = "pikapicture:listPictureVOPage" + hashKey;
            //从redis缓存中查询
            ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
            String cachedValue = valueOps.get(redisKey);
            if (cachedValue != null) {
                //如果缓存命中，返回结果
                Page<PictureVO> pagePictureVO = JSONUtil.toBean(cachedValue, Page.class);
                return pagePictureVO;
            }
            //缓存未命中
            log.info("缓存未命中, key: {}", redisKey);
            return null;
        } catch (Exception e) {
            log.error("读取缓存时发生异常：{}", e.getMessage(), e);
            //读缓存也不要影响主进程，返回空
            return null;
        }

    }

    @Override
    public void writeCache(Page<PictureVO> pictureVOPage, PictureQueryRequest pictureQueryRequest) {
        try {
            // 参数校验
            if (pictureVOPage == null || pictureQueryRequest == null) {
                log.warn("写入缓存参数不合法，pictureVOPage: {}, pictureQueryRequest: {}", pictureVOPage, pictureQueryRequest);
                return;
            }

            // 构建缓存key（与readCache保持一致）
            String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
            String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
            String redisKey = "pikapicture:listPictureVOPage" + hashKey;

            // 存入redis
            ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
            String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
            // 5-10分钟过期，防止雪崩
            int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
            valueOps.set(redisKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);

            log.info("缓存写入成功，key: {}, 过期时间: {}秒", redisKey, cacheExpireTime);

        } catch (Exception e) {
            log.error("写入缓存时发生异常: {}", e.getMessage(), e);
            // 写入缓存失败不应该影响主流程，返回false
        }
    }

    @Override
    public Boolean deleteCache(String redisKey) {
        try {
            // 参数校验
            if (redisKey == null || redisKey.trim().isEmpty()) {
                log.warn("删除缓存参数不合法，redisKey: {}", redisKey);
                return false;
            }

            Boolean deleted = stringRedisTemplate.delete(redisKey);

            if (Boolean.TRUE.equals(deleted)) {
                log.info("缓存删除成功，key: {}", redisKey);
                return true;
            } else {
                log.warn("缓存删除失败或key不存在，key: {}", redisKey);
                return false;
            }

        } catch (Exception e) {
            log.error("删除缓存时发生异常，key: {}, 错误: {}", redisKey, e.getMessage(), e);
            // 删除缓存失败不应该影响主流程，返回false
            return false;
        }
    }
}
