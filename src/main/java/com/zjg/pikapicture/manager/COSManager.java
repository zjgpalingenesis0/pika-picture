package com.zjg.pikapicture.manager;

import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;

import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.zjg.pikapicture.config.CosClientConfig;
import com.zjg.pikapicture.exception.ThrowUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * 提供通用的对象存储操作（文件上传，文件下载）
 */
@Slf4j
@Component
public class COSManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传文件
     *
     * @param key  唯一键
     * @param file 文件
     * @return
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载文件
     *
     * @param key 唯一键
     * @return
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 上传对象，附带图片信息
     *
     * @param key
     * @param file
     * @return
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        //图片处理
//        1. 创建一个图片操作对象，picOperations
        PicOperations picOperations = new PicOperations();
//        2. 设置setIsPicInfo(1)，表示返回原图信息
        picOperations.setIsPicInfo(1);

        //图片压缩,转成webp格式
        List<PicOperations.Rule> rules = new ArrayList<>();
        String webpName = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setRule("imageMogr2/format/webp");
        compressRule.setFileId(webpName);
        rules.add(compressRule);
        //缩略图处理
        //只对>20KB的图片做缩略图处理
        if (file.length() > 20 * 1024) {
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 128, 128));
            thumbnailRule.setFileId(thumbnailKey);
            rules.add(thumbnailRule);
        }

//      构造处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 删除文件
     *
     * @param key 文件路径或完整URL
     */
    public void deleteObject(String key) {
        ThrowUtils.throwIf(key == null || key.trim().isEmpty(),
                new IllegalArgumentException("删除文件的key不能为空"));

        // 如果传入的是完整URL，提取文件路径
        String actualKey = extractKeyFromUrl(key.trim());

        log.info("开始删除COS文件，输入: {}, 提取的key: {}, bucket: {}",
                key, actualKey, cosClientConfig.getBucket());

        try {
            cosClient.deleteObject(cosClientConfig.getBucket(), actualKey);
            log.info("COS文件删除成功: {}", actualKey);
        } catch (CosClientException e) {
            log.error("COS文件删除失败，key: {}, bucket: {}", actualKey,
                    cosClientConfig.getBucket(), e);
            throw e;
        }
    }


    /**
     * 从URL中提取文件路径key
     * 支持以下格式的URL：
     * 1. https://bucket-name.cos.region.myqcloud.com/path/to/file.jpg
     * 2. http://bucket-name.cos.region.myqcloud.com/path/to/file.jpg
     * 3. 直接的文件路径：path/to/file.jpg
     *
     * @param urlOrKey URL或文件路径
     * @return 提取的文件路径key
     */
    private String extractKeyFromUrl(String urlOrKey) {
        // 如果不包含http://或https://，认为是直接的文件路径
        if (!urlOrKey.startsWith("http")) {
            log.debug("输入是文件路径，直接返回: {}", urlOrKey);
            return urlOrKey;
        }

        try {
            URI uri = new URI(urlOrKey);
            String path = uri.getPath();

            // 去掉开头的斜杠（如果存在）
            if (path != null && path.startsWith("/")) {
                path = path.substring(1);
            }

            log.debug("从URL提取文件路径: {} -> {}", urlOrKey, path);
            return path != null ? path : urlOrKey;
        } catch (URISyntaxException e) {
            log.warn("URL解析失败，使用原始输入: {}, 错误: {}", urlOrKey, e.getMessage());
            // 如果URL解析失败，尝试从最后一个斜杠开始截取
            int lastSlashIndex = urlOrKey.lastIndexOf('/');
            if (lastSlashIndex > 0) {
                String extractedKey = urlOrKey.substring(lastSlashIndex + 1);
                log.info("备用方案提取文件路径: {} -> {}", urlOrKey, extractedKey);
                return extractedKey;
            }
            return urlOrKey;
        }
    }


}
