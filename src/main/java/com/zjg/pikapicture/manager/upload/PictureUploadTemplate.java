package com.zjg.pikapicture.manager.upload;


import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.zjg.pikapicture.config.CosClientConfig;
import com.zjg.pikapicture.exception.BusinessException;
import com.zjg.pikapicture.exception.ErrorCode;
import com.zjg.pikapicture.exception.ThrowUtils;
import com.zjg.pikapicture.manager.COSManager;
import com.zjg.pikapicture.model.dto.file.UploadPictureResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    private COSManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        //检验输入源
        validPicture(inputSource);
        //图片上传地址
        String uuid = RandomUtil.randomString(16);
        String filename = getOriginalFilename(inputSource);
        String suffix = FileUtil.getSuffix(filename);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, suffix);
        String uploadPath = String.format("%s%s", uploadPathPrefix, uploadFilename);
        //解析结果并返回
        File file = null;
        try {
            //创建临时文件
            file = File.createTempFile(uploadPath, null);
            //处理文件来源
            processFile(inputSource, file);
            //上传图片到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult()
                    .getOriginalInfo().getImageInfo();

            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (ObjUtil.isNotEmpty(objectList)) {
                CIObject compressCiObject = objectList.get(0);
                //缩略图默认是压缩图
                CIObject thumbnailCiObject = compressCiObject;
                //有缩略图生成得到缩略图
                if (objectList.size() > 1) {
                    thumbnailCiObject = objectList.get(1);
                }
                //封装压缩图到数据库返回结果
                return buildResult(filename, compressCiObject, thumbnailCiObject);
            }
            //封装原图到数据库返回结果
            return buildResult(uploadPath, imageInfo, filename, file);
        } catch (Exception e) {
            log.error("upload picture error", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传图片失败");
        } finally {
            //清理临时文件
            if (file != null) {
                deleteTempFile(file);
            }

        }

    }

    private UploadPictureResult buildResult(String originFileName, CIObject compressCiObject, CIObject thumbnailCiObject) {
        //封装返回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = compressCiObject.getWidth();
        int picHeight = compressCiObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        //设置图片为压缩后的地址
        uploadPictureResult.setUrl(cosClientConfig.getHost() + '/' + compressCiObject.getKey());
        uploadPictureResult.setPicName(FileUtil.mainName(originFileName));
        uploadPictureResult.setPicSize(compressCiObject.getSize().longValue());
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressCiObject.getFormat());
        //缩略图地址
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + '/' + thumbnailCiObject.getKey());
        return uploadPictureResult;
    }

    /**
     * 获取结果
     *
     * @param uploadPath
     * @param file
     * @param filename
     * @return
     */
    private UploadPictureResult buildResult(String uploadPath, ImageInfo imageInfo, String filename, File file) {

        //封装返回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();

        uploadPictureResult.setUrl(cosClientConfig.getHost() + '/' + uploadPath);
        uploadPictureResult.setPicName(FileUtil.mainName(filename));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        return uploadPictureResult;
    }

    protected abstract void validPicture(Object inputSource);


    protected abstract String getOriginalFilename(Object inputSource);

    protected abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 删除临时文件
     *
     * @param file
     */
    private void deleteTempFile(File file) {
//        1. 判断为空，不清理，直接返回
        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR);
//        2. file.delete， 如果删除失败，日志报错
        boolean delete = file.delete();
        if (!delete) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }


}
